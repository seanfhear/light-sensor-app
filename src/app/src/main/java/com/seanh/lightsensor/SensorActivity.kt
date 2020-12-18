package com.seanh.lightsensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.*
import com.google.android.gms.location.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seanh.lightsensor.models.Data
import com.seanh.lightsensor.models.Reading
import com.seanh.lightsensor.models.Weather
import kotlinx.android.synthetic.main.activity_sensor.*
import org.json.JSONObject
import java.time.LocalDateTime


const val LightDeltaThresh = 1
const val ReadingsDataRef = "readings"
const val WeatherDataRef = "weather"

val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

class SensorActivity : AppCompatActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var currentLocation: String = ""
    private var lastLightLevel: Float = 0.0F

    private val locationPermissionReqCode = 1000
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var database: FirebaseDatabase

    lateinit var mainHandler: Handler

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_LIGHT) {
            if (kotlin.math.abs(event.values[0] - lastLightLevel) > LightDeltaThresh) {
                lastLightLevel = event.values[0]
            }
        }
    }

    private val updateTextTask = object : Runnable {
        override fun run() {
            recordEvent()
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        getCurrentLocation()
        mainHandler = Handler(Looper.getMainLooper())

        val btn: ToggleButton = findViewById(R.id.toggleBtn)
        btn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onToggleOn()
            else onToggleOff()
        }
    }

    private fun recordEvent() {
        database = Firebase.database

        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        getCurrentLocation()

        val data = ArrayList<Data>()
        val now = LocalDateTime.now()
        val hour = now.hour
        var time: String = ""
        println(hour)
        time = if (hour in 7..17) {
            "day"
        } else {
            "night"
        }

        if (currentLocation == "") return

        data.add(Data("Light", "%.1f".format(lastLightLevel)))
        data.add(Data("Time", now.toString()))
        data.add(Data("Location", currentLocation))

        val loc = currentLocation.split(",")
        val lat = loc[0].toDouble()
        val lon = loc[1].toDouble()

        val ref: DatabaseReference = database.getReference(ReadingsDataRef).push()
        ref.setValue(
            Reading(
                value = lastLightLevel.toString(),
                time = time,
                latitude = lat,
                longitude = lon,
                device = device
            )
        )
        //getCurrentWeather(lat, lon, time)

        recyclerView.adapter = DataAdapter(data)
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionReqCode)
            return
        }

        // https://stackoverflow.com/questions/29441384/fusedlocationapi-getlastlocation-always-null
        val mLocationRequest = LocationRequest.create()
        mLocationRequest.interval = 3000
        mLocationRequest.fastestInterval = 500
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        val mLocationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult == null) {
                    return
                }
                for (location in locationResult.locations) {
                    if (location != null) {
                        currentLocation = "${location.latitude},${location.longitude}"
                    }
                }
            }
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, mLocationCallback, null)
    }

    private fun getCurrentWeather(lat: Double, lon: Double, time: String) {
        val loc = currentLocation.split(",")
        val weatherURL = "https://api.openweathermap.org/data/2.5/onecall?lat=${loc[0]}&lon=${loc[1]}&exclude=hourly,daily,minutely,alerts&appid=bce4ba66924c5a700dddb9abb2e08382"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, weatherURL, null,
            { response ->
                val ref: DatabaseReference = database.getReference(WeatherDataRef).push()

                val current: JSONObject = response.get("current") as JSONObject
                ref.setValue(
                    Weather(
                        clouds = current.get("clouds") as Int,
                        visibility = current.get("visibility") as Int,
                        latitude = lat,
                        longitude = lon,
                        time = time
                    )
                )
            },
            {
                // TODO: Handle error
            }
        )

        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }

    private fun onToggleOff() {
        sensorManager!!.unregisterListener(this)
        mainHandler.removeCallbacks(updateTextTask)
    }

    private fun onToggleOn() {
        sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL)
        mainHandler.post(updateTextTask)
    }
}
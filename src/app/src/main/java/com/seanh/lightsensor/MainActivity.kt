package com.seanh.lightsensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.seanh.lightsensor.models.Data
import com.seanh.lightsensor.models.Reading
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDateTime


const val LightDeltaThresh = 1

class MainActivity : AppCompatActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var currentLocation: String = ""
    private var lastLightLevel: Float = 0.0F

    private val locationPermissionReqCode = 1000
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var database: FirebaseDatabase

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_LIGHT) {
            if (kotlin.math.abs(event.values[0] - lastLightLevel) > LightDeltaThresh) {
                recordEvent(event)
                lastLightLevel = event.values[0]
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btn: ToggleButton = findViewById(R.id.toggleBtn)
        btn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onToggleOn()
            else onToggleOff()
        }
    }

    private fun recordEvent(event: SensorEvent) {
        database = Firebase.database

        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        getCurrentLocation()

        val data = ArrayList<Data>()
        val light = event.values[0]
        val time = LocalDateTime.now().toString()
        val loc = currentLocation
        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

        data.add(Data("Light", "%.1f".format(light)))
        data.add(Data("Time", time))
        data.add(Data("Location", loc))

        val ref: DatabaseReference = database.getReference("readings").push()
        ref.setValue(Reading(
            value = light.toString(),
            time = time,
            location = loc,
            device = device
        ))

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
        mLocationRequest.interval = 60000
        mLocationRequest.fastestInterval = 5000
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

    private fun onToggleOff() {
        sensorManager!!.unregisterListener(this)
    }

    private fun onToggleOn() {
        sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL)
    }
}
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
import com.seanh.lightsensor.models.Data
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.time.LocalDateTime


const val LightDeltaThresh = 1

class MainActivity : AppCompatActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var currentLocation: String = ""
    private var dataList = ArrayList<Array<String>>()
    private var lastLightLevel: Float = 0.0F

    private val locationPermissionReqCode = 1000;
    private lateinit var fusedLocationClient: FusedLocationProviderClient

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
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        getCurrentLocation()

        val data = ArrayList<Data>()
        val light = event.values[0]
        val time = LocalDateTime.now().toString()
        val loc = currentLocation

        data.add(Data("Light", "%.1f".format(light)))
        data.add(Data("Time", time))
        data.add(Data("Location", loc))

        val entry: Array<String> = arrayOf(light.toString(), time, loc)
        dataList.add(entry)

        recyclerView.adapter = DataAdapter(data)
    }

    private fun writeEventsToFile() {
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val randomString = (1..32)
                .map { _ -> kotlin.random.Random.nextInt(0, charPool.size) }
                .map(charPool::get)
                .joinToString("");
        val fileName = "LightData-${randomString}.csv"
        val file = File(getExternalFilesDir("Data"), fileName)
        val fileExists = file.exists()
        if (!fileExists)
            file.createNewFile()

        var dataString = ""
        for (entry in dataList) {
            dataString = dataString + entry.joinToString(",") + "\n"
        }
        file.writeText(dataString)
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionReqCode);
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
        writeEventsToFile()
    }

    private fun onToggleOn() {
        dataList = ArrayList()
        sensorManager!!.registerListener(this, sensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL)
    }
}
package com.seanh.lightsensor

import android.graphics.Color
import android.os.Bundle
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression.get
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.seanh.lightsensor.models.*
import java.io.File
import java.net.URISyntaxException


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: MapboxMap? = null
    private var mapView: MapView? = null
    private var time = "night"

    private lateinit var database: FirebaseDatabase
    var lightList: ArrayList<Reading> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getMarkers()

        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        setContentView(R.layout.activity_map)

        val btn: ToggleButton = findViewById(R.id.toggleButton)
        btn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) onToggleOn()
            else onToggleOff()
        }


        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        mMap = mapboxMap
        val s = if (time == "day") {
            Style.LIGHT
        } else {
            Style.DARK
        }
        mapboxMap.setStyle(
            s
        ) { style ->
            try {
                val lightGeoJson = GeoJsonSource(
                    "lightdata", createLightGeoJson()
                )
                style.addSource(lightGeoJson)

                style.addLayer(
                    FillExtrusionLayer("light", "lightdata").withProperties(
                        fillExtrusionColor(Color.YELLOW),
                        fillExtrusionOpacity(0.7f),
                        fillExtrusionHeight(get("e"))
                    )
                )
            } catch (exception: URISyntaxException) {

            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onResume() {
        super.onResume()
        mMap?.let { onMapReady(it) }
        getMarkers()
        mapView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    private fun getMarkers() {
        database = Firebase.database
        val ref: DatabaseReference = database.getReference(ReadingsDataRef)

        lightList = ArrayList()
        val dataListener = object : ValueEventListener {

            override fun onDataChange(readingSnapshot: DataSnapshot) {
                if (readingSnapshot.exists()) {
                    for (entry in readingSnapshot.children) {
                        val e = entry.value as HashMap<*, *>
                        if (e.get("time") as String == time) {
                            lightList.add(
                                Reading(
                                    value = e.get("value") as String,
                                    time = e.get("time") as String,
                                    latitude = e.get("latitude") as Double,
                                    longitude = e.get("longitude") as Double,
                                    device = e.get("device") as String
                                )
                            )
                        }
                    }
                    onMapReady(mMap!!)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Failed to read value
            }
        }

        ref.addValueEventListener(dataListener)
    }

    private fun createLightGeoJson (): String {
        val geoJson = GeoJson()
        val features = ArrayList<GeoJsonFeature>()

        for (entry in lightList) {
            val geoJsonFeature = GeoJsonFeature()
            val geoJsonGeometry = GeoJsonGeometry()
            val geoJsonProperty = GeoJsonProperty()

            val coords_top = ArrayList<ArrayList<ArrayList<Double>>>()
            val coords = ArrayList<ArrayList<Double>>()

            // arrange points into squares so they can be extruded
            val offset = 0.0002
            val y_offset = 0.000125
            coords.add(arrayListOf(entry.longitude, entry.latitude))
            coords.add(arrayListOf(entry.longitude, entry.latitude + y_offset))
            coords.add(arrayListOf(entry.longitude + offset, entry.latitude + y_offset))
            coords.add(arrayListOf(entry.longitude + offset, entry.latitude))
            coords.add(arrayListOf(entry.longitude, entry.latitude))

            coords_top.add(coords)
            geoJsonGeometry.coordinates = coords_top

            geoJsonProperty.e = entry.value.toDouble() + 1

            geoJsonFeature.geometry = geoJsonGeometry
            geoJsonFeature.properties = geoJsonProperty

            features.add(geoJsonFeature)
        }
        geoJson.features = features

        val gson = Gson()
        val jsonString = gson.toJson(geoJson)

        val f = File(getExternalFilesDir("Data"), "light.geojson")
        f.writeText(jsonString)

        return jsonString
    }

    private fun onToggleOff() {
        time = "night"
        onResume()
    }

    private fun onToggleOn() {
        time = "day"
        onResume()
    }
}
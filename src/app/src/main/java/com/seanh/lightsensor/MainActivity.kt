package com.seanh.lightsensor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun navigate(view: View) {
        val buttonText: String = (view as Button).text.toString()
        if (buttonText == "Sensor") {
            val nav = Intent(this, SensorActivity::class.java)
            startActivity(nav)
        }
        else if (buttonText == "Map") {
            val nav = Intent(this, MapActivity::class.java)
            startActivity(nav)
        }
    }
}
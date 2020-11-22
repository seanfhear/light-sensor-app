package com.seanh.lightsensor.models

data class Weather(
    var clouds: Int,
    var visibility: Int,
    var latitude: Double,
    var longitude: Double,
    var time: String
)
package com.seanh.lightsensor.models

data class Reading (
    var value: String,
    var time: String,
    var latitude: Double,
    var longitude: Double,
    var device: String
)

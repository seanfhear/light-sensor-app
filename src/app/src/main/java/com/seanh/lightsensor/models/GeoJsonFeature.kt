package com.seanh.lightsensor.models

class GeoJsonFeature {
    var type: String = "Feature"
    var geometry: GeoJsonGeometry? = null
    var properties: GeoJsonProperty? = null
}
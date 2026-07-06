package com.navi.phantom.features.map.logic

interface MapController {
    fun animateCamera(latitude: Double, longitude: Double, zoom: Double)
    fun drawRoute(points: List<Pair<Double, Double>>)
    fun clearRoute()
}

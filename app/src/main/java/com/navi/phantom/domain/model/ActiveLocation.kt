package com.navi.phantom.domain.model

data class ActiveLocation(
    val packageName: String,
    val placeId: Long?,
    val placeName: String?,
    val latitude: Double,
    val longitude: Double,
    val assignedAt: Long = System.currentTimeMillis()
)

package com.navi.phantom.domain.model

data class Place(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val accuracy: Float? = null,
    val createdAt: Long = System.currentTimeMillis()
)

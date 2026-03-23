package com.navi.phantom.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "active_locations")
data class ActiveLocationEntity(
    @PrimaryKey
    val packageName: String,
    val placeId: Long?,
    val placeName: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val assignedAt: Long
)

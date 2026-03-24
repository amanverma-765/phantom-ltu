package com.navi.phantom.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "active_locations",
    indices = [Index("placeId")],
    foreignKeys = [
        ForeignKey(
            entity = PlaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
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

package com.navi.phantom.domain.repository

import com.navi.phantom.domain.model.Place
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    fun getAllPlaces(): Flow<List<Place>>
    suspend fun getPlaceById(id: Long): Place?
    suspend fun insertPlace(place: Place): Long
    suspend fun updatePlace(place: Place)
    suspend fun deletePlace(place: Place)
}

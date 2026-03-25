package com.navi.phantom.domain.usecase

import com.navi.phantom.domain.model.Place
import com.navi.phantom.domain.repository.ActiveLocationRepository
import com.navi.phantom.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow

class PlaceUseCase(
    private val placeRepository: PlaceRepository,
    private val activeLocationRepository: ActiveLocationRepository
) {
    fun getAllPlaces(): Flow<List<Place>> = placeRepository.getAllPlaces()

    suspend fun getPlaceById(id: Long): Place? = placeRepository.getPlaceById(id)

    suspend fun savePlace(name: String, latitude: Double, longitude: Double, address: String? = null, accuracy: Float? = null): Result<Long> =
        runCatching {
            placeRepository.insertPlace(
                Place(name = name, latitude = latitude, longitude = longitude, address = address, accuracy = accuracy)
            )
        }

    suspend fun deletePlace(place: Place): Result<Unit> =
        runCatching { placeRepository.deletePlace(place) }

    suspend fun updatePlace(place: Place): Result<Unit> =
        runCatching {
            placeRepository.updatePlace(place)
            activeLocationRepository.updateByPlaceId(place)
        }
}

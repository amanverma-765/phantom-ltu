package com.navi.phantom.data.places

import com.navi.phantom.data.database.dao.PlaceDao
import com.navi.phantom.data.database.entity.PlaceEntity
import com.navi.phantom.domain.model.Place
import com.navi.phantom.domain.repository.PlaceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaceRepositoryImpl(
    private val placeDao: PlaceDao
) : PlaceRepository {

    override fun getAllPlaces(): Flow<List<Place>> =
        placeDao.getAllPlaces().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getPlaceById(id: Long): Place? =
        placeDao.getPlaceById(id)?.toDomain()

    override suspend fun insertPlace(place: Place): Long =
        placeDao.insertPlace(place.toEntity())

    override suspend fun updatePlace(place: Place) =
        placeDao.updatePlace(place.toEntity())

    override suspend fun deletePlace(place: Place) =
        placeDao.deletePlace(place.toEntity())

    private fun PlaceEntity.toDomain() = Place(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        createdAt = createdAt
    )

    private fun Place.toEntity() = PlaceEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        address = address,
        createdAt = createdAt
    )
}

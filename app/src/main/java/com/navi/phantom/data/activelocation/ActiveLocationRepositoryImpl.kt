package com.navi.phantom.data.activelocation

import com.navi.phantom.data.database.dao.ActiveLocationDao
import com.navi.phantom.data.database.entity.ActiveLocationEntity
import com.navi.phantom.domain.model.ActiveLocation
import com.navi.phantom.domain.model.Place
import com.navi.phantom.domain.repository.ActiveLocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActiveLocationRepositoryImpl(
    private val activeLocationDao: ActiveLocationDao
) : ActiveLocationRepository {

    override fun getActiveLocation(packageName: String): Flow<ActiveLocation?> =
        activeLocationDao.getActiveLocation(packageName).map { it?.toDomain() }

    override fun getAllActiveLocations(): Flow<List<ActiveLocation>> =
        activeLocationDao.getAllActiveLocations().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getActiveLocationSync(packageName: String): ActiveLocation? =
        activeLocationDao.getActiveLocationSync(packageName)?.toDomain()

    override suspend fun setActiveLocation(packageName: String, place: Place) {
        activeLocationDao.setActiveLocation(
            ActiveLocationEntity(
                packageName = packageName,
                placeId = place.id,
                placeName = place.name,
                latitude = place.latitude,
                longitude = place.longitude,
                accuracy = place.accuracy,
                assignedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearActiveLocation(packageName: String) {
        activeLocationDao.clearActiveLocation(packageName)
    }

    private fun ActiveLocationEntity.toDomain() = ActiveLocation(
        packageName = packageName,
        placeId = placeId,
        placeName = placeName,
        latitude = latitude,
        longitude = longitude,
        accuracy = accuracy,
        assignedAt = assignedAt
    )
}

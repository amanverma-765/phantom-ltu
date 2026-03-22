package com.navi.phantom.domain.usecase

import com.navi.phantom.domain.model.ActiveLocation
import com.navi.phantom.domain.model.Place
import com.navi.phantom.domain.repository.ActiveLocationRepository
import kotlinx.coroutines.flow.Flow

class ActiveLocationUseCase(
    private val repository: ActiveLocationRepository
) {
    fun getActiveLocation(packageName: String): Flow<ActiveLocation?> =
        repository.getActiveLocation(packageName)

    fun getAllActiveLocations(): Flow<List<ActiveLocation>> =
        repository.getAllActiveLocations()

    suspend fun getActiveLocationSync(packageName: String): ActiveLocation? =
        repository.getActiveLocationSync(packageName)

    suspend fun assignPlace(packageName: String, place: Place): Result<Unit> =
        runCatching { repository.setActiveLocation(packageName, place) }

    suspend fun clearLocation(packageName: String): Result<Unit> =
        runCatching { repository.clearActiveLocation(packageName) }
}

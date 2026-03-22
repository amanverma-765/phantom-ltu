package com.navi.phantom.domain.repository

import com.navi.phantom.domain.model.ActiveLocation
import com.navi.phantom.domain.model.Place
import kotlinx.coroutines.flow.Flow

interface ActiveLocationRepository {
    fun getActiveLocation(packageName: String): Flow<ActiveLocation?>
    fun getAllActiveLocations(): Flow<List<ActiveLocation>>
    suspend fun getActiveLocationSync(packageName: String): ActiveLocation?
    suspend fun setActiveLocation(packageName: String, place: Place)
    suspend fun clearActiveLocation(packageName: String)
}

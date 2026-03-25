package com.navi.phantom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.navi.phantom.data.database.entity.ActiveLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveLocationDao {
    @Query("SELECT * FROM active_locations WHERE packageName = :packageName")
    fun getActiveLocation(packageName: String): Flow<ActiveLocationEntity?>

    @Query("SELECT * FROM active_locations WHERE packageName = :packageName")
    suspend fun getActiveLocationSync(packageName: String): ActiveLocationEntity?

    @Query("SELECT * FROM active_locations")
    fun getAllActiveLocations(): Flow<List<ActiveLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setActiveLocation(entity: ActiveLocationEntity)

    @Query("DELETE FROM active_locations WHERE packageName = :packageName")
    suspend fun clearActiveLocation(packageName: String)

    @Query(
        """UPDATE active_locations
           SET placeName = :placeName, latitude = :latitude, longitude = :longitude, accuracy = :accuracy
           WHERE placeId = :placeId"""
    )
    suspend fun updateByPlaceId(placeId: Long, placeName: String, latitude: Double, longitude: Double, accuracy: Float?)
}

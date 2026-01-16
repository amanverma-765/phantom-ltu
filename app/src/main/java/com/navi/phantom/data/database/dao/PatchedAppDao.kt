package com.navi.phantom.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.navi.phantom.data.database.entity.PatchedAppEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for patched apps.
 *
 * Provides reactive streams via Flow for observing database changes,
 * which is ideal for Compose UI updates.
 */
@Dao
interface PatchedAppDao {

    /**
     * Observe all patched apps ordered by most recently patched first.
     * Returns a Flow that emits whenever the data changes.
     */
    @Query("SELECT * FROM patched_apps ORDER BY patchedAtMillis DESC")
    fun observeAll(): Flow<List<PatchedAppEntity>>

    /**
     * Get all patched apps (one-shot query).
     */
    @Query("SELECT * FROM patched_apps ORDER BY patchedAtMillis DESC")
    suspend fun getAll(): List<PatchedAppEntity>

    /**
     * Get a patched app by its package name.
     */
    @Query("SELECT * FROM patched_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackageName(packageName: String): PatchedAppEntity?

    /**
     * Observe a specific patched app by package name.
     */
    @Query("SELECT * FROM patched_apps WHERE packageName = :packageName LIMIT 1")
    fun observeByPackageName(packageName: String): Flow<PatchedAppEntity?>

    /**
     * Get a patched app by its ID.
     */
    @Query("SELECT * FROM patched_apps WHERE id = :id")
    suspend fun getById(id: Long): PatchedAppEntity?

    /**
     * Insert a new patched app. If a record with the same package name exists,
     * it will be replaced (useful for re-patching an app).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(patchedApp: PatchedAppEntity): Long

    /**
     * Update an existing patched app.
     */
    @Update
    suspend fun update(patchedApp: PatchedAppEntity)

    /**
     * Delete a patched app.
     */
    @Delete
    suspend fun delete(patchedApp: PatchedAppEntity)

    /**
     * Delete a patched app by package name.
     */
    @Query("DELETE FROM patched_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    /**
     * Delete a patched app by ID.
     */
    @Query("DELETE FROM patched_apps WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Check if a patched app exists for the given package name.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM patched_apps WHERE packageName = :packageName)")
    suspend fun exists(packageName: String): Boolean

    /**
     * Get the count of patched apps.
     */
    @Query("SELECT COUNT(*) FROM patched_apps")
    suspend fun count(): Int

    /**
     * Observe the count of patched apps.
     */
    @Query("SELECT COUNT(*) FROM patched_apps")
    fun observeCount(): Flow<Int>
}

package com.navi.phantom.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.navi.phantom.data.database.entity.PlaceholderEntity

/**
 * Main Room database for the Phantom application.
 *
 * When adding new entities:
 * 1. Create your entity class annotated with @Entity
 * 2. Add the entity to the entities array below
 * 3. Increment the version number
 * 4. Add migration strategy if needed
 *
 * Best practices:
 * - Always use migrations for production releases
 * - Keep DAOs focused on single entity operations
 * - Use TypeConverters for complex types
 */
@Database(
    entities = [PlaceholderEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PhantomDatabase : RoomDatabase() {

    companion object {
        const val DATABASE_NAME = "phantom_database"
    }
}

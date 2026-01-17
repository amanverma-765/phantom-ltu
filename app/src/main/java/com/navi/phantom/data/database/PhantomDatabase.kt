package com.navi.phantom.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.navi.phantom.data.database.entity.PlaceholderEntity

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

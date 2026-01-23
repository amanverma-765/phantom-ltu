package com.navi.phantom.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.navi.phantom.data.database.dao.PlaceDao
import com.navi.phantom.data.database.entity.PlaceEntity

@Database(
    entities = [PlaceEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PhantomDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao

    companion object {
        const val DATABASE_NAME = "phantom_database"
    }
}

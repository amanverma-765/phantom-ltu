package com.navi.phantom.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.navi.phantom.data.database.dao.ActiveLocationDao
import com.navi.phantom.data.database.dao.PlaceDao
import com.navi.phantom.data.database.entity.ActiveLocationEntity
import com.navi.phantom.data.database.entity.PlaceEntity

@Database(
    entities = [PlaceEntity::class, ActiveLocationEntity::class],
    version = 2,
    exportSchema = true
)
abstract class PhantomDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun activeLocationDao(): ActiveLocationDao

    companion object {
        const val DATABASE_NAME = "phantom_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `active_locations` (
                        `packageName` TEXT NOT NULL,
                        `placeId` INTEGER,
                        `placeName` TEXT,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `assignedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`packageName`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}

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
    version = 4,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `places` ADD COLUMN `accuracy` REAL")
                db.execSQL("ALTER TABLE `active_locations` ADD COLUMN `accuracy` REAL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `active_locations_new` (
                        `packageName` TEXT NOT NULL,
                        `placeId` INTEGER,
                        `placeName` TEXT,
                        `latitude` REAL NOT NULL,
                        `longitude` REAL NOT NULL,
                        `accuracy` REAL,
                        `assignedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`packageName`),
                        FOREIGN KEY(`placeId`) REFERENCES `places`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `active_locations_new`
                    SELECT * FROM `active_locations`
                    WHERE `placeId` IS NULL OR `placeId` IN (SELECT `id` FROM `places`)
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `active_locations`")
                db.execSQL("ALTER TABLE `active_locations_new` RENAME TO `active_locations`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_active_locations_placeId` ON `active_locations` (`placeId`)")
            }
        }
    }
}

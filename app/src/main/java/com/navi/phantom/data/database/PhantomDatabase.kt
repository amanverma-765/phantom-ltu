package com.navi.phantom.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.navi.phantom.data.database.dao.PatchedAppDao
import com.navi.phantom.data.database.entity.PatchedAppEntity

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
    entities = [PatchedAppEntity::class],
    version = 2,
    exportSchema = true
)
abstract class PhantomDatabase : RoomDatabase() {

    companion object {
        const val DATABASE_NAME = "phantom_database"

        /**
         * Migration from version 1 to 2:
         * - Replaces iconPath (TEXT) with iconBytes (BLOB)
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE patched_apps_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        versionName TEXT NOT NULL,
                        versionCode INTEGER NOT NULL,
                        patchedApkPath TEXT NOT NULL,
                        originalApkSizeBytes INTEGER NOT NULL,
                        patchedAtMillis INTEGER NOT NULL,
                        isSplitApk INTEGER NOT NULL,
                        iconBytes BLOB
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO patched_apps_new (id, packageName, appName, versionName, versionCode,
                        patchedApkPath, originalApkSizeBytes, patchedAtMillis, isSplitApk, iconBytes)
                    SELECT id, packageName, appName, versionName, versionCode,
                        patchedApkPath, originalApkSizeBytes, patchedAtMillis, isSplitApk, NULL
                    FROM patched_apps
                """.trimIndent())

                db.execSQL("DROP TABLE patched_apps")

                db.execSQL("ALTER TABLE patched_apps_new RENAME TO patched_apps")

                db.execSQL("CREATE UNIQUE INDEX index_patched_apps_packageName ON patched_apps (packageName)")
            }
        }
    }

    abstract fun patchedAppDao(): PatchedAppDao
}

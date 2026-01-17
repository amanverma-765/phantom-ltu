package com.navi.phantom.di

import androidx.room.Room
import com.navi.phantom.data.database.PhantomDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module for database-related dependencies.
 *
 * Provides:
 * - PhantomDatabase singleton instance
 * - All DAO instances (when added)
 */
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            PhantomDatabase::class.java,
            PhantomDatabase.DATABASE_NAME
        ).build()
    }
}

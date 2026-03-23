package com.navi.phantom.di

import androidx.room.Room
import com.navi.phantom.data.database.PhantomDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            PhantomDatabase::class.java,
            PhantomDatabase.DATABASE_NAME
        )
            .addMigrations(PhantomDatabase.MIGRATION_1_2, PhantomDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<PhantomDatabase>().placeDao() }
    single { get<PhantomDatabase>().activeLocationDao() }
}

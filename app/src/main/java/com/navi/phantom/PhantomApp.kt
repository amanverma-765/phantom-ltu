package com.navi.phantom

import android.app.Application
import com.navi.phantom.di.appModule
import com.navi.phantom.di.databaseModule
import com.navi.phantom.di.navModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.maplibre.android.MapLibre

class PhantomApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize MapLibre before any MapView is created
        MapLibre.getInstance(this)

        startKoin {
            androidLogger()
            androidContext(this@PhantomApp)
            modules(databaseModule, navModule, appModule)
        }
    }
}

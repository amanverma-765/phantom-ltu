package com.navi.phantom

import android.app.Application
import com.navi.phantom.di.navModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class PhantomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@PhantomApp)
            modules(navModule)
        }
    }
}
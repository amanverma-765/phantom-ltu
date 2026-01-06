package com.riva.mods

import android.app.Application
import com.riva.mods.di.authModule
import com.riva.mods.di.coreModule
import com.riva.mods.di.dataModule
import com.riva.mods.di.navModule
import com.riva.mods.utils.AssetUtils
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class RivaModsApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Validate required LSPatch assets are present in APK
        // This ensures patched apps won't crash when trying to load from manager
        AssetUtils.validateLspatchAssets(this)

        startKoin {
            androidLogger()
            androidContext(this@RivaModsApp)
            modules(coreModule, dataModule, navModule, authModule)
        }
    }
}
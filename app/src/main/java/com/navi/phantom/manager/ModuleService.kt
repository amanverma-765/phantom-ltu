package com.navi.phantom.manager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import co.touchlab.kermit.Logger

/**
 * Service that handles bind requests from patched apps.
 * When a patched app starts, it binds to this service to receive its assigned Xposed modules.
 */
class ModuleService : Service() {

    private val log = Logger.withTag("ModuleService")

    override fun onCreate() {
        super.onCreate()
        log.d { "ModuleService created" }
    }

    override fun onBind(intent: Intent): IBinder {
        val callerPackage = intent.getStringExtra("packageName") ?: "unknown"
        log.i { "$callerPackage requests binder" }
        return ManagerService.asBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        log.d { "ModuleService destroyed" }
    }
}

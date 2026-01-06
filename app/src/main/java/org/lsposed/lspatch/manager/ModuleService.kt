package org.lsposed.lspatch.manager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.lsposed.lspatch.service.PatchManagerService

/**
 * Bound service that patched apps connect to for retrieving modules.
 *
 * IMPORTANT: This class MUST be in the 'manager' package because the LSPatch
 * loader (loader.dex) hardcodes the service name as:
 * "org.lsposed.lspatch.manager.ModuleService"
 *
 * Patched apps with useManager=true bind to this service via:
 * ComponentName("org.lsposed.lspatch", "org.lsposed.lspatch.manager.ModuleService")
 *
 * This service returns the PatchManagerService binder which implements
 * ILSPApplicationService to serve built-in patches to the calling app.
 */
class ModuleService : Service() {

    companion object {
        private const val TAG = "ModuleService"
    }

    override fun onBind(intent: Intent): IBinder? {
        // Note: packageName from intent is caller-provided and untrusted
        // Actual caller verification uses Binder.getCallingUid() in PatchManagerService
        val packageName = intent.getStringExtra("packageName")
        Log.i(TAG, "Bind request from: $packageName")

        // Return the PatchManagerService binder
        return PatchManagerService.asBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(TAG, "Unbind from: ${intent?.getStringExtra("packageName")}")
        return super.onUnbind(intent)
    }
}
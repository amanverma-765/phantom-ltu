package com.navi.phantom.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import co.touchlab.kermit.Logger
import kotlinx.coroutines.channels.Channel

sealed interface InstallResult {
    data object Success : InstallResult
    data class Failure(val status: Int, val message: String?) : InstallResult
    data class UserActionRequired(val intent: Intent) : InstallResult
}


class InstallStatusReceiver : BroadcastReceiver() {
    
    private val log = Logger.withTag("InstallStatusReceiver")
    
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        
        log.d { "Received: status=$status, sessionId=$sessionId, message=$message" }
        
        val result = when (status) {
            PackageInstaller.STATUS_SUCCESS -> {
                log.i { "Installation succeeded for session $sessionId" }
                InstallResult.Success
            }
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    log.d { "User confirmation required for session $sessionId" }
                    InstallResult.UserActionRequired(confirmIntent)
                } else {
                    log.e { "No confirm intent provided" }
                    InstallResult.Failure(status, "User action required but no intent provided")
                }
            }
            else -> {
                log.e { "Installation failed: status=$status, message=$message" }
                InstallResult.Failure(status, message)
            }
        }
        
        resultChannel.trySend(result)
    }
    
    companion object {
        const val ACTION_INSTALL_STATUS = "com.navi.phantom.INSTALL_STATUS"
        val resultChannel = Channel<InstallResult>(Channel.BUFFERED)
    }
}
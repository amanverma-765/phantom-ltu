package com.navi.phantom.features.permissions.logic

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.navi.phantom.features.permissions.components.EnableGpsDialog

@Composable
internal fun GpsStateHandler(
    onGpsEnabled: () -> Unit,
    onGpsDisabled: () -> Unit,
    showDialogIfDisabled: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isGpsEnabled by remember { mutableStateOf(checkGpsEnabled(context)) }
    var showEnableDialog by remember { mutableStateOf(false) }
    var hasCheckedOnce by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = checkGpsEnabled(context)

                if (isGpsEnabled) {
                    onGpsEnabled()
                } else if (hasCheckedOnce) {
                    onGpsDisabled()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initial check
    if (!hasCheckedOnce) {
        hasCheckedOnce = true
        if (isGpsEnabled) {
            onGpsEnabled()
        } else if (showDialogIfDisabled) {
            showEnableDialog = true
        } else {
            onGpsDisabled()
        }
    }

    if (showEnableDialog) {
        EnableGpsDialog(
            onEnableGps = {
                showEnableDialog = false
                context.openLocationSettings()
            },
            onDismiss = {
                showEnableDialog = false
                onGpsDisabled()
            }
        )
    }
}

@Composable
internal fun rememberGpsEnabledState(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isGpsEnabled by remember { mutableStateOf(checkGpsEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGpsEnabled = checkGpsEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return isGpsEnabled
}

fun checkGpsEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

fun Context.openLocationSettings() {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

package com.navi.phantom.features.permissions.logic

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.navi.phantom.features.permissions.components.PermissionDeniedDialog
import com.navi.phantom.features.permissions.components.PermissionRationaleDialog

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

@Composable
internal fun LocationPermissionHandler(
    onGranted: (isFineLocation: Boolean) -> Unit,
    onDenied: () -> Unit,
    onPermanentlyDenied: () -> Unit,
    requestOnStart: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()

    var hasRequestedOnce by rememberSaveable { mutableStateOf(false) }
    var showRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var showDeniedDialog by rememberSaveable { mutableStateOf(false) }
    var pendingSettingsReturn by rememberSaveable { mutableStateOf(false) }

    val currentPermissionState = remember(context, hasRequestedOnce) {
        checkPermissionState(context, activity, hasRequestedOnce)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasRequestedOnce = true
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        when {
            fineGranted -> onGranted(true)
            coarseGranted -> onGranted(false)
            else -> {
                val shouldShowRationale = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                } ?: false

                if (shouldShowRationale) {
                    onDenied()
                } else {
                    showDeniedDialog = true
                }
            }
        }
    }

    fun requestPermission() {
        permissionLauncher.launch(locationPermissions)
    }

    fun handlePermissionRequest() {
        val state = checkPermissionState(context, activity, hasRequestedOnce)
        when (state) {
            is LocationPermissionState.Granted -> onGranted(true)
            is LocationPermissionState.CoarseOnly -> onGranted(false)
            is LocationPermissionState.RequiresRationale -> showRationaleDialog = true
            is LocationPermissionState.NotRequested,
            is LocationPermissionState.Denied -> requestPermission()
            is LocationPermissionState.PermanentlyDenied -> showDeniedDialog = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingSettingsReturn) {
                pendingSettingsReturn = false
                val state = checkPermissionState(context, activity, hasRequestedOnce)
                when (state) {
                    is LocationPermissionState.Granted -> onGranted(true)
                    is LocationPermissionState.CoarseOnly -> onGranted(false)
                    else -> onPermanentlyDenied()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(requestOnStart, currentPermissionState) {
        if (requestOnStart) {
            handlePermissionRequest()
        }
    }

    if (showRationaleDialog) {
        PermissionRationaleDialog(
            onGrantPermission = {
                showRationaleDialog = false
                requestPermission()
            },
            onDismiss = {
                showRationaleDialog = false
                onDenied()
            }
        )
    }

    if (showDeniedDialog) {
        PermissionDeniedDialog(
            onOpenSettings = {
                showDeniedDialog = false
                pendingSettingsReturn = true
                context.openAppSettings()
            },
            onDismiss = {
                showDeniedDialog = false
                onPermanentlyDenied()
            }
        )
    }
}

@Composable
internal fun rememberCurrentPermissionState(): LocationPermissionState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()

    var permissionState by remember {
        mutableStateOf(checkPermissionState(context, activity, false))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = checkPermissionState(context, activity, false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return permissionState
}

private fun checkPermissionState(
    context: Context,
    activity: Activity?,
    hasRequestedOnce: Boolean
): LocationPermissionState {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return when {
        fineGranted -> LocationPermissionState.Granted
        coarseGranted -> LocationPermissionState.CoarseOnly
        activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) -> LocationPermissionState.RequiresRationale
        hasRequestedOnce -> LocationPermissionState.PermanentlyDenied
        else -> LocationPermissionState.NotRequested
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

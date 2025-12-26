package com.riva.mods.utils

import android.content.Context
import android.util.Log

object AssetUtils {

    private const val TAG = "AssetUtils"

    private val REQUIRED_ASSETS = listOf(
        "lspatch/loader.dex",
        "lspatch/metaloader.dex",
        "lspatch/so/arm64-v8a/liblspatch.so",
        "lspatch/so/armeabi-v7a/liblspatch.so",
        "lspatch/so/x86/liblspatch.so",
        "lspatch/so/x86_64/liblspatch.so"
    )

    fun validateLspatchAssets(context: Context) {
        val missing = REQUIRED_ASSETS.filter { asset ->
            runCatching {
                context.assets.open(asset).use { it.read() != -1 }
            }.getOrDefault(false).not()
        }

        if (missing.isNotEmpty()) {
            val error = "Missing LSPatch assets: ${missing.joinToString()}"
            Log.e(TAG, error)
            throw IllegalStateException(error)
        }

        Log.i(TAG, "LSPatch assets validated")
    }
}
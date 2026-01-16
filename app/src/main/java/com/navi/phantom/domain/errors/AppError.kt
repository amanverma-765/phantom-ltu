package com.navi.phantom.domain.errors

import java.io.PrintWriter
import java.io.StringWriter

sealed class AppError(val message: String) {
    // Apps feature errors
    data object PermissionDenied : AppError("Permission required to read installed apps")

    // Bootstrap feature errors
    data object AppDetailsLoadFailed : AppError("Failed to load app details")
    data object BootstrapFailed : AppError("Bootstrap failed. Please try again")

    // Generic errors
    data object Unknown : AppError("Something went wrong. Please try again")
}

/**
 * Detailed error information for bootstrap failures.
 * Captures the full stack trace for debugging purposes.
 */
sealed class BootstrapError(
    val title: String,
    val message: String,
    open val cause: Throwable? = null
) {
    val stackTrace: String
        get() = cause?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        } ?: ""

    val fullErrorLog: String
        get() = buildString {
            appendLine("=== Bootstrap Error Report ===")
            appendLine("Error Type: ${this@BootstrapError::class.simpleName}")
            appendLine("Title: $title")
            appendLine("Message: $message")
            appendLine()
            if (cause != null) {
                appendLine("=== Exception Details ===")
                appendLine("Exception: ${cause!!::class.qualifiedName}")
                appendLine("Cause Message: ${cause!!.message}")
                appendLine()
                appendLine("=== Stack Trace ===")
                append(stackTrace)
            }
        }

    data class KeystoreNotFound(
        override val cause: Throwable? = null
    ) : BootstrapError(
        title = "Keystore Not Found",
        message = "The signing keystore could not be found. Please configure a valid keystore.",
        cause = cause
    )

    data class ApkNotFound(
        val apkPath: String,
        override val cause: Throwable? = null
    ) : BootstrapError(
        title = "APK Not Found",
        message = "The source APK file does not exist: $apkPath",
        cause = cause
    )

    data class InvalidApk(
        val apkPath: String,
        override val cause: Throwable? = null
    ) : BootstrapError(
        title = "Invalid APK",
        message = "The provided file is not a valid APK: $apkPath",
        cause = cause
    )

    data class ManifestParseError(
        override val cause: Throwable? = null
    ) : BootstrapError(
        title = "Manifest Parse Error",
        message = "Failed to parse the AndroidManifest.xml",
        cause = cause
    )

    data class SignatureExtractionError(
        override val cause: Throwable? = null
    ) : BootstrapError(
        title = "Signature Extraction Failed",
        message = "Failed to extract the original APK signature",
        cause = cause
    )

    data class SigningError(
        override val cause: Throwable? = null
    ) : BootstrapError(
        title = "Signing Error",
        message = "Failed to sign the bootstrapped APK",
        cause = cause
    )

    data class IoError(
        val operation: String,
        override val cause: Throwable? = null
    ) : BootstrapError(
        title = "I/O Error",
        message = "An I/O error occurred during: $operation",
        cause = cause
    )

    data class Cancelled(
        override val cause: Throwable? = null
    ) : BootstrapError(
        title = "Cancelled",
        message = "Bootstrap was cancelled by user",
        cause = cause
    )

    data class UnknownError(
        override val cause: Throwable? = null
    ) : BootstrapError(
        title = "Unknown Error",
        message = cause?.message ?: "An unexpected error occurred during bootstrap",
        cause = cause
    )

    companion object {
        /**
         * Maps a Throwable to the appropriate BootstrapError type.
         */
        fun fromThrowable(throwable: Throwable): BootstrapError {
            val message = throwable.message ?: ""
            return when {
                message.contains("keystore", ignoreCase = true) ||
                message.contains("Default keystore resource not found") -> KeystoreNotFound(throwable)

                message.contains("does not exist") ||
                message.contains("source apk file does not exist", ignoreCase = true) -> ApkNotFound(message, throwable)

                message.contains("not a valid apk", ignoreCase = true) ||
                message.contains("Provided file is not a valid apk", ignoreCase = true) -> InvalidApk(message, throwable)

                message.contains("manifest", ignoreCase = true) -> ManifestParseError(throwable)

                message.contains("signature", ignoreCase = true) -> SignatureExtractionError(throwable)

                message.contains("signer", ignoreCase = true) ||
                message.contains("sign", ignoreCase = true) -> SigningError(throwable)

                throwable is java.io.IOException -> IoError(message, throwable)

                throwable is kotlin.coroutines.cancellation.CancellationException -> Cancelled(throwable)

                else -> UnknownError(throwable)
            }
        }
    }
}
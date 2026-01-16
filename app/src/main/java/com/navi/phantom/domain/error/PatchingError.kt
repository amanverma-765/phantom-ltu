package com.navi.phantom.domain.error

import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

sealed class PatchingError(
    override val title: String,
    override val message: String,
    override val cause: Throwable? = null
) : PhantomError {

    data class KeystoreNotFound(override val cause: Throwable? = null) : PatchingError(
        title = "Keystore Not Found",
        message = "The signing keystore could not be found",
        cause = cause
    )

    data class ApkNotFound(val path: String, override val cause: Throwable? = null) : PatchingError(
        title = "APK Not Found",
        message = "Source APK does not exist: $path",
        cause = cause
    )

    data class InvalidApk(val path: String, override val cause: Throwable? = null) : PatchingError(
        title = "Invalid APK",
        message = "Not a valid APK: $path",
        cause = cause
    )

    data class ManifestParseFailed(override val cause: Throwable? = null) : PatchingError(
        title = "Manifest Parse Error",
        message = "Failed to parse AndroidManifest.xml",
        cause = cause
    )

    data class SignatureExtractFailed(override val cause: Throwable? = null) : PatchingError(
        title = "Signature Extraction Failed",
        message = "Failed to extract original APK signature",
        cause = cause
    )

    data class SigningFailed(override val cause: Throwable? = null) : PatchingError(
        title = "Signing Error",
        message = "Failed to sign the patched APK",
        cause = cause
    )

    data class IoFailed(val operation: String, override val cause: Throwable? = null) : PatchingError(
        title = "I/O Error",
        message = "I/O error during: $operation",
        cause = cause
    )

    data class Cancelled(override val cause: Throwable? = null) : PatchingError(
        title = "Cancelled",
        message = "Patching was cancelled",
        cause = cause
    )

    data class Unknown(override val cause: Throwable? = null) : PatchingError(
        title = "Unknown Error",
        message = cause?.message ?: "An unexpected error occurred",
        cause = cause
    )

    companion object {
        fun from(throwable: Throwable): PatchingError {
            val msg = throwable.message.orEmpty().lowercase()
            return when {
                throwable is CancellationException -> Cancelled(throwable)
                throwable is IOException -> IoFailed(throwable.message.orEmpty(), throwable)
                "keystore" in msg -> KeystoreNotFound(throwable)
                "does not exist" in msg -> ApkNotFound(msg, throwable)
                "not a valid apk" in msg -> InvalidApk(msg, throwable)
                "manifest" in msg -> ManifestParseFailed(throwable)
                "signature" in msg -> SignatureExtractFailed(throwable)
                "sign" in msg -> SigningFailed(throwable)
                else -> Unknown(throwable)
            }
        }
    }
}

package com.navi.phantom.domain.error

sealed class InstallationError(
    override val title: String,
    override val message: String,
    override val cause: Throwable? = null
) : PhantomError {

    data class FileNotFound(val path: String) : InstallationError(
        title = "File Not Found",
        message = "Installation file does not exist: $path"
    )

    data class InvalidPackage(
        val reason: String,
        override val cause: Throwable? = null
    ) : InstallationError(
        title = "Invalid Package",
        message = reason,
        cause = cause
    )

    data class Aborted(val reason: String? = null) : InstallationError(
        title = "Installation Aborted",
        message = reason ?: "Installation was aborted"
    )

    data class Blocked(val reason: String? = null) : InstallationError(
        title = "Installation Blocked",
        message = reason ?: "Installation was blocked"
    )

    data class Conflict(val reason: String? = null) : InstallationError(
        title = "Package Conflict",
        message = reason ?: "Package conflict detected"
    ) {
        val otherPackageName: String? get() = reason?.let(::extractPackageName)
    }

    data class SignatureMismatch(val packageName: String) : InstallationError(
        title = "Signature Mismatch",
        message = "An app with a different signature is already installed"
    )

    data class Incompatible(val reason: String? = null) : InstallationError(
        title = "Incompatible Package",
        message = reason ?: "Package is incompatible with this device"
    )

    data class Invalid(val reason: String? = null) : InstallationError(
        title = "Invalid Package",
        message = reason ?: "Package is invalid"
    )

    data class Storage(val reason: String? = null) : InstallationError(
        title = "Storage Error",
        message = reason ?: "Storage error occurred"
    )

    data class Timeout(override val cause: Throwable? = null) : InstallationError(
        title = "Installation Timeout",
        message = "Installation timed out",
        cause = cause
    )

    data class Exceptional(override val cause: Throwable) : InstallationError(
        title = "Installation Error",
        message = cause.message ?: "An exception occurred",
        cause = cause
    )

    data class Generic(val reason: String? = null) : InstallationError(
        title = "Installation Failed",
        message = reason ?: "Installation failed"
    )

    data object Cancelled : InstallationError(
        title = "Cancelled",
        message = "Installation was cancelled"
    )

    companion object {
        private val PACKAGE_REGEX = Regex("""Package\s+([\w.]+)""")

        fun extractPackageName(message: String): String? =
            PACKAGE_REGEX.find(message)?.groupValues?.getOrNull(1)
    }
}

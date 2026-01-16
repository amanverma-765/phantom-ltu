package com.navi.phantom.domain.error

interface PhantomError {
    val title: String
    val message: String
    val cause: Throwable?

    val stackTrace: String
        get() = cause?.stackTraceToString().orEmpty()

    val fullErrorLog: String
        get() = buildString {
            appendLine("=== ${this@PhantomError::class.simpleName} ===")
            appendLine("Title: $title")
            appendLine("Message: $message")
            cause?.let {
                appendLine()
                appendLine("=== Exception ===")
                appendLine("Type: ${it::class.qualifiedName}")
                appendLine("Cause: ${it.message}")
                appendLine()
                appendLine("=== Stack Trace ===")
                append(stackTrace)
            }
        }
}

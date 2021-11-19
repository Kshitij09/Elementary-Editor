package com.kshitijpatil.elementaryeditor.util

import timber.log.Timber

/**
 * Checks given [value] for nullability and uses [loggerImpl]
 * to log the error message provided by the [lazyErrorMessage]
 */
fun <T> tapNullWithLogger(
    value: T?,
    loggerImpl: (String) -> Unit,
    lazyErrorMessage: () -> String
): T? {
    if (value == null) {
        val message = lazyErrorMessage()
        loggerImpl(message)
    }
    return value
}

/**
 * Uses [Timber] to log the nullability error messages
 */
fun <T> tapNullWithTimber(value: T?, lazyErrorMessage: () -> String): T? {
    return tapNullWithLogger(
        value = value,
        loggerImpl = { Timber.e(it) },
        lazyErrorMessage = lazyErrorMessage
    )
}
package com.kshitijpatil.elementaryeditor.util

import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy

/**
 * Creates a [OneTimeWorkRequest] with the given inputData and a [tag] if set.
 */
inline fun <reified T : ListenableWorker> workRequest(
    inputData: Data,
    expedited: Boolean = true,
    tag: String? = null
) =
    OneTimeWorkRequestBuilder<T>().apply {
        setInputData(inputData)
        if (expedited)
            setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        if (!tag.isNullOrEmpty()) {
            addTag(tag)
        }
    }.build()
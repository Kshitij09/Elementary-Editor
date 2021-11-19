package com.kshitijpatil.elementaryeditor.util

import androidx.work.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformWhile

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

fun Flow<WorkInfo>.takeWhileFinished(): Flow<WorkInfo> =
    transformWhile {
        emit(it)
        !it.state.isFinished
    }
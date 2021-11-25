package com.kshitijpatil.elementaryeditor.ui.edit.middleware

import androidx.lifecycle.asFlow
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditAction
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditMiddleware
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditViewState
import com.kshitijpatil.elementaryeditor.ui.edit.contract.InternalAction
import com.kshitijpatil.elementaryeditor.util.takeWhileFinished
import com.kshitijpatil.elementaryeditor.util.tapNullWithTimber
import com.kshitijpatil.elementaryeditor.util.toOffsetBounds
import com.kshitijpatil.elementaryeditor.util.workRequest
import com.kshitijpatil.elementaryeditor.worker.EditImageWorker
import com.kshitijpatil.elementaryeditor.worker.WorkerConstants
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class CropMiddleware(private val workManager: WorkManager) : EditMiddleware {
    override fun bind(
        actions: Flow<EditAction>,
        state: StateFlow<EditViewState>
    ): Flow<EditAction> {
        return actions.filter { it is InternalAction.MutatingAction.PerformCrop }
            .flatMapMerge {
                channelFlow {
                    val workData = prepareWorkData(state.value)
                    if (workData == null) {
                        send(InternalAction.CropFailed)
                        return@channelFlow
                    }
                    val request = workRequest<EditImageWorker>(workData)
                    workManager.enqueue(request)
                    launch { observeCropWorkerForCompletion(request.id) }
                }
            }
    }

    private suspend fun ProducerScope<InternalAction>.observeCropWorkerForCompletion(requestId: UUID) {
        workManager.getWorkInfoByIdLiveData(requestId).asFlow()
            .takeWhileFinished()
            .collect { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val newImageUri =
                            workInfo.outputData.getString(WorkerConstants.KEY_IMAGE_URI)
                        // TODO: Fix it
                        //newImageUri?.toUri()?.let { send(InternalAction.CropSucceeded(it)) }
                    }
                    WorkInfo.State.FAILED -> {
                        send(InternalAction.CropFailed)
                    }
                    WorkInfo.State.RUNNING -> {
                        send(InternalAction.Cropping)
                    }
                    else -> {
                    }
                }
            }
    }

    private fun prepareWorkData(editViewState: EditViewState): Data? {
        val imageBounds = tapNullWithTimber(editViewState.cropState.imageBounds) {
            "CropImageWorker: Image Bounds were not set, skipping..."
        } ?: return null
        val cropBounds = tapNullWithTimber(editViewState.cropState.cropBounds) {
            "CropImageWorker: Crop Bounds were not set, skipping..."
        } ?: return null
        val imageUri = tapNullWithTimber(editViewState.currentImageUri) {
            "CropImageWorker: Target ImageUri was not set, skipping..."
        } ?: return null
        val viewWidth = imageBounds.width()
        val viewHeight = imageBounds.height()
        return workDataOf(
            WorkerConstants.KEY_IMAGE_URI to imageUri.toString(),
            WorkerConstants.KEY_CROP_BOUNDS to toOffsetBounds(imageBounds, cropBounds),
            WorkerConstants.KEY_VIEW_WIDTH to viewWidth,
            WorkerConstants.KEY_VIEW_HEIGHT to viewHeight
        )
    }
}
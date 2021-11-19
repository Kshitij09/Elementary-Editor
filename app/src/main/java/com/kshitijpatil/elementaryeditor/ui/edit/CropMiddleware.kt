package com.kshitijpatil.elementaryeditor.ui.edit

import android.graphics.Rect
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.asFlow
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kshitijpatil.elementaryeditor.util.workRequest
import com.kshitijpatil.elementaryeditor.worker.CropImageWorker
import com.kshitijpatil.elementaryeditor.worker.WorkerConstants
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class CropMiddleware(private val workManager: WorkManager) : MiddleWare<EditAction, EditViewState> {
    override fun bind(
        actions: Flow<EditAction>,
        state: StateFlow<EditViewState>
    ): Flow<EditAction> {
        return actions.filter { it is InternalAction.PerformCrop }
            .flatMapMerge {
                //flowOf(InternalAction.Cropping, InternalAction.CropFailed)
                channelFlow {
                    val workData = prepareWorkData(state.value)
                    if (workData == null) {
                        send(InternalAction.CropFailed)
                        return@channelFlow
                    }
                    val request = workRequest<CropImageWorker>(workData)
                    workManager.enqueue(request)
                    launch { observeCropWorkerForCompletion(request.id) }
                }
            }
    }
    /*override fun bind(actions: EditAction, state: EditViewState): Flow<EditAction> {
        return if (actions == InternalAction.PerformCrop) {
            channelFlow {
                val workData = prepareWorkData(state)
                if (workData == null) {
                    send(InternalAction.CropFailed)
                    return@channelFlow
                }
                val request = workRequest<CropImageWorker>(workData)
                workManager.enqueue(request)
                launch { observeCropWorkerForCompletion(request.id) }
            }
        } else emptyFlow()
    }*/

    private suspend fun ProducerScope<InternalAction>.observeCropWorkerForCompletion(requestId: UUID) {
        workManager.getWorkInfoByIdLiveData(requestId).asFlow()
            .collect { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val newImageUri =
                            workInfo.outputData.getString(WorkerConstants.KEY_IMAGE_URI)
                        newImageUri?.toUri()?.let { send(InternalAction.CropSucceeded(it)) }
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

    private fun getImageBounds(editViewState: EditViewState): Rect? {
        val imageBounds = editViewState.cropState.imageBounds
        if (imageBounds == null) {
            Timber.e("Image Bounds were not set, skipping...")
            return null
        }
        return imageBounds
    }

    private fun getCropBounds(editViewState: EditViewState): Rect? {
        val cropBounds = editViewState.cropState.cropBounds
        if (cropBounds == null) {
            Timber.e("Crop Bounds were not set, skipping...")
            return null
        }
        return cropBounds
    }

    private fun getCurrentImageUri(editViewState: EditViewState): Uri? {
        val imageUri = editViewState.currentImageUri
        if (imageUri == null) {
            Timber.e("Target ImageUri was not set, skipping...")
            return null
        }
        return imageUri
    }

    private fun prepareWorkData(editViewState: EditViewState): Data? {
        val imageBounds = getImageBounds(editViewState) ?: return null
        val cropBounds = getCropBounds(editViewState) ?: return null
        val imageUri = getCurrentImageUri(editViewState)
        val viewWidth = imageBounds.width()
        val viewHeight = imageBounds.height()
        return workDataOf(
            WorkerConstants.KEY_IMAGE_URI to imageUri.toString(),
            WorkerConstants.KEY_CROP_BOUNDS to toOffsetBounds(imageBounds, cropBounds),
            WorkerConstants.KEY_VIEW_WIDTH to viewWidth,
            WorkerConstants.KEY_VIEW_HEIGHT to viewHeight
        )
    }

    /**
     * @return [IntArray] crop region coordinates in
     *  the (offsetX, offsetY, width, height) order. If the [initialBounds]
     *  or [currentBounds] are not initialized, the method will return null.
     */
    private fun toOffsetBounds(initialBounds: Rect, currentBounds: Rect): IntArray {
        val offsetX = currentBounds.left - initialBounds.left
        val offsetY = currentBounds.top - initialBounds.top
        val width = currentBounds.right - currentBounds.left
        val height = currentBounds.bottom - currentBounds.top
        return intArrayOf(offsetX, offsetY, width, height)
    }
}
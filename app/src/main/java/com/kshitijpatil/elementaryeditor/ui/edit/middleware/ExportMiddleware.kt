package com.kshitijpatil.elementaryeditor.ui.edit.middleware

import android.graphics.Bitmap
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kshitijpatil.elementaryeditor.data.EditPayload
import com.kshitijpatil.elementaryeditor.ui.edit.contract.*
import com.kshitijpatil.elementaryeditor.util.chronicle.Chronicle
import com.kshitijpatil.elementaryeditor.util.workRequest
import com.kshitijpatil.elementaryeditor.worker.EditImageWorker
import com.kshitijpatil.elementaryeditor.worker.SaveImageToGalleryWorker
import com.kshitijpatil.elementaryeditor.worker.WorkerConstants
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber

class ExportMiddleware(
    private val bitmapChronicle: Chronicle<Pair<Bitmap, EditPayload?>>,
    private val editPayloadListJsonAdapter: JsonAdapter<List<EditPayload>>,
    private val workManager: WorkManager
) : EditMiddleware {

    override fun bind(
        actions: Flow<EditAction>,
        state: StateFlow<EditViewState>
    ): Flow<EditAction> {
        return actions.filter { it is Export }
            .flatMapLatest {
                val currentState = state.value
                channelFlow {
                    val imageBounds = currentState.cropState.imageBounds
                    if (imageBounds == null) {
                        Timber.e("Image Bounds were not set, skipping export...")
                        send(InternalAction.ExportFailed)
                        return@channelFlow
                    }
                    val imageUri = currentState.currentImageUri
                    if (imageUri == null) {
                        Timber.e("Target ImageUri was not set, skipping...")
                        send(InternalAction.ExportFailed)
                        return@channelFlow
                    }

                    val viewWidth = imageBounds.width()
                    val viewHeight = imageBounds.height()
                    send(InternalAction.Exporting)
                    val editPayloadJson = withContext(Dispatchers.Default) {
                        runCatching {
                            val outstandingPayloads =
                                bitmapChronicle.toList().mapNotNull { it.second }
                            editPayloadListJsonAdapter.toJson(outstandingPayloads)
                        }.getOrNull()
                    }
                    if (editPayloadJson == null) {
                        Timber.e("Failed creating payload JSON, skipping...")
                        send(InternalAction.ExportFailed)
                        return@channelFlow
                    }

                    val inputData = workDataOf(
                        WorkerConstants.KEY_IMAGE_URI to imageUri.toString(),
                        WorkerConstants.KEY_VIEW_WIDTH to viewWidth,
                        WorkerConstants.KEY_VIEW_HEIGHT to viewHeight,
                        WorkerConstants.KEY_EDIT_PAYLOAD to editPayloadJson
                    )
                    val request = workRequest<EditImageWorker>(inputData)
                    workManager.beginWith(request)
                        .then(workRequest<SaveImageToGalleryWorker>())
                        .enqueue()
                    send(InternalAction.EditWorkerScheduled(request.id))
                }
            }
    }
}
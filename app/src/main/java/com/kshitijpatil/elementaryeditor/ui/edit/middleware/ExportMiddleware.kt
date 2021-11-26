package com.kshitijpatil.elementaryeditor.ui.edit.middleware

import android.graphics.Bitmap
import android.util.Size
import androidx.work.WorkManager
import com.kshitijpatil.elementaryeditor.data.EditPayload
import com.kshitijpatil.elementaryeditor.domain.CombineAdjacentStrategy
import com.kshitijpatil.elementaryeditor.domain.ReduceCropPayloadStrategy2
import com.kshitijpatil.elementaryeditor.domain.ReduceRotatePayloadStrategy1
import com.kshitijpatil.elementaryeditor.ui.edit.contract.*
import com.kshitijpatil.elementaryeditor.util.chronicle.Chronicle
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
                    val imageUri = currentState.currentImageUri
                    if (imageUri == null) {
                        Timber.e("Target ImageUri was not set, skipping...")
                        send(InternalAction.ExportFailed)
                        return@channelFlow
                    }

                    send(InternalAction.Exporting)
                    currentState.imageSize?.let { combinePayloads(it) }
                    /*val editPayloadJson = getEditPayloadJson()
                    if (editPayloadJson == null) {
                        Timber.e("Failed creating payload JSON, skipping...")
                        send(InternalAction.ExportFailed)
                        return@channelFlow
                    }

                    val inputData = workDataOf(
                        WorkerConstants.KEY_IMAGE_URI to imageUri.toString(),
                        WorkerConstants.KEY_EDIT_PAYLOAD to editPayloadJson
                    )
                    val editRequest = workRequest<EditImageWorker>(inputData)
                    val saveRequest = workRequest<SaveImageToGalleryWorker>()
                    workManager.beginWith(editRequest)
                        .then(saveRequest)
                        .enqueue()
                    send(InternalAction.ExportWorkersScheduled(editRequest.id, saveRequest.id))*/
                }
            }
    }

    private val combinePayloadStrategy =
        CombineAdjacentStrategy(ReduceCropPayloadStrategy2, ReduceRotatePayloadStrategy1)

    private suspend fun combinePayloads(imageSize: Size) {
        return withContext(Dispatchers.Default) {
            val outstandingPayloads = bitmapChronicle.toList()
                .mapNotNull { it.second } as List<EditPayload.Crop>
            Timber.d("[Before Combine] Edit Payloads: $outstandingPayloads")
            val reducedPayload = combinePayloadStrategy.combine(
                outstandingPayloads,
                imageSize.width,
                imageSize.height
            )
            /*val reducedPayload = outstandingPayloads.reduce { acc, current ->
                val offsetX = acc.cropBoundScaled.offsetX + current.cropBoundScaled.offsetX
                val offsetY = acc.cropBoundScaled.offsetY + current.cropBoundScaled.offsetY
                val accLeftOffset = imageSize.width - (acc.cropBoundScaled.offsetX + acc.cropBoundScaled.width)
                val currentLeftOffset = imageSize.width - (current.cropBoundScaled.offsetX + current.cropBoundScaled.width)
                val width = imageSize.width - accLeftOffset - currentLeftOffset

                val accBottomOffset = imageSize.height - (acc.cropBoundScaled.offsetY + acc.cropBoundScaled.height)
                val currentBottomOffset = imageSize.height - (current.cropBoundScaled.offsetY + current.cropBoundScaled.height)
                val height = imageSize.height - accBottomOffset - currentBottomOffset
                EditPayload.Crop(Bound(offsetX, offsetY, width, height))
            }*/
            Timber.d("[Reduced] EditPayload: $reducedPayload")
        }
    }

    private suspend fun getEditPayloadJson(): String? {
        return withContext(Dispatchers.Default) {
            runCatching {
                val outstandingPayloads = bitmapChronicle.toList()
                    .mapNotNull { it.second }
                editPayloadListJsonAdapter.toJson(outstandingPayloads)
            }.getOrNull()
        }
    }
}
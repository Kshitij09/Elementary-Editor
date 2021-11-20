package com.kshitijpatil.elementaryeditor.ui.edit.middleware

import com.bumptech.glide.Glide
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditAction
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditMiddleware
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditViewState
import com.kshitijpatil.elementaryeditor.ui.edit.contract.InternalAction
import com.kshitijpatil.elementaryeditor.util.toOffsetBounds
import com.kshitijpatil.elementaryeditor.worker.OffsetCropTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*


class CropBitmapMiddleware : EditMiddleware {
    override fun bind(
        actions: Flow<EditAction>,
        state: StateFlow<EditViewState>
    ): Flow<EditAction> {
        return actions.filter { it is InternalAction.PerformCrop }
            .map { (it as InternalAction.PerformCrop).context }
            .flatMapLatest { context ->
                val currentState = state.value
                channelFlow {
                    val bitmap = currentState.currentBitmap
                    if (bitmap == null) {
                        Timber.e("$TAG: Current Bitmap is null, returning...")
                        send(InternalAction.CropFailed)
                        close()
                        return@channelFlow
                    }
                    send(InternalAction.PersistBitmap(context, bitmap))

                    val imageBounds = currentState.cropState.imageBounds
                    if (imageBounds == null) {
                        Timber.e("$TAG: Image Bounds were not set, skipping...")
                        send(InternalAction.CropFailed)
                        close()
                        return@channelFlow
                    }
                    val cropBounds = currentState.cropState.cropBounds
                    if (cropBounds == null) {
                        Timber.e("$TAG: Crop Bounds were not set, skipping...")
                        send(InternalAction.CropFailed)
                        close()
                        return@channelFlow
                    }
                    send(InternalAction.Cropping)
                    val viewWidth = imageBounds.width()
                    val viewHeight = imageBounds.height()
                    val cropJob = launch(Dispatchers.Default) {
                        val glideTarget = Glide.with(context)
                            .asBitmap()
                            .load(bitmap)
                            .transform(
                                OffsetCropTransformation(
                                    cropBounds = toOffsetBounds(imageBounds, cropBounds),
                                    viewWidth = viewWidth,
                                    viewHeight = viewHeight
                                )
                            ).submit()

                        val cropped = glideTarget.get()
                        // wait for current bitmap to get persisted before
                        // emitting a signal to modify the same
                        //state.first { it.bitmapPersisted }
                        send(InternalAction.CropSucceeded(cropped))
                    }
                    awaitClose { cropJob.cancel() }
                }
            }
    }

    companion object {
        private const val TAG = "InMemoryCrop"
    }
}

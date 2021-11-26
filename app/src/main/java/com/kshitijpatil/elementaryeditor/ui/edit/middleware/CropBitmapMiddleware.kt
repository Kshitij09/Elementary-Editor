package com.kshitijpatil.elementaryeditor.ui.edit.middleware

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Size
import com.bumptech.glide.Glide
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditAction
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditMiddleware
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditViewState
import com.kshitijpatil.elementaryeditor.ui.edit.contract.InternalAction
import com.kshitijpatil.elementaryeditor.util.glide.OffsetCropTransformationV2
import com.kshitijpatil.elementaryeditor.util.tapNullWithTimber
import com.kshitijpatil.elementaryeditor.util.toOffsetBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*


class CropBitmapMiddleware : EditMiddleware {
    override fun bind(
        actions: Flow<EditAction>,
        state: StateFlow<EditViewState>
    ): Flow<EditAction> {
        return actions.filter { it is InternalAction.MutatingAction.PerformCrop }
            .map { (it as InternalAction.MutatingAction.PerformCrop).context }
            // cancels upstream flow in favor of new downstream events
            .flatMapLatest { context ->
                val currentState = state.value
                channelFlow {
                    val params = prepareParams(currentState) ?: return@channelFlow
                    val offsetBounds = toOffsetBounds(params.viewBounds, params.cropBounds)
                    val scaledBounds = offsetBounds.scaleBy(
                        scaleX = params.imageSize.width / params.viewBounds.width().toFloat(),
                        scaleY = params.imageSize.height / params.viewBounds.height().toFloat()
                    )
                    send(InternalAction.Cropping)
                    val cropJob = launch(Dispatchers.Default) {
                        val glideTarget = Glide.with(context)
                            .asBitmap()
                            .load(params.bitmap)
                            .transform(OffsetCropTransformationV2(scaledBounds))
                            .submit()

                        val cropped = glideTarget.get()
                        // wait for current bitmap to get persisted before
                        // emitting a signal to modify the same
                        //state.first { it.bitmapPersisted }
                        send(InternalAction.CropSucceeded(cropped))
                        /*send(
                            InternalAction.PersistBitmap(
                                cropped,
                                EditPayload.Crop(cropOffsetBounds, viewWidth, viewHeight)
                            )
                        )*/
                    }
                    cropJob.invokeOnCompletion {
                        it?.let {
                            trySend(InternalAction.CropFailed)
                        }
                    }
                }
            }
    }

    private fun prepareParams(viewState: EditViewState): Params? {
        val bitmap = tapNullWithTimber(
            viewState.currentBitmap,
            nullErrorMessageFor("currentBitmap")
        ) ?: return null
        val cropBounds = tapNullWithTimber(
            viewState.cropState.cropBounds,
            nullErrorMessageFor("cropBounds")
        ) ?: return null
        val imageBounds = tapNullWithTimber(
            viewState.cropState.imageBounds,
            nullErrorMessageFor("imageBounds")
        ) ?: return null
        val imageSize = tapNullWithTimber(
            viewState.imageSize,
            nullErrorMessageFor("imageSize")
        ) ?: return null
        return Params(bitmap, cropBounds, imageBounds, imageSize)
    }

    private fun nullErrorMessageFor(fieldName: String): () -> String {
        return { "'$fieldName' was null, returning..." }
    }

    internal data class Params(
        val bitmap: Bitmap,
        val cropBounds: Rect,
        val viewBounds: Rect,
        val imageSize: Size,
    )
}

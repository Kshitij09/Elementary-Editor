package com.kshitijpatil.elementaryeditor.ui.edit.middleware

import com.bumptech.glide.Glide
import com.kshitijpatil.elementaryeditor.data.EditPayload
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditAction
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditMiddleware
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditViewState
import com.kshitijpatil.elementaryeditor.ui.edit.contract.InternalAction
import com.kshitijpatil.elementaryeditor.util.glide.RotateTransformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import timber.log.Timber

class RotateBitmapMiddleware : EditMiddleware {
    override fun bind(
        actions: Flow<EditAction>,
        state: StateFlow<EditViewState>
    ): Flow<EditAction> {
        return actions.filter { it is InternalAction.MutatingAction.Rotate }
            .map { it as InternalAction.MutatingAction.Rotate }
            // Cancel ongoing rotate operations
            .flatMapLatest { action ->
                val currentState = state.value
                channelFlow {
                    val bitmap = currentState.currentBitmap
                    if (bitmap == null) {
                        Timber.e("Current Bitmap is null, returning...")
                        send(InternalAction.RotateFailed)
                    }
                    send(InternalAction.BitmapLoading)
                    val rotationAngle = currentState.rotateState.rotationAngle
                    Timber.d("Rotating bitmap by $rotationAngle degrees")
                    withContext(Dispatchers.Default) {
                        val glideTarget = Glide.with(action.context)
                            .asBitmap()
                            .load(bitmap)
                            .transform(RotateTransformation(rotationAngle))
                            .submit()

                        val rotated = glideTarget.get()
                        send(InternalAction.RotateSucceeded(rotated))
                        send(
                            InternalAction.PersistBitmap(
                                rotated,
                                rotatePayloadFrom(rotationAngle)
                            )
                        )
                    }
                }
            }
    }

    private fun rotatePayloadFrom(rotationAngle: Float): EditPayload.Rotate {
        return EditPayload.Rotate(rotationAngle)
    }
}
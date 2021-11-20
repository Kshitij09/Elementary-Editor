package com.kshitijpatil.elementaryeditor.ui.edit.middleware

import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.kshitijpatil.elementaryeditor.ui.common.ReduxViewModel
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditAction
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditViewState
import com.kshitijpatil.elementaryeditor.ui.edit.contract.InternalAction
import com.kshitijpatil.elementaryeditor.ui.edit.contract.SetCurrentImageUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LoadBitmapFromUriMiddleware(
    private val sizeMultiplier: Float = 0.1f
) : ReduxViewModel.MiddleWare<EditAction, EditViewState> {
    override fun bind(
        actions: Flow<EditAction>,
        state: StateFlow<EditViewState>
    ): Flow<EditAction> {
        return actions.filter { it is SetCurrentImageUri }
            .map { it as SetCurrentImageUri }
            .map(::toGlideTarget)
            .flatMapLatest { glideTarget ->
                channelFlow {
                    val loadBitmapJob = launch(Dispatchers.Default) {
                        val bitmap = glideTarget.get()
                        send(InternalAction.CurrentBitmapUpdated(bitmap))
                    }
                    awaitClose {
                        loadBitmapJob.cancel()
                    }
                }
            }
    }

    private fun toGlideTarget(action: SetCurrentImageUri): FutureTarget<Bitmap> {
        return Glide.with(action.context)
            .asBitmap()
            .load(action.imageUri)
            .thumbnail(sizeMultiplier)
            .submit()
    }

}
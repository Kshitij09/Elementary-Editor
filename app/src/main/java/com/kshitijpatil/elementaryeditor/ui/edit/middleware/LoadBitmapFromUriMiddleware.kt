package com.kshitijpatil.elementaryeditor.ui.edit.middleware

import android.graphics.Bitmap
import android.util.Size
import com.bumptech.glide.Glide
import com.bumptech.glide.request.FutureTarget
import com.kshitijpatil.elementaryeditor.ui.common.ReduxViewModel
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditAction
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditViewState
import com.kshitijpatil.elementaryeditor.ui.edit.contract.InternalAction
import com.kshitijpatil.elementaryeditor.ui.edit.contract.SetCurrentImageUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

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
                    withContext(Dispatchers.Default) {
                        val bitmap = glideTarget.get()
                        send(InternalAction.BitmapLoaded(bitmap))
                        send(InternalAction.ImageSizeReceived(Size(bitmap.width, bitmap.height)))
                        send(InternalAction.PersistBitmap(bitmap))
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
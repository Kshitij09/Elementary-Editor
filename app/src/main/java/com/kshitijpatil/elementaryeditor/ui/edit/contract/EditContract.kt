package com.kshitijpatil.elementaryeditor.ui.edit.contract

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.kshitijpatil.elementaryeditor.ui.common.ReduxViewModel
import java.util.*


sealed interface EditAction
data class Confirm(val context: Context) : EditAction
object Cancel : EditAction
data class SetCurrentImageUri(val imageUri: Uri, val context: Context) : EditAction
data class SetActiveEditOperation(val operation: EditOperation) : EditAction

sealed class CropAction : EditAction {
    data class SetCropBounds(val cropBounds: Rect?) : CropAction()
    data class SetImageBounds(val imageBounds: Rect) : CropAction()
}

sealed class InternalAction : EditAction {
    data class PerformCrop(val context: Context) : InternalAction()
    data class PersistBitmap(val context: Context, val bitmap: Bitmap) : InternalAction()
    object Cropping : InternalAction()
    data class CropSucceeded(val bitmap: Bitmap) : InternalAction()
    data class CurrentBitmapUpdated(val bitmap: Bitmap) : InternalAction()
    object CropFailed : InternalAction()
}

data class CropState(
    val cropBounds: Rect? = null,
    val imageBounds: Rect? = null,
    val inProgress: Boolean = false
) {
    val cropBoundsModified: Boolean
        get() = (cropBounds != imageBounds)
}

enum class EditOperation {
    CROP, ROTATE
}

data class EditViewState(
    val activeEditOperation: EditOperation,
    val currentImageUri: Uri? = null,
    val internedBitmaps: LinkedList<Bitmap> = LinkedList(),
    val currentBitmap: Bitmap? = null,
    val cropState: CropState = CropState(),
)

sealed class EditUiEffect {
    sealed class Crop {
        object Succeeded : EditUiEffect()
        object Failed : EditUiEffect()
        object Reset : EditUiEffect()
    }
}

interface EditMiddleware : ReduxViewModel.MiddleWare<EditAction, EditViewState>
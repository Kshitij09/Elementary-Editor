package com.kshitijpatil.elementaryeditor.ui.edit.contract

import android.graphics.Rect
import android.net.Uri


sealed interface EditAction
object Confirm : EditAction
object Cancel : EditAction
data class SetCurrentImageUri(val imageUri: Uri) : EditAction
data class SetActiveEditOperation(val operation: EditOperation) : EditAction

sealed class CropAction : EditAction {
    data class SetCropBounds(val cropBounds: Rect?) : CropAction()
    data class SetImageBounds(val imageBounds: Rect) : CropAction()
}

sealed class InternalAction : EditAction {
    object PerformCrop : InternalAction()
    object Cropping : InternalAction()
    data class CropSucceeded(val imageUri: Uri) : InternalAction()
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
    val cropState: CropState = CropState(),
)

sealed class EditUiEffect {
    sealed class Crop {
        object Succeeded : EditUiEffect()
        object Failed : EditUiEffect()
        object Reset : EditUiEffect()
    }
}
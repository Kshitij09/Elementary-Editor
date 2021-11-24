package com.kshitijpatil.elementaryeditor.ui.edit.contract

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.kshitijpatil.elementaryeditor.data.EditOperation
import com.kshitijpatil.elementaryeditor.ui.common.ReduxViewModel


sealed interface EditAction
data class Confirm(val context: Context) : EditAction
object Cancel : EditAction
object Undo : EditAction
object Redo : EditAction
object PeekFirst : EditAction
object LoadLatest : EditAction
data class SetCurrentImageUri(val imageUri: Uri, val context: Context) : EditAction
data class SetActiveEditOperation(val operation: EditOperation) : EditAction

sealed class CropAction : EditAction {
    data class SetCropBounds(val cropBounds: Rect?) : CropAction()
    data class SetImageBounds(val imageBounds: Rect) : CropAction()
}

sealed class RotateAction : EditAction {
    data class SetRotationAngle(val rotationAngle: Float) : RotateAction()
}

sealed class InternalAction : EditAction {
    /** These action indicate current bitmap is about to get changed
     * Necessary steps such as persisting current bitmap should be
     * performed for every action that's part of it.
     * */
    sealed class MutatingAction : InternalAction() {
        abstract val context: Context

        data class PerformCrop(override val context: Context) : MutatingAction()
        data class Rotate(override val context: Context) : MutatingAction()
    }

    data class PersistBitmap(val bitmap: Bitmap) : InternalAction()
    object Cropping : InternalAction()
    object BitmapLoading : InternalAction()
    object PersistBitmapSkipped : InternalAction()
    data class CropSucceeded(val bitmap: Bitmap) : InternalAction()
    data class RotateSucceeded(val bitmap: Bitmap) : InternalAction()
    data class BitmapLoaded(val bitmap: Bitmap?) : InternalAction()
    data class StepsCountUpdated(val forwardSteps: Int, val backwardSteps: Int) : InternalAction()
    object CropFailed : InternalAction()
    object RotateFailed : InternalAction()
}

data class CropState(
    val cropBounds: Rect? = null,
    val imageBounds: Rect? = null,
    // TODO: Migrate this to EditViewState#bitmapLoading
    @Deprecated("Migrate this to EditViewState::bitmapLoading")
    val inProgress: Boolean = false
) {
    val cropBoundsModified: Boolean
        get() = (cropBounds != imageBounds)
}

@JvmInline
value class RotateState(
    val rotationAngle: Float = 0f
) {
    val modified: Boolean
        get() = rotationAngle != 0f
}

data class EditViewState(
    val activeEditOperation: EditOperation,
    val currentImageUri: Uri? = null,
    val currentBitmap: Bitmap? = null,
    val backwardSteps: Int = 0,
    val forwardSteps: Int = 0,
    val bitmapLoading: Boolean = false,
    val cropState: CropState = CropState(),
    val rotateState: RotateState = RotateState(),
) {
    val imageModified: Boolean
        get() = cropState.cropBoundsModified || rotateState.modified
}

sealed interface EditUiEffect
sealed interface SuccessEffect
sealed interface FailureEffect
sealed interface ResetEffect
sealed class Crop : EditUiEffect {
    object Succeeded : Crop(), SuccessEffect
    object Failed : Crop(), FailureEffect
    object Reset : Crop(), ResetEffect
}

sealed class Rotate : EditUiEffect {
    object Succeeded : Rotate(), SuccessEffect
    object Failed : Rotate(), FailureEffect
    object Reset : Rotate(), ResetEffect
}

interface EditMiddleware : ReduxViewModel.MiddleWare<EditAction, EditViewState>
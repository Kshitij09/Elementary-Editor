package com.kshitijpatil.elementaryeditor.ui.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.kshitijpatil.elementaryeditor.data.EditOperation
import com.kshitijpatil.elementaryeditor.data.EditPayload
import com.kshitijpatil.elementaryeditor.ui.common.LoggingMiddleware
import com.kshitijpatil.elementaryeditor.ui.common.ReduxViewModel
import com.kshitijpatil.elementaryeditor.ui.edit.contract.*
import com.kshitijpatil.elementaryeditor.ui.edit.middleware.*
import com.kshitijpatil.elementaryeditor.util.chronicle.Chronicle
import com.kshitijpatil.elementaryeditor.util.chronicle.createChronicle
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import timber.log.Timber


class EditViewModel(
    private val handle: SavedStateHandle,
    context: Context,
    private val editPayloadListJsonAdapter: JsonAdapter<List<EditPayload>>,
    initialState: EditViewState
) : ReduxViewModel<EditViewState, EditAction>(initialState) {
    // TODO: Fetch this from BuildConfig
    private val maxUndoStackSize: Int = 10
    private val bitmapEditsChronicle: Chronicle<Pair<Bitmap, EditPayload?>> = createChronicle(
        maxSize = maxUndoStackSize,
        threadSafe = true
    )
    private val workManager = WorkManager.getInstance(context)
    override val middlewares = listOf(
        CropBitmapMiddleware(),
        RotateBitmapMiddleware(),
        BitmapChronicleMiddleware(bitmapEditsChronicle),
        ExportMiddleware(bitmapEditsChronicle, editPayloadListJsonAdapter, workManager),
        LoadBitmapFromUriMiddleware(),
        LoggingMiddleware(TAG)
    )
    private val _uiEffect = Channel<EditUiEffect>(capacity = BUFFERED)
    val uiEffect: Flow<EditUiEffect> = _uiEffect
        .receiveAsFlow()
        .shareIn(viewModelScope, WhileSubscribed())

    init {
        wire()
    }

    override fun reduce(action: EditAction, state: EditViewState): EditViewState {
        return when (action) {
            Cancel -> {
                val resetEffect = resetEffectFor(state.activeEditOperation)
                sendEffect(resetEffect)
                resetStateForCurrentOperation(state)
            }
            is Confirm -> confirmedStateForCurrentOperation(action.context, state)
            is CropAction.SetCropBounds -> {
                val cropState = state.cropState.copy(cropBounds = Rect(action.cropBounds))
                state.copy(cropState = cropState)
            }
            is CropAction.SetImageBounds -> {
                val cropState = state.cropState.copy(
                    imageBounds = Rect(action.imageBounds),
                    cropBounds = Rect(action.imageBounds)
                )
                state.copy(cropState = cropState)
            }
            is SetCurrentImageUri -> {
                state.copy(currentImageUri = action.imageUri)
            }
            is RotateAction.SetRotationAngle -> {
                state.copy(rotateState = RotateState(action.rotationAngle))
            }
            InternalAction.CropFailed -> {
                sendEffect(Crop.Failed)
                val cropState = state.cropState.copy(inProgress = false)
                state.copy(cropState = cropState)
            }
            InternalAction.RotateFailed -> {
                sendEffect(Crop.Failed)
                val cropState = state.cropState.copy(inProgress = false)
                state.copy(cropState = cropState)
            }
            is InternalAction.CropSucceeded -> {
                sendEffect(Crop.Succeeded)
                state.copy(
                    cropState = CropState(),
                    currentBitmap = action.bitmap
                )
            }
            is InternalAction.RotateSucceeded -> {
                sendEffect(Rotate.Succeeded)
                state.copy(
                    currentBitmap = action.bitmap,
                    bitmapLoading = false,
                    rotateState = RotateState()
                )
            }
            InternalAction.Cropping -> {
                val cropState = state.cropState.copy(inProgress = true)
                state.copy(cropState = cropState)
            }
            is SetActiveEditOperation -> {
                handle[ACTIVE_EDIT_OPERATION_KEY] = action.operation.ordinal
                state.copy(activeEditOperation = action.operation)
            }
            is InternalAction.BitmapLoaded -> {
                val newState = state.copy(bitmapLoading = false)
                if (action.bitmap == null)
                    newState
                else
                    newState.copy(currentBitmap = action.bitmap)
            }
            InternalAction.BitmapLoading -> state.copy(bitmapLoading = true)

            is InternalAction.StepsCountUpdated -> {
                state.copy(backwardSteps = action.backwardSteps, forwardSteps = action.forwardSteps)
            }
            // actions intercepted by the middlewares should explicitly be listed here
            is InternalAction.ExportFailed -> {
                sendEffect(ExportImageFailed)
                state
            }
            is InternalAction.ExportWorkScheduled -> {
                sendEffect(EditImageWorkScheduled(action.editRequestId, action.saveRequestId))
                state
            }
            is InternalAction.PersistBitmap -> state
            is InternalAction.MutatingAction.PerformCrop -> state
            is InternalAction.MutatingAction.Rotate -> state
            Export -> state
            is InternalAction.Exporting -> state
            InternalAction.PersistBitmapSkipped -> state
            Redo -> state
            PeekFirst -> state
            LoadLatest -> state
            Undo -> state
        }
    }

    private fun resetEffectFor(activeEditOperation: EditOperation): EditUiEffect {
        return when (activeEditOperation) {
            EditOperation.CROP -> Crop.Reset
            EditOperation.ROTATE -> Rotate.Reset
        }
    }

    private fun sendEffect(effect: EditUiEffect) {
        viewModelScope.launch {
            if (!_uiEffect.isClosedForSend) {
                Timber.d("Sending ${effect::class.qualifiedName} Effect")
                _uiEffect.send(effect)
            }
        }
    }

    private fun resetStateForCurrentOperation(state: EditViewState): EditViewState {
        return when (state.activeEditOperation) {
            EditOperation.CROP -> {
                val currentCropState = state.cropState
                val cropState = currentCropState.copy(
                    cropBounds = currentCropState.imageBounds,
                )
                state.copy(cropState = cropState)
            }
            EditOperation.ROTATE -> state.copy(rotateState = RotateState())
        }
    }

    private fun confirmedStateForCurrentOperation(
        context: Context,
        state: EditViewState
    ): EditViewState {
        when (state.activeEditOperation) {
            EditOperation.CROP -> {
                submitAction(InternalAction.MutatingAction.PerformCrop(context))
            }
            EditOperation.ROTATE -> {
                submitAction(InternalAction.MutatingAction.Rotate(context))
            }
        }
        return state
    }

    companion object {
        private const val TAG = "EditViewModel"
        private const val ACTIVE_EDIT_OPERATION_KEY =
            "com.kshitijpatil.elementaryeditor.ui.edit.ACTIVE_EDIT_OPERATION_KEY"
    }
}
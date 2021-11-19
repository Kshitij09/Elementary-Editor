package com.kshitijpatil.elementaryeditor.ui.edit

import android.content.Context
import android.graphics.Rect
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.kshitijpatil.elementaryeditor.ui.common.LoggingMiddleware
import com.kshitijpatil.elementaryeditor.ui.common.ReduxViewModel
import com.kshitijpatil.elementaryeditor.ui.edit.contract.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch


class EditViewModel(
    private val handle: SavedStateHandle,
    context: Context,
    initialState: EditViewState
) : ReduxViewModel<EditViewState, EditAction>(initialState) {
    private val workManager = WorkManager.getInstance(context)
    override val middlewares = listOf(
        CropMiddleware(workManager),
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
                sendEffect(EditUiEffect.Crop.Reset)
                resetStateForCurrentOperation(state)
            }
            Confirm -> confirmedStateForCurrentOperation(state)
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
            InternalAction.CropFailed -> {
                sendEffect(EditUiEffect.Crop.Failed)
                val cropState = state.cropState.copy(inProgress = false)
                state.copy(cropState = cropState)
            }
            is InternalAction.CropSucceeded -> {
                submitAction(SetCurrentImageUri(action.imageUri))
                sendEffect(EditUiEffect.Crop.Succeeded)
                state.copy(cropState = CropState())
            }
            InternalAction.Cropping -> {
                val cropState = state.cropState.copy(inProgress = true)
                state.copy(cropState = cropState)
            }
            is SetActiveEditOperation -> {
                handle[ACTIVE_EDIT_OPERATION_KEY] = action.operation.ordinal
                state.copy(activeEditOperation = action.operation)
            }
            else -> state
        }
    }

    private fun sendEffect(effect: EditUiEffect) {
        viewModelScope.launch {
            if (!_uiEffect.isClosedForSend)
                _uiEffect.send(effect)
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
            EditOperation.ROTATE -> state
        }
    }

    private fun confirmedStateForCurrentOperation(state: EditViewState): EditViewState {
        when (state.activeEditOperation) {
            EditOperation.CROP -> {
                submitAction(InternalAction.PerformCrop)
            }
            EditOperation.ROTATE -> TODO()
        }
        return state
    }

    companion object {
        private const val TAG = "EditViewModel"
        private const val ACTIVE_EDIT_OPERATION_KEY =
            "com.kshitijpatil.elementaryeditor.ui.edit.ACTIVE_EDIT_OPERATION_KEY"
    }
}
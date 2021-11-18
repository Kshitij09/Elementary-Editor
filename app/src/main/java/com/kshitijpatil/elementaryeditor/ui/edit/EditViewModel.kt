package com.kshitijpatil.elementaryeditor.ui.edit

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import androidx.work.*
import com.kshitijpatil.elementaryeditor.util.workRequest
import com.kshitijpatil.elementaryeditor.worker.CropImageWorker
import com.kshitijpatil.elementaryeditor.worker.WorkerConstants
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

sealed interface EditAction
object Confirm : EditAction
object Cancel : EditAction
data class SetCurrentImageUri(val imageUri: Uri) : EditAction
data class SetActiveEditOperation(val operation: EditOperation) : EditAction

sealed class CropAction : EditAction {
    data class SetCropBounds(val cropBounds: Rect) : CropAction()
    data class SetImageBounds(val imageBounds: Rect) : CropAction()
}

sealed class InternalAction : EditAction {
    object PerformCrop : InternalAction()
    object Cropping : InternalAction()
    data class CropSucceeded(val imageUri: Uri) : InternalAction()
    object CropFailed : InternalAction()
}

interface MiddleWare<A, S> {
    fun bind(actions: Flow<A>, state: Flow<S>): Flow<A>
}

abstract class ReduxViewModel<S, A>(initialState: S) : ViewModel() {
    private val _state = MutableStateFlow<S>(initialState)
    val state: StateFlow<S>
        get() = _state.asStateFlow()
    private val pendingActions = MutableSharedFlow<A>()
    protected abstract val middlewares: List<MiddleWare<A, S>>

    init {
        viewModelScope.launch { wire() }
    }

    private suspend fun wire() {
        val actionFlows = middlewares.map {
            it.bind(pendingActions, state)
        }.toTypedArray()
        merge(*actionFlows).collect { reduce(it) }
    }

    abstract fun reduce(action: A)

    fun submitAction(action: A) {
        viewModelScope.launch { pendingActions.emit(action) }
    }

    protected fun setState(reducer: S.() -> S) {
        _state.value = reducer(_state.value)
    }
}

data class CropState(
    val cropBounds: Rect? = null,
    val imageBounds: Rect? = null,
    val modifying: Boolean = false
) {
    val cropBoundsModified: Boolean
        get() = (cropBounds == imageBounds)
}

enum class EditOperation {
    CROP, ROTATE
}

data class EditViewState(
    val activeEditOperation: EditOperation,
    val currentImageUri: Uri? = null,
    val cropState: CropState = CropState(),
)

class CropMiddleware(private val workManager: WorkManager) : MiddleWare<EditAction, EditViewState> {
    override fun bind(actions: Flow<EditAction>, state: Flow<EditViewState>): Flow<EditAction> {
        return actions.filter { it is InternalAction.PerformCrop }
            .combine(state, ::Pair)
            .flatMapConcat { (_, state) ->
                channelFlow {
                    val workData = prepareWorkData(state)
                    if (workData == null) {
                        send(InternalAction.CropFailed)
                        return@channelFlow
                    }
                    val request = workRequest<CropImageWorker>(workData)
                    workManager.enqueue(request)
                    launch { observeCropWorkerForCompletion(request.id) }
                }
            }
    }

    private suspend fun ProducerScope<InternalAction>.observeCropWorkerForCompletion(requestId: UUID) {
        workManager.getWorkInfoByIdLiveData(requestId).asFlow()
            .collect { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val newImageUri =
                            workInfo.outputData.getString(WorkerConstants.KEY_IMAGE_URI)
                        newImageUri?.toUri()?.let { send(InternalAction.CropSucceeded(it)) }
                        close(CancellationException("CropImageWorker finished"))
                    }
                    WorkInfo.State.FAILED -> {
                        send(InternalAction.CropFailed)
                        close(CancellationException("CropImageWorker finished"))
                    }
                    WorkInfo.State.RUNNING -> {
                        send(InternalAction.Cropping)
                    }
                    else -> {
                    }
                }
            }
    }

    private fun prepareWorkData(editViewState: EditViewState): Data? {
        val _viewBounds = editViewState.cropState.imageBounds
        if (_viewBounds == null) {
            Timber.e("View Bounds were not set, skipping...")
            return null
        }
        val imageUri = editViewState.currentImageUri
        if (imageUri == null) {
            Timber.e("Target ImageUri was not set, skipping...")
            return null
        }
        val viewWidth = _viewBounds.width()
        val viewHeight = _viewBounds.height()
        return workDataOf(
            WorkerConstants.KEY_IMAGE_URI to imageUri.toString(),
            WorkerConstants.KEY_CROP_BOUNDS to editViewState.cropState.cropBounds,
            WorkerConstants.KEY_VIEW_WIDTH to viewWidth,
            WorkerConstants.KEY_VIEW_HEIGHT to viewHeight
        )
    }
}

sealed class EditUiEffect {
    object CropSucceeded : EditUiEffect()
    object CropFailed : EditUiEffect()
}

class EditViewModel(
    private val handle: SavedStateHandle,
    context: Context,
    initialState: EditViewState
) : ReduxViewModel<EditViewState, EditAction>(initialState) {
    private val workManager = WorkManager.getInstance(context)
    override val middlewares: List<MiddleWare<EditAction, EditViewState>>
        get() = listOf(CropMiddleware(workManager))
    private val _uiEffect = Channel<EditUiEffect>(capacity = BUFFERED)
    val uiEffect: Flow<EditUiEffect>
        get() = _uiEffect.receiveAsFlow().shareIn(viewModelScope, WhileSubscribed())

    override fun reduce(action: EditAction) {
        when (action) {
            Cancel -> cancelCurrentChanges()
            Confirm -> confirmCurrentOperation()
            is CropAction.SetCropBounds -> {
                val cropState = state.value.cropState.copy(cropBounds = action.cropBounds)
                setState { copy(cropState = cropState) }
            }
            is CropAction.SetImageBounds -> {
                val cropState = state.value.cropState.copy(imageBounds = action.imageBounds)
                setState { copy(cropState = cropState) }
            }
            is SetCurrentImageUri -> {
                setState { copy(currentImageUri = action.imageUri) }
            }
            InternalAction.CropFailed -> {
                sendEffect(EditUiEffect.CropFailed)
                val cropState = state.value.cropState.copy(modifying = false)
                setState { copy(cropState = cropState) }
            }
            is InternalAction.CropSucceeded -> {
                sendEffect(EditUiEffect.CropSucceeded)
                val cropState = state.value.cropState.copy(modifying = false)
                setState { copy(cropState = cropState) }
            }
            InternalAction.Cropping -> {
                val cropState = state.value.cropState.copy(modifying = true)
                setState { copy(cropState = cropState) }
            }
            is SetActiveEditOperation -> {
                handle["active-edit-operation"] = action.operation.ordinal
                setState { copy(activeEditOperation = action.operation) }
            }
            else -> {
            }
        }
    }

    private fun sendEffect(effect: EditUiEffect) {
        viewModelScope.launch {
            if (!_uiEffect.isClosedForSend)
                _uiEffect.send(effect)
        }
    }

    private fun cancelCurrentChanges() {
        when (state.value.activeEditOperation) {
            EditOperation.CROP -> {
                val currentCropState = state.value.cropState
                val cropState = currentCropState.copy(
                    cropBounds = currentCropState.imageBounds,
                    modifying = false
                )
                setState { copy(cropState = cropState) }
            }
            EditOperation.ROTATE -> TODO()
        }
    }

    private fun confirmCurrentOperation() {
        when (state.value.activeEditOperation) {
            EditOperation.CROP -> {
                submitAction(InternalAction.PerformCrop)
            }
            EditOperation.ROTATE -> TODO()
        }
    }

    /**
     * Enqueues the [CropImageWorker] to perform Crop Operation on [targetImageUri]
     * with the given [cropBounds]
     * @return Unique requestId if work enqueued properly, null otherwise
     *//*
    fun cropImage(cropBounds: IntArray): UUID? {
        val workData = prepareWorkData(cropBounds) ?: return null
        val request = workRequest<CropImageWorker>(workData)
        workManager.enqueue(request)
        launchWorkInfoObserver(request.id)
        return request.id
    }*/

    /*private fun launchWorkInfoObserver(requestId: UUID) {
        workManager.getWorkInfoByIdLiveData(requestId).asFlow()
            .onEach { workInfo ->
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    val newImageUri = workInfo.outputData.getString(WorkerConstants.KEY_IMAGE_URI)
                    _targetImageUri.value = newImageUri?.toUri()
                }
            }.launchIn(viewModelScope)
    }

    private fun prepareWorkData(cropBounds: IntArray): Data? {
        val _viewBounds = viewBounds
        if (_viewBounds == null) {
            Timber.e("View Bounds were not set, skipping...")
            return null
        }
        val imageUri = targetImageUri.value
        if (imageUri == null) {
            Timber.e("Target ImageUri was not set, skipping...")
            return null
        }
        val viewWidth = _viewBounds.width()
        val viewHeight = _viewBounds.height()
        return workDataOf(
            WorkerConstants.KEY_IMAGE_URI to imageUri.toString(),
            WorkerConstants.KEY_CROP_BOUNDS to cropBounds,
            WorkerConstants.KEY_VIEW_WIDTH to viewWidth,
            WorkerConstants.KEY_VIEW_HEIGHT to viewHeight
        )
    }*/
}

class EditViewModelFactory(
    owner: SavedStateRegistryOwner,
    context: Context,
    defaultArgs: Bundle?
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    private val contextRef = WeakReference(context)
    private val editOperationValues = enumValues<EditOperation>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle
    ): T {
        val context = contextRef.get()
            ?: throw IllegalStateException("No Context available to initialize the ViewModel")
        if (modelClass.isAssignableFrom(EditViewModel::class.java)) {
            val editOperationIndex = handle.get<Int>("active-edit-operation") ?: 0
            val activeEditOperation = editOperationValues[editOperationIndex]
            val initialState = EditViewState(activeEditOperation)
            return EditViewModel(handle, context, initialState) as T
        }
        throw IllegalArgumentException("ViewModel not found")
    }
}
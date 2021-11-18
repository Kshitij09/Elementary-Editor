package com.kshitijpatil.elementaryeditor.ui.edit

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.kshitijpatil.elementaryeditor.util.workRequest
import com.kshitijpatil.elementaryeditor.worker.CropImageWorker
import com.kshitijpatil.elementaryeditor.worker.WorkerConstants
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

class EditViewModel(context: Context) : ViewModel() {
    private val workManager = WorkManager.getInstance(context)
    private val _targetImageUri = MutableStateFlow<Uri?>(null)
    val targetImageUri: StateFlow<Uri?> get() = _targetImageUri.asStateFlow()
    private val _cropBoundsModified = MutableStateFlow(false)
    val cropBoundsModified: StateFlow<Boolean> get() = _cropBoundsModified.asStateFlow()
    private var viewBounds: Rect? = null
    private val lock = Object()

    fun setTargetImageUri(imageUri: Uri) {
        _targetImageUri.value = imageUri
    }

    fun setViewBounds(viewBounds: Rect) {
        synchronized(lock) {
            this.viewBounds = viewBounds
        }
    }

    fun setCropBoundsModified(modified: Boolean) {
        _cropBoundsModified.value = modified
    }

    /**
     * Enqueues the [CropImageWorker] to perform Crop Operation on [targetImageUri]
     * with the given [cropBounds]
     * @return Unique requestId if work enqueued properly, null otherwise
     */
    fun cropImage(cropBounds: IntArray): UUID? {
        val workData = prepareWorkData(cropBounds) ?: return null
        val request = workRequest<CropImageWorker>(workData)
        workManager.enqueue(request)
        launchWorkInfoObserver(request.id)
        return request.id
    }

    private fun launchWorkInfoObserver(requestId: UUID) {
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
    }
}

class EditViewModelFactory(context: Context) : ViewModelProvider.Factory {
    private val contextRef = WeakReference(context)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        val context = contextRef.get()
            ?: throw IllegalStateException("No Context available to initialize the ViewModel")
        if (modelClass.isAssignableFrom(EditViewModel::class.java)) {
            return EditViewModel(context) as T
        }
        throw IllegalArgumentException("ViewModel not found")
    }
}
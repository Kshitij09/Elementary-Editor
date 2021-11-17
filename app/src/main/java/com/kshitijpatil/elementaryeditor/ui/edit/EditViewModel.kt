package com.kshitijpatil.elementaryeditor.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EditViewModel : ViewModel() {
    private val _targetImageUri = MutableStateFlow<Uri?>(null)
    val targetImageUri: StateFlow<Uri?> get() = _targetImageUri.asStateFlow()

    fun setTargetImageUri(imageUri: Uri) {
        _targetImageUri.value = imageUri
    }
}
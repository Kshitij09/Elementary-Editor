package com.kshitijpatil.elementaryeditor.ui.edit

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.kshitijpatil.elementaryeditor.data.EditOperation
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditViewState
import java.lang.ref.WeakReference

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
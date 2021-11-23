package com.kshitijpatil.elementaryeditor.ui.edit

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import com.kshitijpatil.elementaryeditor.R
import com.kshitijpatil.elementaryeditor.databinding.ActivityEditBinding
import com.kshitijpatil.elementaryeditor.ui.edit.contract.*
import com.kshitijpatil.elementaryeditor.ui.home.MainActivity
import com.kshitijpatil.elementaryeditor.util.launchAndRepeatWithViewLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    private lateinit var navController: NavController
    private val editViewModel: EditViewModel by viewModels {
        EditViewModelFactory(this, applicationContext, null)
    }

    // TODO: Persist this in bundle to survive configuration changes
    private lateinit var lastSelectedEditOperation: EditOperation
    private var pendingEditOpSelected: EditOperation? = null

    private val saveChangesActionListener =
        SaveChangesAlertBottomSheet.OnActionSelectedListener { action ->
            when (action) {
                ChangeAction.SAVE -> {
                    editViewModel.submitAction(Confirm(applicationContext))
                }
                ChangeAction.DISCARD -> {
                    editViewModel.submitAction(Cancel)
                }
                ChangeAction.CANCEL -> {
                    binding.cgEditOptions.check(lastSelectedEditOperation.toChipId())
                    pendingEditOpSelected = null
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntentUri()
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.edit_action_fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        setupToolbar()
        restoreSelectedEditOperation()
        lastSelectedEditOperation = binding.cgEditOptions.checkedChipId.toEditOperation()
        setupUiCallbacks()
        launchAndRepeatWithViewLifecycle {
            launch { observeForActionVisibility() }
            launch { observeForUndoRedoEnabled() }
            launch { observeUiEffects() }
        }
    }

    private suspend fun observeForUndoRedoEnabled() {
        editViewModel.state
            .map { Pair(it.forwardSteps, it.backwardSteps) }
            .stateIn(lifecycleScope)
            .collect { (forwardSteps, backwardSteps) ->
                binding.toolbar.menu.findItem(R.id.menu_item_undo)?.isEnabled = backwardSteps != 0
                binding.toolbar.menu.findItem(R.id.menu_item_redo)?.isEnabled = forwardSteps != 0
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val currentState = editViewModel.state.value
        menuInflater.inflate(R.menu.edit_menu, menu)
        menu.findItem(R.id.menu_item_undo)?.isEnabled = currentState.backwardSteps != 0
        menu.findItem(R.id.menu_item_redo)?.isEnabled = currentState.forwardSteps != 0
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_item_undo -> {
            editViewModel.submitAction(Undo)
            true
        }
        R.id.menu_item_redo -> {
            editViewModel.submitAction(Redo)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupUiCallbacks() {
        binding.cgEditOptions.setOnCheckedChangeListener { group, checkedId ->
            if (pendingEditOpSelected != null) return@setOnCheckedChangeListener
            val imageModified = editViewModel.state.value.imageModified
            val currentSelected = checkedId.toEditOperation()
            if (imageModified) {
                promptSaveChanges(currentSelected)
            } else onEditOperationSelected(currentSelected)
        }
        binding.ivConfirm.setOnClickListener {
            editViewModel.submitAction(Confirm(applicationContext))
        }
        binding.ivCancel.setOnClickListener { editViewModel.submitAction(Cancel) }
    }

    private fun restoreSelectedEditOperation() {
        val lastSelectedEditOperation = editViewModel.state.value.activeEditOperation
        onEditOperationSelected(lastSelectedEditOperation)
    }

    private suspend fun observeUiEffects() {
        editViewModel.uiEffect
            .collect {
                when (it) {
                    is SuccessEffect, is ResetEffect -> performPendingEditOperationSelections()
                    is FailureEffect -> showSnackbar(R.string.error_operation_failed)
                }
            }
    }

    private fun performPendingEditOperationSelections() {
        pendingEditOpSelected?.let {
            lastSelectedEditOperation = it
            editViewModel.submitAction(SetActiveEditOperation(it))
            onEditOperationSelected(it)
            pendingEditOpSelected = null
        }
    }

    private fun handleIntentUri() {
        val imageUri = intent.getStringExtra(MainActivity.IMAGE_URI_KEY_EXTRA)
        Timber.d("Editing image with the uri: $imageUri")
        if (imageUri == null) {
            warnAndExit()
        } else {
            editViewModel.submitAction(SetCurrentImageUri(imageUri.toUri(), applicationContext))
        }
    }

    private suspend fun observeForActionVisibility() {
        editViewModel.state
            .collect { state ->
                val activeOpModified = when (state.activeEditOperation) {
                    EditOperation.CROP -> state.cropState.cropBoundsModified
                    EditOperation.ROTATE -> state.rotateState.modified
                }
                binding.ivCancel.isVisible = activeOpModified && !state.bitmapLoading
                binding.ivConfirm.isVisible = activeOpModified && !state.bitmapLoading
            }
    }

    private fun warnAndExit() {
        Timber.e("IllegalState: imageUri was null")
        val appName = getString(R.string.app_name)
        Toast.makeText(
            this,
            "Please share an Image accessible to $appName",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    private fun promptSaveChanges(currentSelected: EditOperation) {
        pendingEditOpSelected = currentSelected
        val bottomSheet = SaveChangesAlertBottomSheet(saveChangesActionListener)
        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).also {
            it.anchorView = binding.scrollviewEditOptions
        }.show()
    }

    private fun showSnackbar(@StringRes messageResId: Int) {
        val message = getString(messageResId)
        showSnackbar(message)
    }

    private fun onEditOperationSelected(editOperation: EditOperation) {
        val navDestination = editOperationToFragmentId(editOperation)
        if (navController.currentDestination?.id == navDestination) {
            // already at the destination
            return
        }
        val builder = NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(true)
        builder.setPopUpTo(
            navController.graph.findStartDestination().id,
            inclusive = false,
            saveState = true
        )
        val options = builder.build()
        navController.navigate(navDestination, null, options)
    }

    private fun editOperationToFragmentId(editOperation: EditOperation): Int {
        return when (editOperation) {
            EditOperation.CROP -> R.id.fragment_crop
            EditOperation.ROTATE -> R.id.fragment_rotate
        }
    }
}

private fun Int.toEditOperation(): EditOperation {
    return when (this) {
        R.id.chip_crop -> EditOperation.CROP
        R.id.chip_rotate -> EditOperation.ROTATE
        else -> EditOperation.CROP
    }
}

@IdRes
private fun EditOperation.toChipId(): Int {
    return when (this) {
        EditOperation.CROP -> R.id.chip_crop
        EditOperation.ROTATE -> R.id.chip_rotate
    }
}
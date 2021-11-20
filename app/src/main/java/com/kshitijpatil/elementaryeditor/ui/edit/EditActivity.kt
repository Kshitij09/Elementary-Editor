package com.kshitijpatil.elementaryeditor.ui.edit

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.IdRes
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntentUri()
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.edit_action_fragment_container) as NavHostFragment
        navController = navHostFragment.navController
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        restoreSelectedEditOperation()
        setupUiCallbacks()
        launchAndRepeatWithViewLifecycle {
            launch { observeForActionVisibility() }
            launch { observeUiEffects() }
        }
    }

    private suspend fun observeUiEffects() {
        editViewModel.uiEffect.collect { effect ->
            when (effect) {
                EditUiEffect.Crop.Failed -> {
                    showSnackbar("Crop Failed")
                }
                EditUiEffect.Crop.Succeeded -> {
                    showSnackbar("Crop Successful!")
                }
                else -> {
                }
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).also {
            it.anchorView = binding.scrollviewEditOptions
        }.show()
    }

    private fun setupUiCallbacks() {
        binding.cgEditOptions.setOnCheckedChangeListener { _, checkedId ->
            chipIdToEditOperation(checkedId)?.let {
                editViewModel.submitAction(SetActiveEditOperation(it))
                onEditOperationSelected(it)
            }
        }
        binding.ivConfirm.setOnClickListener {
            Timber.d("confirm clicked")
            editViewModel.submitAction(Confirm(applicationContext))
        }
        binding.ivCancel.setOnClickListener { editViewModel.submitAction(Cancel) }
    }

    private fun restoreSelectedEditOperation() {
        val lastSelectedEditOperation = editViewModel.state.value.activeEditOperation
        onEditOperationSelected(lastSelectedEditOperation)
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
            .map { Pair(it.cropState.cropBoundsModified, it.cropState.inProgress) }
            .stateIn(lifecycleScope)
            .collect { (cropBoundsModified, cropInProgress) ->
                binding.ivCancel.isVisible = cropBoundsModified && !cropInProgress
                binding.ivConfirm.isVisible = cropBoundsModified && !cropInProgress
            }
    }

    private fun warnAndExit() {
        Timber.e("IllegalState: imageUri was null")
        Toast.makeText(
            this,
            "Please share an Image accessible to Elementary Editor",
            Toast.LENGTH_SHORT
        ).show()
        finish()
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

    private fun chipIdToEditOperation(@IdRes chipId: Int): EditOperation? {
        return when (chipId) {
            R.id.chip_crop -> EditOperation.CROP
            R.id.chip_rotate -> EditOperation.ROTATE
            else -> null
        }
    }
}
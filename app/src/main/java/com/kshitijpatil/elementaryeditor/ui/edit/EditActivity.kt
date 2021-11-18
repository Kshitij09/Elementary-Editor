package com.kshitijpatil.elementaryeditor.ui.edit

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.kshitijpatil.elementaryeditor.R
import com.kshitijpatil.elementaryeditor.databinding.ActivityEditBinding
import com.kshitijpatil.elementaryeditor.ui.home.MainActivity
import timber.log.Timber

class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    private val navController by lazy {
        findNavController(R.id.edit_action_fragment_container)
    }
    private val editViewModel: EditViewModel by viewModels {
        EditViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageUri = intent.getStringExtra(MainActivity.IMAGE_URI_KEY_EXTRA)
        Timber.d("Received image-uri: $imageUri")
        if (imageUri == null) warnAndExit()
        else editViewModel.setTargetImageUri(imageUri.toUri())
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        binding.cgEditOptions.setOnCheckedChangeListener { _, checkedId ->
            onNavDestinationSelected(checkedId)
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

    private fun onNavDestinationSelected(@IdRes checkedChipId: Int) {
        val builder = NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(true)
        builder.setPopUpTo(
            navController.graph.findStartDestination().id,
            inclusive = false,
            saveState = true
        )
        val options = builder.build()
        chipIdToFragmentId(checkedChipId)?.let {
            navController.navigate(it, null, options)
        }
    }

    private fun chipIdToFragmentId(@IdRes chipId: Int): Int? {
        return when (chipId) {
            R.id.chip_crop -> R.id.fragment_crop
            R.id.chip_rotate -> R.id.fragment_rotate
            else -> null
        }
    }
}
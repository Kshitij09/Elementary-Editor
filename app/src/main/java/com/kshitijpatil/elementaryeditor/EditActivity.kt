package com.kshitijpatil.elementaryeditor

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.kshitijpatil.elementaryeditor.databinding.ActivityEditBinding
import com.kshitijpatil.elementaryeditor.ui.home.MainActivity
import timber.log.Timber

class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    private val navController by lazy {
        findNavController(R.id.edit_action_fragment_container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageUri = intent.getStringExtra(MainActivity.IMAGE_URI_KEY_EXTRA)
        Timber.d("Performing edit on $imageUri")
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.cgEditOptions.setOnCheckedChangeListener { _, checkedId ->
            onNavDestinationSelected(checkedId)
        }
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
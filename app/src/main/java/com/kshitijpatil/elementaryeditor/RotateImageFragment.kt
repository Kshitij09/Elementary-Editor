package com.kshitijpatil.elementaryeditor

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.kshitijpatil.elementaryeditor.databinding.FragmentRotateImageBinding
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModel
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModelFactory
import com.kshitijpatil.elementaryeditor.ui.edit.contract.Rotate
import com.kshitijpatil.elementaryeditor.ui.edit.contract.RotateAction
import com.kshitijpatil.elementaryeditor.util.glide.loadThumbnail
import com.kshitijpatil.elementaryeditor.util.launchAndRepeatWithViewLifecycle
import com.kshitijpatil.elementaryeditor.util.viewLifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.absoluteValue

class ImageSaver(
    private val registry: ActivityResultRegistry,
    private val resolver: ContentResolver
) : DefaultLifecycleObserver {
    companion object {
        const val REQUEST_STORAGE_PERMISSION_KEY =
            "com.kshitijpatil.elementaryeditor.REQUEST_STORAGE_PERMISSION_KEY"
    }

    lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var targetBitmap: Bitmap? = null

    override fun onCreate(owner: LifecycleOwner) {
        requestPermissionLauncher =
            registry.register(REQUEST_STORAGE_PERMISSION_KEY, owner, RequestPermission()) {
                saveToImageCollection()
            }
    }

    private fun saveToImageCollection() {
        val data = targetBitmap
        if (data != null) {
            val photosCollection = getPhotosCollection()
            val imageDetails = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "test-rotation-image.png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            resolver.insert(photosCollection, imageDetails)?.let { imageUri ->
                resolver.openOutputStream(imageUri, "w").use {
                    data.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    imageDetails.clear()
                    imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, imageDetails, null, null)
                    targetBitmap = null
                }
            }
        } else {
            Timber.e("Incorrect save-bitmap call, nothing to save")
        }
    }

    fun saveImage(bitmap: Bitmap) {
        targetBitmap = bitmap
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveToImageCollection()
        }
    }

    private fun getPhotosCollection(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // don't leak any references
        targetBitmap = null
    }
}

class RotateImageFragment : Fragment() {
    private var _binding: FragmentRotateImageBinding? = null

    // only valid through onCreateView to onDestroyView
    private val binding: FragmentRotateImageBinding get() = _binding!!
    private val editViewModel: EditViewModel by activityViewModels {
        EditViewModelFactory(requireActivity(), requireContext(), arguments)
    }
    private lateinit var imageSaver: ImageSaver

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRotateImageBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageSaver = ImageSaver(
            registry = requireActivity().activityResultRegistry,
            resolver = requireContext().contentResolver
        )
        lifecycle.addObserver(imageSaver)
    }

    private var currentRotation = 0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivRotate.setOnClickListener {
            currentRotation = binding.imgPreview.animatedRotate(currentRotation, false)
            editViewModel.submitAction(RotateAction.SetRotationAngle(currentRotation))
        }
        launchAndRepeatWithViewLifecycle {
            launch { observeCurrentBitmap() }
            launch { observeRotateUiEffect() }
        }
    }

    private suspend fun observeCurrentBitmap() {
        editViewModel.state
            .map { Pair(it.currentBitmap, it.bitmapLoading) }
            .stateIn(viewLifecycleScope)
            .collect { (bitmap, loading) ->
                val showPreview = bitmap != null && !loading
                if (showPreview) {
                    binding.imgPreview.loadThumbnail(bitmap!!)
                }
                binding.imgPreview.isVisible = showPreview
                binding.progressRotate.isVisible = !showPreview
            }
    }

    private suspend fun observeRotateUiEffect() {
        editViewModel.uiEffect
            .filter { it is Rotate }
            .collect {
                Timber.d("Resetting...")
                resetCurrentPreview()
            }
    }

    private fun resetCurrentPreview() {
        //binding.imgPreview.animateResetRotation(currentRotation)
        binding.imgPreview.clearAnimation()
        currentRotation = 0f
        //Timber.d("Current rotation=$currentRotation")
    }

    /**
     *
     * 0 -- 90 -- 180 -- 270 -- 360 -- 0
     */
    private fun ImageView.animatedRotate(currentRotation: Float, clockwise: Boolean = true): Float {
        val fromRotation = if (currentRotation.absoluteValue == 360f) 0f else currentRotation
        val rotateDegrees = if (clockwise) 90f else -90f
        val toRotation = (fromRotation + rotateDegrees) % 450f
        Timber.d("Rotating from $fromRotation to $toRotation")
        val rotateAnimation = getRotateAnimation(fromRotation, toRotation)
        startAnimation(rotateAnimation)
        return toRotation
    }

    private fun ImageView.animateResetRotation(currentRotation: Float) {
        val fromRotation = if (currentRotation.absoluteValue == 360f) 0f else currentRotation
        val toRotation = 0f
        val rotateAnimation = getRotateAnimation(fromRotation, toRotation)
        startAnimation(rotateAnimation)
    }

    private fun ImageView.getRotateAnimation(
        from: Float,
        to: Float,
        animateDuration: Long = 400
    ): RotateAnimation {
        return RotateAnimation(
            from,
            to,
            width / 2f,
            height / 2f
        ).apply {
            duration = animateDuration
            fillAfter = true
        }
    }
}
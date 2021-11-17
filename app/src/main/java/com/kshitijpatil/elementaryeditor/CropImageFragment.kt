package com.kshitijpatil.elementaryeditor

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.kshitijpatil.elementaryeditor.databinding.FragmentCropImageBinding
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModel
import com.kshitijpatil.elementaryeditor.util.getBitmapPositionInsideImageView
import com.kshitijpatil.elementaryeditor.util.launchAndRepeatWithViewLifecycle
import com.kshitijpatil.elementaryeditor.util.viewLifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber


class CropImageFragment : Fragment(R.layout.fragment_crop_image) {
    private lateinit var imageSaver: ImageSaver
    private val editViewModel: EditViewModel by activityViewModels()
    private var _binding: FragmentCropImageBinding? = null
    private val binding: FragmentCropImageBinding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageSaver = ImageSaver(
            registry = requireActivity().activityResultRegistry,
            resolver = requireContext().contentResolver
        )
        lifecycle.addObserver(imageSaver)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCropImageBinding.inflate(inflater, container, false)
        binding.imgPreview.viewTreeObserver.addOnGlobalLayoutListener {
            getBitmapPositionInsideImageView(binding.imgPreview)?.let {
                binding.cropOverlay.setImageBounds(it)
            }
        }
        binding.btnSave.setOnClickListener {
            editViewModel.targetImageUri.value?.let { imageUri ->
                val bitmapTarget = Glide.with(requireContext())
                    .asBitmap()
                    .load(imageUri)
                    .submit()
                binding.btnSave.isEnabled = false
                viewLifecycleScope.launch(Dispatchers.Default) {
                    try {
                        binding.cropOverlay.getCropBbox()?.let { cropBounds ->
                            var processed = bitmapTarget.get()
                            val viewWidth = binding.cropOverlay.initialBounds!!.width()
                            val viewHeight = binding.cropOverlay.initialBounds!!.height()
                            val scaleX = processed.width / viewWidth.toFloat()
                            val scaleY = processed.height / viewHeight.toFloat()
                            val topX = cropBounds.startX * scaleX
                            val topY = cropBounds.startY * scaleY
                            val width = cropBounds.width * scaleX
                            val height = cropBounds.height * scaleY
                            processed = Bitmap.createBitmap(
                                processed,
                                topX.toInt(),
                                topY.toInt(),
                                width.toInt(),
                                height.toInt()
                            )
                            imageSaver.saveImage(processed)
                        }
                    } catch (throwable: Throwable) {
                        Timber.e(throwable, "Failed loading bitmap")
                    }
                }.invokeOnCompletion {
                    if (it != null) {
                        Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
                    }
                    viewLifecycleScope.launch(Dispatchers.Main) {
                        binding.btnSave.isEnabled = true
                    }
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        launchAndRepeatWithViewLifecycle {
            launch { observeTargetImageUri() }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private suspend fun observeTargetImageUri() {
        editViewModel.targetImageUri.collect { uri ->
            if (uri != null) {
                Glide.with(requireContext())
                    .load(uri)
                    .thumbnail(0.1f)
                    .into(binding.imgPreview)
            }
            binding.imgPreview.isVisible = uri != null
            binding.cropOverlay.isVisible = uri != null
        }
    }
}
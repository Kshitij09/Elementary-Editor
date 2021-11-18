package com.kshitijpatil.elementaryeditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.kshitijpatil.elementaryeditor.databinding.FragmentCropImageBinding
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModel
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModelFactory
import com.kshitijpatil.elementaryeditor.util.getBitmapPositionInsideImageView
import com.kshitijpatil.elementaryeditor.util.launchAndRepeatWithViewLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*


class CropImageFragment : Fragment(R.layout.fragment_crop_image) {
    private lateinit var imageSaver: ImageSaver
    private val editViewModel: EditViewModel by activityViewModels {
        EditViewModelFactory(requireContext())
    }
    private var _binding: FragmentCropImageBinding? = null
    private val binding: FragmentCropImageBinding get() = _binding!!
    private val workManager: WorkManager by lazy {
        WorkManager.getInstance(requireContext())
    }

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
                editViewModel.setViewBounds(it)
            }
        }
        binding.btnSave.setOnClickListener {
            editViewModel.targetImageUri.value?.let { imageUri ->
                binding.btnSave.isEnabled = false
                binding.cropOverlay.getCropBounds().let { cropBounds ->
                    if (cropBounds == null) {
                        Timber.e("Crop bounds are null, skipping...")
                        return@setOnClickListener
                    }
                    editViewModel.cropImage(cropBounds)?.let {
                        observeWorkForCompletion(it)
                    }
                }
                /*if (it != null) {
                    Toast.makeText(requireContext(), "Saved!", Toast.LENGTH_SHORT).show()
                }
                viewLifecycleScope.launch(Dispatchers.Main) {
                    binding.btnSave.isEnabled = true
                }*/
            }
        }
        return binding.root
    }

    private fun observeWorkForCompletion(workRequestId: UUID) {
        val tag = "CropImageWork"
        workManager
            .getWorkInfoByIdLiveData(workRequestId)
            .observe(viewLifecycleOwner) { workInfo ->
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> {
                        Timber.v("$tag: Enqueued")
                    }
                    WorkInfo.State.RUNNING -> {
                        Timber.v("$tag: Running")
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        Timber.v("$tag: Succeeded with data: ${workInfo.outputData}")
                        Toast.makeText(requireContext(), "Finished Cropping", Toast.LENGTH_SHORT)
                            .show()
                        binding.btnSave.isEnabled = true
                    }
                    WorkInfo.State.FAILED -> {
                        Timber.v("$tag: Failed")
                    }
                    WorkInfo.State.BLOCKED -> {
                        Timber.v("$tag: Blocked")
                    }
                    WorkInfo.State.CANCELLED -> {
                        Timber.v("$tag: Cancelled")
                    }
                }
            }
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
package com.kshitijpatil.elementaryeditor

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.kshitijpatil.elementaryeditor.databinding.FragmentCropImageBinding
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModel
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModelFactory
import com.kshitijpatil.elementaryeditor.ui.edit.contract.CropAction
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditUiEffect
import com.kshitijpatil.elementaryeditor.util.getBitmapPositionInsideImageView
import com.kshitijpatil.elementaryeditor.util.launchAndRepeatWithViewLifecycle
import com.kshitijpatil.elementaryeditor.util.viewLifecycleScope
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class CropImageFragment : Fragment(R.layout.fragment_crop_image) {
    private lateinit var imageSaver: ImageSaver
    private val editViewModel: EditViewModel by activityViewModels {
        EditViewModelFactory(requireActivity(), requireContext(), arguments)
    }
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

    private val previewImageChangedListener =
        View.OnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            if (view.id == R.id.img_preview) {
                getBitmapPositionInsideImageView(binding.imgPreview)?.let {
                    binding.cropOverlay.setImageBounds(it)
                    editViewModel.submitAction(CropAction.SetImageBounds(it))
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCropImageBinding.inflate(inflater, container, false)
        binding.imgPreview.addOnLayoutChangeListener(previewImageChangedListener)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        launchAndRepeatWithViewLifecycle {
            launch { observeCurrentImageUri() }
            launch { observeCropBoundsModified() }
            launch { observeCropResetEffect() }
        }
    }

    private suspend fun observeCropResetEffect() {
        editViewModel.uiEffect
            .filter { it is EditUiEffect.Crop.Reset }
            .collect { binding.cropOverlay.reset() }
    }

    private suspend fun observeCropBoundsModified() {
        binding.cropOverlay.cropBoundsChangedFlow()
            .debounce(300)
            .collect {
                editViewModel.submitAction(CropAction.SetCropBounds(it))
            }
    }

    override fun onDestroyView() {
        binding.imgPreview.removeOnLayoutChangeListener(previewImageChangedListener)
        _binding = null
        super.onDestroyView()
    }

    private suspend fun observeCurrentImageUri() {
        editViewModel.state
            .map { Pair(it.currentImageUri, it.cropState.inProgress) }
            .stateIn(viewLifecycleScope)
            .collect { (uri, cropInProgress) ->
                if (uri != null) {
                    var requestBuilder = Glide.with(requireContext()).load(uri)
                    if (cropInProgress) {
                        requestBuilder = requestBuilder.apply(
                            bitmapTransform(BlurTransformation(25))
                        )
                    }
                    requestBuilder.thumbnail(0.1f).into(binding.imgPreview)
                }
                binding.imgPreview.isVisible = uri != null
                binding.cropOverlay.isVisible = uri != null && !cropInProgress
                binding.progressCrop.isVisible = uri == null || cropInProgress
            }
    }

    private fun CropOverlay.cropBoundsChangedFlow(): Flow<Rect?> {
        return callbackFlow {
            val callback = CropOverlay.OnCropBoundsChangedListener {
                trySend(it)
            }
            onCropBoundsChangedListener = callback
            awaitClose { onCropBoundsChangedListener = null }
        }
    }
}
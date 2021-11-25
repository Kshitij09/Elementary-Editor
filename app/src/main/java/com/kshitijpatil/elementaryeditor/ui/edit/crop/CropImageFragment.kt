package com.kshitijpatil.elementaryeditor.ui.edit.crop

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.kshitijpatil.elementaryeditor.R
import com.kshitijpatil.elementaryeditor.databinding.FragmentCropImageBinding
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModel
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModelFactory
import com.kshitijpatil.elementaryeditor.ui.edit.contract.Crop
import com.kshitijpatil.elementaryeditor.ui.edit.contract.CropAction
import com.kshitijpatil.elementaryeditor.util.getBitmapPositionInsideImageView
import com.kshitijpatil.elementaryeditor.util.glide.loadThumbnail
import com.kshitijpatil.elementaryeditor.util.launchAndRepeatWithViewLifecycle
import com.kshitijpatil.elementaryeditor.util.viewLifecycleScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class CropImageFragment : Fragment(R.layout.fragment_crop_image) {
    private val editViewModel: EditViewModel by activityViewModels {
        EditViewModelFactory(requireActivity(), requireContext(), arguments)
    }
    private var _binding: FragmentCropImageBinding? = null
    private val binding: FragmentCropImageBinding get() = _binding!!

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
            launch { observeCurrentBitmap() }
            launch { observeCropBoundsModified() }
            launch { observeCropResetEffect() }
        }
    }

    private suspend fun observeCropResetEffect() {
        editViewModel.uiEffect
            .filter { it is Crop.Reset }
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

    private suspend fun observeCurrentBitmap() {
        editViewModel.state
            .map { Pair(it.currentBitmap, it.cropState.inProgress) }
            .stateIn(viewLifecycleScope)
            .collect { (bitmap, cropInProgress) ->
                val showPreview = bitmap != null && !cropInProgress
                if (showPreview) {
                    binding.imgPreview.loadThumbnail(bitmap!!)
                }
                binding.imgPreview.isVisible = showPreview
                binding.cropOverlay.isVisible = showPreview
                binding.progressCrop.isVisible = !showPreview
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
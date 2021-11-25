package com.kshitijpatil.elementaryeditor.ui.edit.rotate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import kotlin.math.absoluteValue

class RotateImageFragment : Fragment() {
    private var _binding: FragmentRotateImageBinding? = null

    // TODO: Handle configuration changes
    private var currentRotation = 0f

    // only valid through onCreateView to onDestroyView
    private val binding: FragmentRotateImageBinding get() = _binding!!
    private val editViewModel: EditViewModel by activityViewModels {
        EditViewModelFactory(requireActivity(), requireContext(), arguments)
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivRotate.setOnClickListener {
            currentRotation = binding.imgPreview.animatedRotate(currentRotation, false)
            editViewModel.submitAction(RotateAction.SetRotationAngle(resetIf360(currentRotation)))
        }
        launchAndRepeatWithViewLifecycle {
            launch { observeCurrentBitmap() }
            launch { observeRotateUiEffect() }
        }
    }

    private fun resetIf360(currentRotation: Float): Float {
        return if (currentRotation.absoluteValue == 360f)
            0f
        else currentRotation
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
            .collect { resetCurrentPreview() }
    }

    private fun resetCurrentPreview() {
        binding.imgPreview.clearAnimation()
        currentRotation = 0f
    }

    /**
     *
     * 0 -- 90 -- 180 -- 270 -- 360 -- 0
     */
    private fun ImageView.animatedRotate(currentRotation: Float, clockwise: Boolean = true): Float {
        val fromRotation = resetIf360(currentRotation)
        val rotateDegrees = if (clockwise) 90f else -90f
        val toRotation = (fromRotation + rotateDegrees) % 450f
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
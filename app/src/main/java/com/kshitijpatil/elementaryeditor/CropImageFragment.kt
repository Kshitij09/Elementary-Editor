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
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.Target
import com.kshitijpatil.elementaryeditor.databinding.FragmentCropImageBinding
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModel
import com.kshitijpatil.elementaryeditor.ui.edit.EditViewModelFactory
import com.kshitijpatil.elementaryeditor.ui.edit.contract.CropAction
import com.kshitijpatil.elementaryeditor.ui.edit.contract.EditUiEffect
import com.kshitijpatil.elementaryeditor.util.getBitmapPositionInsideImageView
import com.kshitijpatil.elementaryeditor.util.launchAndRepeatWithViewLifecycle
import com.kshitijpatil.elementaryeditor.util.viewLifecycleScope
import jp.wasabeef.transformers.glide.BlurTransformation
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
    //private var currentBitmap: Bitmap? = null

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
        /*binding.btnSave.setOnClickListener {
            val currentState = editViewModel.state.value
            val bitmap = tapNullWithTimber(currentBitmap) {
                "Current Bitmap is null, returning..."
            } ?: return@setOnClickListener
            val imageBounds = tapNullWithTimber(currentState.cropState.imageBounds) {
                "CropImageWorker: Image Bounds were not set, skipping..."
            } ?: return@setOnClickListener
            val cropBounds = tapNullWithTimber(currentState.cropState.cropBounds) {
                "CropImageWorker: Crop Bounds were not set, skipping..."
            } ?: return@setOnClickListener
            val viewWidth = imageBounds.width()
            val viewHeight = imageBounds.height()
            Glide.with(requireContext())
                .asBitmap()
                .load(bitmap)
                .transform(
                    OffsetCropTransformation(
                        cropBounds = toOffsetBounds(imageBounds, cropBounds),
                        viewWidth = viewWidth,
                        viewHeight = viewHeight
                    )
                ).addListener(object : DefaultRequestListener<Bitmap>(){
                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        currentBitmap = resource
                        return false
                    }
                }).into(binding.imgPreview)
        }*/
        return binding.root
    }

    /**
     * @return [IntArray] crop region coordinates in
     *  the (offsetX, offsetY, width, height) order. If the [initialBounds]
     *  or [currentBounds] are not initialized, the method will return null.
     */
    private fun toOffsetBounds(initialBounds: Rect, currentBounds: Rect): IntArray {
        val offsetX = currentBounds.left - initialBounds.left
        val offsetY = currentBounds.top - initialBounds.top
        val width = currentBounds.right - currentBounds.left
        val height = currentBounds.bottom - currentBounds.top
        return intArrayOf(offsetX, offsetY, width, height)
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

    private suspend fun observeCurrentBitmap() {
        editViewModel.state
            .map { Pair(it.currentBitmap, it.cropState.inProgress) }
            .stateIn(viewLifecycleScope)
            .collect { (bitmap, cropInProgress) ->
                if (bitmap != null) {
                    val context = requireContext()
                    var requestBuilder = Glide.with(context).load(bitmap)
                    if (cropInProgress) {
                        requestBuilder = requestBuilder.apply(
                            bitmapTransform(BlurTransformation(context, 25))
                        )
                    }
                    requestBuilder.thumbnail(0.1f).into(binding.imgPreview)
                    /*viewLifecycleScope.launch(Dispatchers.Default) {
                        val target = Glide.with(context)
                            .asBitmap()
                            .load(uri)
                            .thumbnail(0.1f)
                            .submit()

                        currentBitmap = target.get()
                        Timber.d("Thumbnail bitmap loaded in memory")
                    }*/
                }
                binding.imgPreview.isVisible = bitmap != null
                binding.cropOverlay.isVisible = bitmap != null && !cropInProgress
                binding.progressCrop.isVisible = bitmap == null || cropInProgress
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

open class DefaultRequestListener<T> : RequestListener<T> {
    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<T>?,
        isFirstResource: Boolean
    ): Boolean = false

    override fun onResourceReady(
        resource: T,
        model: Any?,
        target: Target<T>?,
        dataSource: DataSource?,
        isFirstResource: Boolean
    ): Boolean = false

}
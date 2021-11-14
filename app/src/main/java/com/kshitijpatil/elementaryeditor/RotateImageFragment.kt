package com.kshitijpatil.elementaryeditor

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.kshitijpatil.elementaryeditor.databinding.FragmentRotateImageBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber


class RotateImageFragment : Fragment() {
    private var _binding: FragmentRotateImageBinding? = null

    // only valid through onCreateView to onDestroyView
    private val binding: FragmentRotateImageBinding get() = _binding!!
    private var rotateBitmapJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentRotateImageBinding.inflate(
            inflater,
            container,
            false
        ).also {
            _binding = it
        }.root
    }

    private var currentBitmap: Bitmap? = null
    private val mutex = Mutex()
    private val options = RequestOptions()
        .sizeMultiplier(0.25f)
        .fitCenter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (currentBitmap == null) {
            loadDefaultBitmap()
        }
        binding.btnLeft.setOnClickListener {
            val source = currentBitmap
            if (source != null) {
                rotateBitmapJob?.cancel()
                rotateBitmapJob = rotateImageToLeft(source)
            } else {
                Timber.d("Current bitmap is not initialized, skipping rotate")
            }

        }
        binding.btnRight.setOnClickListener {
            val source = currentBitmap
            if (source != null) {
                rotateBitmapJob?.cancel()
                rotateBitmapJob = rotateImageToRight(source)
            } else {
                Timber.d("Current bitmap is not initialized, skipping rotate")
            }
        }
    }

    private fun loadDefaultBitmap() {
        val bitmapTarget = Glide.with(requireContext())
            .asBitmap()
            .apply(options)
            .load(R.drawable.bird_sample_image5x4)
            .submit()
        viewLifecycleScope.launch(Dispatchers.Default) {
            try {
                val loadedBitmap = bitmapTarget.get()
                currentBitmap = loadedBitmap
                setPreviewBitmap(loadedBitmap)
            } catch (throwable: Throwable) {
                Timber.e(throwable, "Failed loading bitmap")
            }
        }
    }

    private val Fragment.viewLifecycleScope get() = viewLifecycleOwner.lifecycleScope

    private suspend fun setPreviewBitmap(bitmap: Bitmap) {
        withContext(Dispatchers.Main.immediate) {
            binding.imgPreview.setImageBitmap(bitmap)
        }
    }

    private fun rotateImageToLeft(source: Bitmap): Job {
        return viewLifecycleScope.launch {
            rotateAndSetBitmap(source, -90f)
        }

        /*binding.imgPreview.apply {
            scaleType = ImageView.ScaleType.MATRIX
            imageMatrix = getRotationMatrixFor(-90f)
        }*/
    }

    private fun resizeBitmapForDisplay(bitmap: Bitmap): Bitmap {
        val display = activity!!.windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        val newWidth: Int = size.x

        //Get actual width and height of image
        val width: Int = bitmap.width
        val height: Int = bitmap.height

        // Calculate the ratio between height and width of Original Image
        val ratio = height.toFloat() / width.toFloat()
        val scale: Float = resources.displayMetrics.density
        val newHeight = ((width * ratio).toInt() / scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private suspend fun rotateAndSetBitmap(source: Bitmap, degrees: Float) {
        val updated = withContext(Dispatchers.Default) {
            source.rotateBy(degrees)
        }
        mutex.withLock { currentBitmap = updated }
        binding.imgPreview.setImageBitmap(updated)
    }

    private fun rotateImageToRight(source: Bitmap): Job {
        return viewLifecycleScope.launch {
            rotateAndSetBitmap(source, 90f)
        }

        /*binding.imgPreview.apply {
            scaleType = ImageView.ScaleType.MATRIX
            imageMatrix = getRotationMatrixFor(90f)
        }*/
    }

    private fun Bitmap.rotateBy(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun ImageView.getRotationMatrixFor(degrees: Float): Matrix {
        val rotationMatrix = Matrix()
        val pivotX = drawable.bounds.width() / 2f
        val pivotY = drawable.bounds.height() / 2f
        rotationMatrix.postRotate(degrees, pivotX, pivotY)
        return rotationMatrix
    }
}
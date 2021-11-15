package com.kshitijpatil.elementaryeditor

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
                    data.compress(Bitmap.CompressFormat.PNG, 100, it)
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
    private lateinit var imageSaver: ImageSaver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imageSaver = ImageSaver(
            registry = requireActivity().activityResultRegistry,
            resolver = requireContext().contentResolver
        )
        lifecycle.addObserver(imageSaver)
    }

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

        binding.btnSave.setOnClickListener { currentBitmap?.let { imageSaver.saveImage(it) } }
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
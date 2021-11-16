package com.kshitijpatil.elementaryeditor

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt


class CropImageFragment : Fragment(R.layout.fragment_crop_image) {
    private lateinit var ivPreview: ImageView
    private lateinit var cropOverlay: CropOverlay
    private lateinit var btnSave: Button
    private lateinit var imageSaver: ImageSaver

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
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view?.run {
            ivPreview = findViewById(R.id.img_preview)
            cropOverlay = findViewById(R.id.crop_overlay)
            btnSave = findViewById(R.id.btn_save)
            ivPreview.viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    ivPreview.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    getBitmapPositionInsideImageView(ivPreview)?.let {
                        cropOverlay.setImageBounds(it)
                    }
                }
            })
            btnSave.setOnClickListener {
                val bitmapTarget = Glide.with(requireContext())
                    .asBitmap()
                    //.apply(options)
                    .load(R.drawable.bird_sample_image5x4)
                    .submit()
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    try {
                        cropOverlay.getCropBbox()?.let { cropBounds ->
                            var processed = bitmapTarget.get()
                            val viewWidth = cropOverlay.initialBounds!!.width()
                            val viewHeight = cropOverlay.initialBounds!!.height()
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
                }
            }
        }
        return view
    }

    /**
     * Returns the bitmap position inside an imageView.
     * @param imageView source ImageView
     * @return [Rect] with required Bitmap bounds
     */
    fun getBitmapPositionInsideImageView(imageView: ImageView?): Rect? {
        if (imageView == null || imageView.drawable == null) return null

        // Get image dimensions
        // Get image matrix values and place them in an array
        val f = FloatArray(9)
        imageView.imageMatrix.getValues(f)

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        val scaleX = f[Matrix.MSCALE_X]
        val scaleY = f[Matrix.MSCALE_Y]

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        val d = imageView.drawable
        val origW = d.intrinsicWidth
        val origH = d.intrinsicHeight

        // Calculate the actual dimensions
        val actW = (origW * scaleX).roundToInt()
        val actH = (origH * scaleY).roundToInt()

        // Get image position
        // We assume that the image is centered into ImageView
        val imgViewW = imageView.width
        val imgViewH = imageView.height
        val top = (imgViewH - actH) / 2
        val left = (imgViewW - actW) / 2
        val right = left + actW
        val bottom = top + actH
        return Rect(left, top, right, bottom)
    }
}
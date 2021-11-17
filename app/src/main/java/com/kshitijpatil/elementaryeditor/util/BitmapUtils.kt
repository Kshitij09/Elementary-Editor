package com.kshitijpatil.elementaryeditor.util

import android.graphics.Matrix
import android.graphics.Rect
import android.widget.ImageView
import kotlin.math.roundToInt

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
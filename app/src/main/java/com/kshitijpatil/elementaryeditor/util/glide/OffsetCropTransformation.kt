package com.kshitijpatil.elementaryeditor.util.glide

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.toRect
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.kshitijpatil.elementaryeditor.util.Bound
import java.nio.charset.Charset
import java.security.MessageDigest

/**
 * Crop source bitmap with provided [cropBounds] in the
 * coordinate space of a view with [viewWidth] x [viewHeight]
 * dimensions
 */
class OffsetCropTransformation(
    private var cropBounds: Bound,
    private var viewWidth: Int,
    private var viewHeight: Int
) : BitmapTransformation() {
    private val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {

        val (offsetX, offsetY, width, height) = cropBounds
        val config = toTransform.config ?: Bitmap.Config.ARGB_8888

        val scaleX = toTransform.width / viewWidth.toFloat()
        val scaleY = toTransform.height / viewHeight.toFloat()
        val scaledOffsetX = offsetX * scaleX
        val scaledOffsetY = offsetY * scaleY
        val scaledWidth = width * scaleX
        val scaledHeight = height * scaleY

        val bitmap = pool.get(scaledWidth.toInt(), scaledHeight.toInt(), config)
        bitmap.density = toTransform.density
        bitmap.setHasAlpha(true)
        val canvas = Canvas(bitmap).apply {
            val sourceRect = RectF(
                scaledOffsetX,
                scaledOffsetY,
                scaledOffsetX + scaledWidth,
                scaledOffsetY + scaledHeight
            ).toRect()
            val targetRect = RectF(0f, 0f, scaledWidth, scaledHeight)
            drawBitmap(toTransform, sourceRect, targetRect, paint)
        }
        canvas.setBitmap(null)

        /*val bitmapBounds = Rect(0,0,toTransform.width,toTransform.height).toRectF()

        setCanvasBitmapDensity(toTransform, bitmap)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawRect(sourceRect, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(toTransform, 0f, 0f, paint)

        return Bitmap.createBitmap(
            toTransform,
            scaledOffsetX.toInt(),
            scaledOffsetY.toInt(),
            scaledWidth.toInt(),
            scaledHeight.toInt()
        )*/

        return bitmap
    }

    fun setCanvasBitmapDensity(toTransform: Bitmap, canvasBitmap: Bitmap) {
        canvasBitmap.density = toTransform.density
    }

    companion object {
        private val ID = "com.kshitijpatil.elementaryeditor.util.glide.OffsetCropTransformation"
        private val ID_BYTES = ID.toByteArray(Charset.forName("UTF-8"))
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OffsetCropTransformation) return false
        return true
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }

}
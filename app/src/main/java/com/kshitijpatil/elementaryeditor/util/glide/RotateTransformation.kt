package com.kshitijpatil.elementaryeditor.util.glide

import android.graphics.Bitmap
import android.graphics.Matrix
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.nio.charset.Charset
import java.security.MessageDigest

class RotateTransformation(private val rotationAngle: Float) : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val rotationMatrix = Matrix()
        rotationMatrix.postRotate(rotationAngle)
        return Bitmap.createBitmap(
            toTransform,
            0,
            0,
            toTransform.width,
            toTransform.height,
            rotationMatrix,
            true
        )
    }

    companion object {
        private val ID = "com.kshitijpatil.elementaryeditor.util.glide.RotateTransformation"
        private val ID_BYTES = ID.toByteArray(Charset.forName("UTF-8"))
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }
}
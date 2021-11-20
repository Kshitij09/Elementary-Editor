package com.kshitijpatil.elementaryeditor.worker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.graphics.toRect
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.kshitijpatil.elementaryeditor.R
import com.kshitijpatil.elementaryeditor.util.Bound
import com.kshitijpatil.elementaryeditor.util.createEditNotification
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*

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
        private val ID = "com.kshitijpatil.elementaryeditor.OffsetCropTransformation"
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

class CropImageWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        TODO()
        /*return runCatching {
            val resourceUri = getResourceUri()
            val cropBounds = getCropBounds()
            val (viewWidth, viewHeight) = getViewDimensions()
            val (offsetX, offsetY, width, height) = cropBounds

            val bitmapTarget = Glide.with(applicationContext)
                .asBitmap()
                .load(resourceUri)
                .transform(OffsetCropTransformation(cropBounds, viewWidth, viewHeight))
                .submit()

            val cropped = bitmapTarget.get()

            *//*val scaleX = processed.width / viewWidth.toFloat()
            val scaleY = processed.height / viewHeight.toFloat()
            val scaledOffsetX = offsetX * scaleX
            val scaledOffsetY = offsetY * scaleY
            val scaledWidth = width * scaleX
            val scaledHeight = height * scaleY

            processed = Bitmap.createBitmap(
                processed,
                scaledOffsetX.toInt(),
                scaledOffsetY.toInt(),
                scaledWidth.toInt(),
                scaledHeight.toInt()
            )*//*
            writeBitmapToFile(cropped)
        }.fold(
            onSuccess = { Result.success(workDataOf(WorkerConstants.KEY_IMAGE_URI to it.toString())) },
            onFailure = {
                Timber.e(it)
                Result.failure()
            }
        )*/
    }

    private fun getViewDimensions(): IntArray {
        val viewWidth = inputData.getInt(WorkerConstants.KEY_VIEW_WIDTH, 0)
        val viewHeight = inputData.getInt(WorkerConstants.KEY_VIEW_HEIGHT, 0)
        if (viewWidth == 0 || viewHeight == 0) {
            Timber.e("Invalid View dimensions, skipping...")
            throw IllegalArgumentException("Invalid crop coordinates")
        }
        return intArrayOf(viewWidth, viewHeight)

    }

    private fun getCropBounds(): IntArray {
        val cropBounds = inputData.getIntArray(WorkerConstants.KEY_CROP_BOUNDS)
        if (cropBounds == null || cropBounds.size != 4) {
            Timber.e("Invalid crop coordinates, skipping...")
            throw IllegalArgumentException("Invalid crop coordinates")
        }
        return cropBounds
    }

    private fun getResourceUri(): String {
        return inputData.getString(WorkerConstants.KEY_IMAGE_URI)
            ?: throw IllegalArgumentException("Invalid Input Uri")
    }

    /**
     * Writes a given [Bitmap] to the [Context.getFilesDir] directory.
     *
     * @param bitmap the [Bitmap] which needs to be written to the files directory.
     * @return a [Uri] to the output [Bitmap].
     */
    private fun writeBitmapToFile(
        bitmap: Bitmap
    ): Uri {
        // Bitmaps are being written to a temporary directory. This is so they can serve as inputs
        // for workers downstream, via Worker chaining.
        val name = "crop-output-${UUID.randomUUID()}.png"
        val outputDir = File(applicationContext.filesDir, WorkerConstants.CROP_OUTPUT_PATH)
        if (!outputDir.exists()) {
            outputDir.mkdirs() // should succeed
        }
        val outputFile = File(outputDir, name)
        FileOutputStream(outputFile).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /* ignored for PNG */, it)
        }
        return Uri.fromFile(outputFile)
    }


    /**
     * Create ForegroundInfo required to run a Worker in a foreground service.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            createEditNotification(
                applicationContext,
                id,
                applicationContext.getString(R.string.notification_title_crop_image)
            )
        )
    }

    companion object {
        // For a real world app you might want to use a different id for each Notification.
        const val NOTIFICATION_ID = 1
    }
}
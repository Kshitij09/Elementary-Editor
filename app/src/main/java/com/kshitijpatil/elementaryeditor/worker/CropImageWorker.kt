package com.kshitijpatil.elementaryeditor.worker

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.kshitijpatil.elementaryeditor.R
import com.kshitijpatil.elementaryeditor.util.createEditNotification
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CropImageWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        return runCatching {
            val resourceUri = getResourceUri()
            val cropBounds = getCropBounds()
            val (viewWidth, viewHeight) = getViewDimensions()
            val bitmapTarget = Glide.with(applicationContext)
                .asBitmap()
                .load(resourceUri)
                .submit()

            val (offsetX, offsetY, width, height) = cropBounds
            var processed = bitmapTarget.get()
            val scaleX = processed.width / viewWidth.toFloat()
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
            )
            writeBitmapToFile(processed)
        }.fold(
            onSuccess = { Result.success(workDataOf(WorkerConstants.KEY_IMAGE_URI to it.toString())) },
            onFailure = {
                Timber.e(it)
                Result.failure()
            }
        )
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
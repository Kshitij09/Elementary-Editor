package com.kshitijpatil.elementaryeditor.worker

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.kshitijpatil.elementaryeditor.R
import com.kshitijpatil.elementaryeditor.data.EditPayload
import com.kshitijpatil.elementaryeditor.util.createEditNotification
import com.kshitijpatil.elementaryeditor.util.glide.OffsetCropTransformation
import com.kshitijpatil.elementaryeditor.util.glide.RotateTransformation
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*

class EditImageWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val editPayloadListJsonAdapter: JsonAdapter<List<EditPayload>>
) : CoroutineWorker(appContext, workerParameters) {

    data class Params(
        val resourceUri: Uri,
        val viewWidth: Int,
        val viewHeight: Int,
        val editPayloadJson: String
    )

    override suspend fun doWork(): Result {
        val inputData = parseInputData() ?: return Result.failure()
        val editPayloads = withContext(Dispatchers.IO) {
            runCatching {
                editPayloadListJsonAdapter.fromJson(inputData.editPayloadJson)
            }.getOrNull()
        } ?: return Result.failure()
        val glideTransforms = editPayloads
            .map(::payloadToGlideTransform)
            .toTypedArray()
        val outputUri = withContext(Dispatchers.Default) {
            val bitmapTarget = Glide.with(applicationContext)
                .asBitmap()
                .load(inputData.resourceUri)
                .transform(*glideTransforms)
                .submit()
            val processed = bitmapTarget.get()
            writeBitmapToFile(processed)
        }
        return Result.success(workDataOf(WorkerConstants.KEY_IMAGE_URI to outputUri.toString()))
    }

    private fun payloadToGlideTransform(editPayload: EditPayload): BitmapTransformation {
        return when (editPayload) {
            is EditPayload.Crop -> {
                OffsetCropTransformation(
                    editPayload.cropBounds,
                    editPayload.viewWidth,
                    editPayload.viewHeight
                )
            }
            is EditPayload.Rotate -> {
                RotateTransformation(editPayload.degrees)
            }
        }
    }

    private fun parseInputData(): Params? {
        val resourceUri = inputData.getString(WorkerConstants.KEY_IMAGE_URI)?.toUri() ?: return null
        val viewWidth = inputData.getInt(WorkerConstants.KEY_VIEW_WIDTH, 0)
        val viewHeight = inputData.getInt(WorkerConstants.KEY_VIEW_HEIGHT, 0)
        if (viewWidth == 0 || viewHeight == 0) return null
        val editPayloadJson = inputData.getString(WorkerConstants.KEY_EDIT_PAYLOAD) ?: return null
        return Params(resourceUri, viewWidth, viewHeight, editPayloadJson)
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
                applicationContext.getString(R.string.notification_title_preparing_photo)
            )
        )
    }

    companion object {
        // For a real world app you might want to use a different id for each Notification.
        const val NOTIFICATION_ID = 1
    }
}
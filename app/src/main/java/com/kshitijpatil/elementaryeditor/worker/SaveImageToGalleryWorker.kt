package com.kshitijpatil.elementaryeditor.worker

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.kshitijpatil.elementaryeditor.R
import com.kshitijpatil.elementaryeditor.util.SaveImageStrategy
import com.kshitijpatil.elementaryeditor.util.createEditNotification
import timber.log.Timber

/**
 * Saves an output image to the [MediaStore].
 */
class SaveImageToGalleryWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val saveImageStrategy: SaveImageStrategy
) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val resolver = applicationContext.contentResolver
        return try {
            val input = Uri.parse(inputData.getString(WorkerConstants.KEY_IMAGE_URI))
            //val imageLocation = insertImage(resolver, input)
            val imageLocation = saveImageStrategy.saveImage(resolver, input)?.toString()
            if (imageLocation.isNullOrEmpty()) {
                Timber.e("Writing to MediaStore failed")
                Result.failure()
            }
            // Set the result of the worker by calling setOutputData().
            val output = Data.Builder()
                .putString(WorkerConstants.KEY_IMAGE_URI, imageLocation)
                .build()
            Result.success(output)
        } catch (exception: Exception) {
            Timber.e("Unable to save image to Gallery", exception)
            Result.failure()
        }
    }


    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID, createEditNotification(
                applicationContext, id,
                applicationContext.getString(R.string.notification_title_saving_image)
            )
        )
    }

    companion object {
        // Use same notification id as BaseFilter worker to update existing notification. For a real
        // world app you might consider using a different id for each notification.
        private const val NOTIFICATION_ID = 1
    }
}
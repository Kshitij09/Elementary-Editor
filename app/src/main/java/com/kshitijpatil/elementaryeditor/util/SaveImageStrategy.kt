package com.kshitijpatil.elementaryeditor.util

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.*

data class SaveOptions(
    val dateFormatPattern: String = "yyyyMMdd_HHmmss",
    val albumName: String = "Elementary Editor"
)

interface SaveImageStrategy {
    val saveOptions: SaveOptions
    fun saveImage(resolver: ContentResolver, resourceUri: Uri): Uri?
    fun getImageFilename(): String
}

class DefaultSaveImageStrategy(override val saveOptions: SaveOptions) : SaveImageStrategy {
    private val DATE_FORMATTER =
        SimpleDateFormat(saveOptions.dateFormatPattern, Locale.getDefault())

    override fun getImageFilename(): String = DATE_FORMATTER.format(Calendar.getInstance().time)

    override fun saveImage(resolver: ContentResolver, resourceUri: Uri): Uri? {
        val bitmap = BitmapFactory.decodeStream(resolver.openInputStream(resourceUri))
        val photosCollection = getPhotosCollection()
        val imageDetails = ContentValues().apply {
            val fileName = getImageFilename()
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${saveOptions.albumName}")
            }
        }

        return resolver.insert(photosCollection, imageDetails)?.also { imageUri ->
            resolver.openOutputStream(imageUri, "w").use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imageDetails.clear()
                imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, imageDetails, null, null)
            }
        }
    }

    private fun getPhotosCollection(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    }
}
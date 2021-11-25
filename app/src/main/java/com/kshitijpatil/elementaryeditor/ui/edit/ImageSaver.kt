package com.kshitijpatil.elementaryeditor.ui.edit

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
            registry.register(
                REQUEST_STORAGE_PERMISSION_KEY, owner,
                ActivityResultContracts.RequestPermission()
            ) {
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
                    data.compress(Bitmap.CompressFormat.JPEG, 100, it)
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
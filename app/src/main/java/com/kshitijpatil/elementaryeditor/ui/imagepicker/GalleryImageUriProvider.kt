package com.kshitijpatil.elementaryeditor.ui.imagepicker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

class GalleryImageUriProvider(
    appContext: Context,
    private val registry: ActivityResultRegistry,
    override val callback: OnImageUriCallback
) : ImageUriProvider() {
    private val hasReadExternalStoragePermission by lazy {
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private lateinit var selectImageLauncher: ActivityResultLauncher<String>
    private lateinit var selectImageWithPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(owner: LifecycleOwner) {
        selectImageLauncher = registry.register(SELECT_IMAGE_LAUNCH_KEY, owner, GetContent()) {
            callback.onImageUriReceived(it)
        }
        selectImageWithPermissionLauncher = registry.register(
            REQUEST_READ_EXTERNAL_STORAGE_LAUNCH_KEY,
            owner,
            RequestPermission()
        ) {
            launchSelectImage()
        }
    }

    override fun launch() {
        val preSdk28 = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
        if (preSdk28 && !hasReadExternalStoragePermission) {
            selectImageWithPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            launchSelectImage()
        }
    }

    private fun launchSelectImage() = selectImageLauncher.launch("image/*")

    companion object {
        const val SELECT_IMAGE_LAUNCH_KEY =
            "com.kshitijpatil.elementaryeditor.ui.imagepicker.SELECT_IMAGE_LAUNCH_KEY"
        const val REQUEST_READ_EXTERNAL_STORAGE_LAUNCH_KEY =
            "com.kshitijpatil.elementaryeditor.ui.imagepicker.REQUEST_READ_EXTERNAL_STORAGE_LAUNCH_KEY"
    }
}
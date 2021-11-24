package com.kshitijpatil.elementaryeditor.ui.imagepicker

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber

open class CameraImageUriProvider(
    appContext: Context,
    private val tempFileUriProvider: TempFileUriProvider,
    private val registry: ActivityResultRegistry,
    override var callback: OnImageUriCallback?
) : ImageUriProvider() {
    open val hasCameraFeature by lazy {
        appContext.packageManager.hasSystemFeature(
            PackageManager.FEATURE_CAMERA_ANY
        )
    }
    private var latestTmpUri: Uri? = null
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(owner: LifecycleOwner) {
        takePictureLauncher =
            registry.register(TAKE_PICTURE_LAUNCH_KEY, owner, TakePicture()) { succeed ->
                val imageUri = if (succeed) latestTmpUri else null
                callback?.onImageUriReceived(imageUri)
            }
    }

    override fun launch() {
        if (!hasCameraFeature) {
            Timber.e("Device doesn't support camera feature!")
            callback?.onImageUriReceived(null)
        }
        val tempFileUri = tempFileUriProvider.get()
        if (tempFileUri == null) {
            Timber.e("Failed retrieving temporary file uri!")
            return
        }
        latestTmpUri = tempFileUri
        takePictureLauncher.launch(tempFileUri)
    }

    companion object {
        const val TAKE_PICTURE_LAUNCH_KEY =
            "com.kshitijpatil.elementaryeditor.ui.imagepicker.TAKE_PICTURE_LAUNCH_KEY"
    }
}
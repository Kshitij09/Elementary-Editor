package com.kshitijpatil.elementaryeditor.ui.home

import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import com.kshitijpatil.elementaryeditor.R
import com.kshitijpatil.elementaryeditor.ui.edit.EditActivity
import com.kshitijpatil.elementaryeditor.ui.imagepicker.*
import com.kshitijpatil.elementaryeditor.util.openActivity
import timber.log.Timber

/**
 * Reference for Activity Result API:
 * https://medium.com/codex/how-to-use-the-android-activity-result-api-for-selecting-and-taking-images-5dbcc3e6324b
 */
class MainActivity : AppCompatActivity(), OnImageUriCallback {
    private lateinit var galleryImageUriProvider: ImageUriProvider
    private lateinit var cameraImageUriProvider: CameraImageUriProvider
    private lateinit var tempFileUriProvider: TempFileUriProvider
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        tempFileUriProvider = DefaultTempFileUriProvider(applicationContext)
        galleryImageUriProvider = GalleryImageUriProvider(
            appContext = applicationContext,
            registry = activityResultRegistry,
            callback = this
        )
        cameraImageUriProvider = CameraImageUriProvider(
            appContext = applicationContext,
            tempFileUriProvider = tempFileUriProvider,
            registry = activityResultRegistry,
            callback = this
        )
        lifecycle.addObserver(galleryImageUriProvider)
        lifecycle.addObserver(cameraImageUriProvider)
        findViewById<FrameLayout>(R.id.root_container).setOnClickListener {
            galleryImageUriProvider.launch()
        }
        findViewById<ImageView>(R.id.iv_launch_camera).run {
            isVisible = cameraImageUriProvider.hasCameraFeature
            setOnClickListener { cameraImageUriProvider.launch() }
        }
    }

    override fun onImageUriReceived(imageUri: Uri?) {
        if (imageUri == null) {
            Timber.e("Failed getting target imageUri!")
        } else {
            launchEditActivity(imageUri)
        }
    }

    private fun launchEditActivity(imageUri: Uri) {
        openActivity<EditActivity> {
            putString(IMAGE_URI_KEY_EXTRA, imageUri.toString())
        }
    }

    companion object {
        const val IMAGE_URI_KEY_EXTRA =
            "com.kshitijpatil.elementaryeditor.ui.home.IMAGE_URI_KEY_EXTRA"
    }
}
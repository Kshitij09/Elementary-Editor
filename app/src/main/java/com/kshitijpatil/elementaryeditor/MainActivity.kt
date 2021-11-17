package com.kshitijpatil.elementaryeditor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import timber.log.Timber
import java.io.File

/**
 * Reference for Activity Result API:
 * https://medium.com/codex/how-to-use-the-android-activity-result-api-for-selecting-and-taking-images-5dbcc3e6324b
 */
class MainActivity : AppCompatActivity() {
    private val selectImageLauncher = registerForActivityResult(GetContent()) { uri ->
        Timber.d("Selected file uri: $uri")
        launchEditActivity(uri)
    }
    private val takePictureLauncher = registerForActivityResult(TakePicture()) { isSuccess ->
        if (isSuccess) {
            val tmpUri = latestTmpUri ?: return@registerForActivityResult
            Timber.d("Latest Captured image is saved at $tmpUri")
            launchEditActivity(tmpUri)
        }
    }

    private var latestTmpUri: Uri? = null
    private val hasCameraFeature by lazy {
        applicationContext.packageManager.hasSystemFeature(
            PackageManager.FEATURE_CAMERA_FRONT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<FrameLayout>(R.id.root_container).setOnClickListener {
            selectImageLauncher.launch("image/*")
        }
        findViewById<ImageView>(R.id.iv_launch_camera).run {
            isVisible = hasCameraFeature
            setOnClickListener { takeImage() }
        }
    }

    private fun takeImage() {
        getTmpFileUri().let { uri ->
            latestTmpUri = uri
            takePictureLauncher.launch(uri)
        }
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("camera_capture", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(
            applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }

    private fun launchEditActivity(imageUri: Uri) {
        openActivity<EditActivity> {
            putString("image-uri", imageUri.toString())
        }
    }

    inline fun <reified T> Context.openActivity(extras: Bundle.() -> Unit = {}) {
        val intent = Intent(this, T::class.java)
        intent.putExtras(Bundle().apply(extras))
        startActivity(intent)
    }
}
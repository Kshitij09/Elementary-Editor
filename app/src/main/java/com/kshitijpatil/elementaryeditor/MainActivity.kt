package com.kshitijpatil.elementaryeditor

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<FrameLayout>(R.id.root_container).setOnClickListener {
            Timber.d("\'Anywhere\' clicked")
        }
        findViewById<ImageView>(R.id.iv_launch_camera).setOnClickListener {
            Timber.d("Camera Button clicked")
        }
    }
}
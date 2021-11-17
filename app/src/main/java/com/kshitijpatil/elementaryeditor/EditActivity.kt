package com.kshitijpatil.elementaryeditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kshitijpatil.elementaryeditor.ui.home.MainActivity
import timber.log.Timber

class EditActivity : AppCompatActivity(R.layout.activity_edit) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageUri = intent.getStringExtra(MainActivity.IMAGE_URI_KEY_EXTRA)
        Timber.d("Performing edit on $imageUri")
    }
}
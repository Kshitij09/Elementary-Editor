package com.kshitijpatil.elementaryeditor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import timber.log.Timber

class EditActivity : AppCompatActivity(R.layout.activity_edit) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imageUri = intent.getStringExtra("image-uri")
        Timber.d("Performing edit on $imageUri")
    }
}
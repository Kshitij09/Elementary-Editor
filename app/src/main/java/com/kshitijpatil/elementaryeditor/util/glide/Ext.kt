package com.kshitijpatil.elementaryeditor.util.glide

import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide

fun ImageView.loadThumbnail(bitmap: Bitmap, sizeMultiplier: Float = 0.1f) {
    Glide.with(context)
        .load(bitmap)
        .thumbnail(sizeMultiplier)
        .into(this)
}
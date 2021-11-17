package com.kshitijpatil.elementaryeditor.imagepicker

import android.net.Uri

fun interface OnImageUriCallback {
    fun onImageUriReceived(imageUri: Uri?)
}
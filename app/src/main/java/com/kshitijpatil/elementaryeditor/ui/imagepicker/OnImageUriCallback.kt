package com.kshitijpatil.elementaryeditor.ui.imagepicker

import android.net.Uri

fun interface OnImageUriCallback {
    fun onImageUriReceived(imageUri: Uri?)
}
package com.kshitijpatil.elementaryeditor.imagepicker

import androidx.lifecycle.DefaultLifecycleObserver

abstract class ImageUriProvider : DefaultLifecycleObserver {
    abstract fun launch()
    protected abstract val callback: OnImageUriCallback
}
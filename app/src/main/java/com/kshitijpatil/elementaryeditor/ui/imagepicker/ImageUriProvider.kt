package com.kshitijpatil.elementaryeditor.ui.imagepicker

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

abstract class ImageUriProvider : DefaultLifecycleObserver {
    abstract fun launch()
    protected open var callback: OnImageUriCallback? = null

    override fun onDestroy(owner: LifecycleOwner) {
        callback = null
    }
}
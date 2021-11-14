package com.kshitijpatil.elementaryeditor

import android.app.Application
import timber.log.Timber

class ElementaryEditorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
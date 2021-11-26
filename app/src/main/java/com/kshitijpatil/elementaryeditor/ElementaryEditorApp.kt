package com.kshitijpatil.elementaryeditor

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import com.kshitijpatil.elementaryeditor.di.WorkerModule
import timber.log.Timber

class ElementaryEditorApp : Application(), Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        val logLevel = if (BuildConfig.DEBUG) Log.DEBUG else Log.ERROR
        val delegatingWorkerFactory = DelegatingWorkerFactory().apply {
            addFactory(WorkerModule.provideEditImageWorkerFactory())
            addFactory(WorkerModule.provideSaveImageToGalleryWorkerFactory())
        }
        return Configuration.Builder()
            .setMinimumLoggingLevel(logLevel)
            .setWorkerFactory(delegatingWorkerFactory)
            .build()
    }
}
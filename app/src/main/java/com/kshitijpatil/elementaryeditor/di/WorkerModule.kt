package com.kshitijpatil.elementaryeditor.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.kshitijpatil.elementaryeditor.util.DefaultSaveImageStrategy
import com.kshitijpatil.elementaryeditor.util.SaveOptions
import com.kshitijpatil.elementaryeditor.worker.EditImageWorker
import com.kshitijpatil.elementaryeditor.worker.SaveImageToGalleryWorker

object WorkerModule {

    private fun createEditImageWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ): EditImageWorker {
        val jsonAdapter = MoshiModule.editPayloadListJsonAdapter
        return EditImageWorker(appContext, workerParams, jsonAdapter)
    }

    private fun createSaveImageToGalleryWorker(
        appContext: Context,
        workerParams: WorkerParameters
    ): SaveImageToGalleryWorker {
        // TODO: consider fetching it from BuildConfig
        val saveOptions = SaveOptions()
        val saveStrategy = DefaultSaveImageStrategy(saveOptions)
        return SaveImageToGalleryWorker(appContext, workerParams, saveStrategy)
    }

    private inline fun <reified T : ListenableWorker?> workerFactoryOf(
        crossinline workerProducer: (Context, WorkerParameters) -> T
    ): WorkerFactory {
        return object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters
            ): ListenableWorker? {
                return if (workerClassName == T::class.java.name)
                    return workerProducer(appContext, workerParameters)
                else null
            }

        }
    }

    fun provideEditImageWorkerFactory() = workerFactoryOf(::createEditImageWorker)
    fun provideSaveImageToGalleryWorkerFactory() = workerFactoryOf(::createSaveImageToGalleryWorker)
}
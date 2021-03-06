package com.kshitijpatil.elementaryeditor.ui.imagepicker

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.kshitijpatil.elementaryeditor.BuildConfig
import java.io.File
import java.lang.ref.WeakReference

fun interface TempFileUriProvider {
    fun get(): Uri?
}

class DefaultTempFileUriProvider(context: Context, private val prefix: String) :
    TempFileUriProvider {
    private val contextRef = WeakReference(context)
    override fun get(): Uri? {
        val context = contextRef.get() ?: return null
        val tmpFile = File.createTempFile(prefix, ".png", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(
            context.applicationContext,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
    }
}
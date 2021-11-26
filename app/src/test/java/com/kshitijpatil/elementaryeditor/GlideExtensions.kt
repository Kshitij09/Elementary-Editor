package com.kshitijpatil.elementaryeditor

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bumptech.glide.Glide
import com.google.common.truth.Truth.assertThat
import com.kshitijpatil.elementaryeditor.util.glide.ConcurrencyHelper
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class GlideExtensions {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = TestCoroutineDispatcher()
    val concurrencyHelpers = ConcurrencyHelper()

    @Test
    fun loadImage() {
        val futureTarget = Glide.with(context)
            .asBitmap()
            .load(R.drawable.bird_sample_image5x4)
            .thumbnail(0.1f)
            .submit()

        val bitmapRef = AtomicReference<Bitmap>()
        concurrencyHelpers.loadOnOtherThread {
            bitmapRef.set(futureTarget.get())
        }
        assertThat(bitmapRef.get()).isNotNull()
        println(bitmapRef.get().width)
        println(bitmapRef.get().height)

        /*.into(object: CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                assertThat(resource.width).isNotEqualTo(0)
                assertThat(resource.height).isNotEqualTo(0)
                println(resource.width)
                println(resource.height)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                TODO("Not yet implemented")
            }
        })*/
    }
}
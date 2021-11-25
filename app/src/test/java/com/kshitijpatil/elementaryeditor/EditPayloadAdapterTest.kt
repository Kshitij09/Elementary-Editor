package com.kshitijpatil.elementaryeditor

import com.kshitijpatil.elementaryeditor.data.EditPayload
import com.kshitijpatil.elementaryeditor.di.MoshiModule
import com.kshitijpatil.elementaryeditor.util.Bound
import org.junit.Assert.assertNotNull
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class EditPayloadAdapterTest {
    private val editPayloadJsonAdapter = MoshiModule.editPayloadListJsonAdapter

    @Test
    fun cropPayloadDeserialization() {
        val cropPayload = editPayloadJsonAdapter.fromJson(
            "[{\"type\":\"CROP\",\"offsetX\":0,\"offsetY\":0,\"width\":100,\"height\":100}]"
        )
        assertNotNull(cropPayload)
    }

    @Test
    fun cropPayloadSerialization() {
        val cropPayload = EditPayload.Crop(Bound(0, 0, 100, 100), 300, 400)
        val cropPayloadSerialized = editPayloadJsonAdapter.toJson(listOf(cropPayload))
        assertNotNull(cropPayloadSerialized)
    }

    @Test
    fun rotatePayloadDeserialization() {
        val rotatePayload =
            editPayloadJsonAdapter.fromJson("[{\"type\":\"ROTATE\",\"degrees\":90.0}]")
        assertNotNull(rotatePayload)
    }

    @Test
    fun rotatePayloadSerialization() {
        val rotatePayload = EditPayload.Rotate(90f)
        val rotatePayloadSerialized = editPayloadJsonAdapter.toJson(listOf(rotatePayload))
        assertNotNull(rotatePayloadSerialized)
    }
}
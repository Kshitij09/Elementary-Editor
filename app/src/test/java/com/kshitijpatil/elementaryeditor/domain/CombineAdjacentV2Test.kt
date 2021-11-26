package com.kshitijpatil.elementaryeditor.domain

import com.google.common.truth.Truth.assertThat
import com.kshitijpatil.elementaryeditor.data.EditPayload
import com.kshitijpatil.elementaryeditor.util.Bound
import org.junit.Test

class CombineAdjacentV2Test {
    private val combinePayloadStrategy = CombineAdjacentStrategy(
        ReduceCropPayloadStrategy2,
        ReduceRotatePayloadStrategy1
    )

    @Test
    fun left_right_right() {
        val imageWidth = 4608
        val imageHeight = 3456
        val editPayloads = listOf(
            EditPayload.Crop(Bound(offsetX = 1387, offsetY = 0, width = 3220, height = 3456)),
            EditPayload.Crop(Bound(offsetX = 0, offsetY = 0, width = 3578, height = 3456)),
            EditPayload.Crop(Bound(offsetX = 0, offsetY = 0, width = 3679, height = 3456)),
        )
        val reduced = combinePayloadStrategy.combine(editPayloads, imageWidth, imageHeight)
        println(reduced)
        assertThat(reduced).isNotEmpty()
    }
}
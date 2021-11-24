package com.kshitijpatil.elementaryeditor.data

import com.kshitijpatil.elementaryeditor.util.Bound
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
sealed class EditPayload() {
    abstract val type: String

    @JsonClass(generateAdapter = true)
    data class Crop(
        override val type: String,
        val offsetX: Int,
        val offsetY: Int,
        val width: Int,
        val height: Int
    ) : EditPayload()

    @JsonClass(generateAdapter = true)
    data class Rotate(
        override val type: String,
        val degrees: Float,
    ) : EditPayload()
}

fun Bound.toCropPayload(): EditPayload.Crop {
    return EditPayload.Crop(
        EditOperation.CROP.name,
        offsetX, offsetY, width, height
    )
}
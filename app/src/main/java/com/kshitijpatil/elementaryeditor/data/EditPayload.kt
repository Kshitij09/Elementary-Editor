package com.kshitijpatil.elementaryeditor.data

import com.kshitijpatil.elementaryeditor.util.Bound
import com.squareup.moshi.JsonClass

sealed class EditPayload(val type: EditOperation) {

    @JsonClass(generateAdapter = true)
    data class Crop(
        val offsetX: Int,
        val offsetY: Int,
        val width: Int,
        val height: Int
    ) : EditPayload(EditOperation.CROP)

    @JsonClass(generateAdapter = true)
    data class Rotate(val degrees: Float) : EditPayload(EditOperation.ROTATE)
}

fun Bound.toCropPayload(): EditPayload.Crop {
    return EditPayload.Crop(offsetX, offsetY, width, height)
}
package com.kshitijpatil.elementaryeditor.data

import com.kshitijpatil.elementaryeditor.util.Bound
import com.squareup.moshi.JsonClass

sealed class EditPayload(val type: EditOperation) {

    @JsonClass(generateAdapter = true)
    data class Crop(val cropBoundScaled: Bound) : EditPayload(EditOperation.CROP)

    @JsonClass(generateAdapter = true)
    data class Rotate(val degrees: Float) : EditPayload(EditOperation.ROTATE)
}
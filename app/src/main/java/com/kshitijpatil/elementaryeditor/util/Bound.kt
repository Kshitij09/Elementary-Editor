package com.kshitijpatil.elementaryeditor.util

import com.squareup.moshi.JsonClass

/**
 * Utility class to hold offset bounds of a
 * rectangular region
 */
@JsonClass(generateAdapter = true)
data class Bound(
    val offsetX: Int,
    val offsetY: Int,
    val width: Int,
    val height: Int
) {
    fun toBoundF(): BoundF = BoundF(
        offsetX.toFloat(),
        offsetY.toFloat(),
        width.toFloat(),
        height.toFloat()
    )

    fun scaleBy(scaleX: Float, scaleY: Float): Bound {
        val scaledOffsetX = offsetX * scaleX
        val scaledOffsetY = offsetY * scaleY
        val scaledWidth = width * scaleX
        val scaledHeight = height * scaleY
        return BoundF(scaledOffsetX, scaledOffsetY, scaledWidth, scaledHeight).toBound()
    }
}

/**
 * Float variant of [Bound]
 */
data class BoundF(
    val offsetX: Float,
    val offsetY: Float,
    val width: Float,
    val height: Float
) {
    fun toBound(): Bound = Bound(
        offsetX.toInt(),
        offsetY.toInt(),
        width.toInt(),
        height.toInt()
    )
}

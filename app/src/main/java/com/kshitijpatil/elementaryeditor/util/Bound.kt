package com.kshitijpatil.elementaryeditor.util

/**
 * Utility class to hold offset bounds of a
 * rectangular region
 */
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

package com.kshitijpatil.elementaryeditor.domain

import com.kshitijpatil.elementaryeditor.data.EditPayload
import com.kshitijpatil.elementaryeditor.util.Bound
import kotlin.math.min

val ReduceCropPayloadStrategy1 = ReduceCropPayloadStrategy { input, imageWidth, imageHeight ->
    input.reduce { acc, current ->
        val currentBounds = current.cropBoundScaled
        val offsetX = acc.cropBoundScaled.offsetX + currentBounds.offsetX
        val offsetY = acc.cropBoundScaled.offsetY + currentBounds.offsetY
        val width = min(acc.cropBoundScaled.width, currentBounds.width)
        val height = min(acc.cropBoundScaled.height, currentBounds.height)
        EditPayload.Crop(Bound(offsetX, offsetY, width, height))
    }
}

val ReduceCropPayloadStrategy2 = ReduceCropPayloadStrategy { input, imageWidth, imageHeight ->
    input.reduce { acc, current ->
        val offsetX = acc.cropBoundScaled.offsetX + current.cropBoundScaled.offsetX
        val offsetY = acc.cropBoundScaled.offsetY + current.cropBoundScaled.offsetY
        val accRightOffset = imageWidth - (acc.cropBoundScaled.offsetX + acc.cropBoundScaled.width)
        val currentRightOffset =
            imageWidth - (current.cropBoundScaled.offsetX + current.cropBoundScaled.width)
        val width = offsetX + (imageWidth - (accRightOffset + currentRightOffset))

        val accBottomOffset =
            imageHeight - (acc.cropBoundScaled.offsetY + acc.cropBoundScaled.height)
        val currentBottomOffset =
            imageHeight - (current.cropBoundScaled.offsetY + current.cropBoundScaled.height)
        val height = imageHeight - (accBottomOffset - currentBottomOffset + offsetY)
        EditPayload.Crop(Bound(offsetX, offsetY, width, height))
    }
}

val ReduceRotatePayloadStrategy1 = ReduceRotatePayloadStrategy { input, imageWidth, imageHeight ->
    input.reduce { acc, current ->
        val rotationAngle = (acc.degrees + current.degrees) % 360
        EditPayload.Rotate(rotationAngle)
    }
}

class CombineAdjacentStrategy(
    override val reduceCropPayloadStrategy: ReduceCropPayloadStrategy,
    override val reduceRotatePayloadStrategy: ReduceRotatePayloadStrategy
) : CombinePayloadStrategy {
    override fun combine(
        input: List<EditPayload>,
        imageWidth: Int,
        imageHeight: Int
    ): List<EditPayload> {
        val resultOperations = mutableListOf<EditPayload>()
        var sameOperationStart = 0
        var currentIndex = 1
        while (sameOperationStart < input.size) {
            while (
                currentIndex < input.size
                && (input[currentIndex].type == input[currentIndex - 1].type)
            ) currentIndex++
            val combinedPayload = combinePayloads(
                input,
                sameOperationStart,
                currentIndex - 1,
                imageWidth,
                imageHeight
            )
            resultOperations.add(combinedPayload)
            sameOperationStart = currentIndex
        }
        return resultOperations
    }

    private fun combinePayloads(
        input: List<EditPayload>,
        start: Int,
        end: Int,
        imageWidth: Int,
        imageHeight: Int
    ): EditPayload {
        return when (input[start]) {
            is EditPayload.Crop -> combineCropPayloads(
                input as List<EditPayload.Crop>,
                start,
                end,
                imageWidth,
                imageHeight
            )
            is EditPayload.Rotate -> combineRotatePayloads(
                input as List<EditPayload.Rotate>,
                start,
                end,
                imageWidth,
                imageHeight
            )
        }
    }

    private fun combineCropPayloads(
        input: List<EditPayload.Crop>,
        start: Int,
        end: Int,
        imageWidth: Int,
        imageHeight: Int
    ): EditPayload.Crop {
        return reduceCropPayloadStrategy.reduce(
            input.subList(start, end + 1),
            imageWidth,
            imageHeight
        )
    }

    private fun combineRotatePayloads(
        input: List<EditPayload.Rotate>,
        start: Int,
        end: Int,
        imageWidth: Int,
        imageHeight: Int
    ): EditPayload.Rotate {
        return reduceRotatePayloadStrategy.reduce(
            input.subList(start, end + 1),
            imageWidth,
            imageHeight
        )
    }
}
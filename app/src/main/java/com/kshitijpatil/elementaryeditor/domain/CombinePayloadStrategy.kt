package com.kshitijpatil.elementaryeditor.domain

import com.kshitijpatil.elementaryeditor.data.EditPayload

interface CombinePayloadStrategy {
    val reduceCropPayloadStrategy: ReduceCropPayloadStrategy
    val reduceRotatePayloadStrategy: ReduceRotatePayloadStrategy
    fun combine(input: List<EditPayload>, imageWidth: Int, imageHeight: Int): List<EditPayload>
}

fun interface ReduceCropPayloadStrategy {
    fun reduce(input: List<EditPayload.Crop>, imageWidth: Int, imageHeight: Int): EditPayload.Crop
}

fun interface ReduceRotatePayloadStrategy {
    fun reduce(
        input: List<EditPayload.Rotate>,
        imageWidth: Int,
        imageHeight: Int
    ): EditPayload.Rotate
}
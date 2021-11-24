package com.kshitijpatil.elementaryeditor.di

import com.kshitijpatil.elementaryeditor.data.EditOperation
import com.kshitijpatil.elementaryeditor.data.EditPayload
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory

object MoshiModule {
    private val editPayloadAdapter =
        PolymorphicJsonAdapterFactory.of(EditPayload::class.java, "type")
            .withSubtype(EditPayload.Crop::class.java, EditOperation.CROP.name)
            .withSubtype(EditPayload.Rotate::class.java, EditOperation.ROTATE.name)

    private val moshiWithEditPayloadAdapter by lazy {
        Moshi.Builder().add(editPayloadAdapter).build()
    }
}
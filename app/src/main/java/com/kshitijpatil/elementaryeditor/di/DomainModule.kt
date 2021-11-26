package com.kshitijpatil.elementaryeditor.di

import com.kshitijpatil.elementaryeditor.domain.CombineAdjacentStrategy
import com.kshitijpatil.elementaryeditor.domain.CombinePayloadStrategy
import com.kshitijpatil.elementaryeditor.domain.ReduceCropPayloadStrategy1
import com.kshitijpatil.elementaryeditor.domain.ReduceRotatePayloadStrategy1

object DomainModule {
    fun provideCombineOperationsStrategy(): CombinePayloadStrategy {
        return CombineAdjacentStrategy(ReduceCropPayloadStrategy1, ReduceRotatePayloadStrategy1)
    }
}
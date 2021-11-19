package com.kshitijpatil.elementaryeditor.ui.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapConcat
import timber.log.Timber

class LoggingMiddleware<A, S>(private val tag: String) : ReduxViewModel.MiddleWare<A, S> {
    override fun bind(
        actions: Flow<A>,
        state: StateFlow<S>
    ): Flow<A> {
        return actions.flatMapConcat { action ->
            Timber.v("$tag: action=$action, state=${state.value}")
            emptyFlow()
        }
    }
}
package com.kshitijpatil.elementaryeditor.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class ReduxViewModel<S, A>(initialState: S) : ViewModel() {
    interface MiddleWare<A, S> {
        fun bind(actions: Flow<A>, state: StateFlow<S>): Flow<A>
    }

    protected val _state = MutableStateFlow(initialState)
    val state: StateFlow<S>
        get() = _state.asStateFlow()
    protected val pendingActions = MutableSharedFlow<A>()
    protected abstract val middlewares: List<MiddleWare<A, S>>

    protected fun wire() {
        viewModelScope.launch {
            pendingActions
                .map { reduce(it, state.value) }
                .collect(_state::emit)
        }
        viewModelScope.launch {
            val internalActions = middlewares
                .map {
                    Timber.v("Wired ${it.javaClass.simpleName}")
                    it.bind(pendingActions, state)
                }.asFlow().flattenMerge()
            internalActions.collect(pendingActions::emit)
        }
    }

    abstract fun reduce(action: A, state: S): S

    fun submitAction(action: A) {
        viewModelScope.launch { pendingActions.emit(action) }
    }
}
package com.kshitijpatil.elementaryeditor.ui.edit.middleware

import android.graphics.Bitmap
import com.kshitijpatil.elementaryeditor.ui.edit.contract.*
import com.kshitijpatil.elementaryeditor.util.chronicle.Chronicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import timber.log.Timber

class BitmapChronicleMiddleware(
    private val bitmapChronicle: Chronicle<Bitmap>
) : EditMiddleware {
    override fun bind(
        actions: Flow<EditAction>,
        state: StateFlow<EditViewState>
    ): Flow<EditAction> {
        return actions.flatMapConcat { action ->
            flow {
                when (action) {
                    // persist current bitmap in the chronicle
                    is InternalAction.PersistBitmap -> {
                        Timber.d("[Before] BitmapChronicle=${bitmapChronicle.toList()}")
                        val chronicleCurrentEntry = bitmapChronicle.current
                        bitmapChronicle.add(action.bitmap)
                        emit(
                            InternalAction.StepsCountUpdated(
                                bitmapChronicle.forwardSteps,
                                bitmapChronicle.backwardSteps
                            )
                        )
                        /*if (chronicleCurrentEntry != action.bitmap) {
                        } else {
                            emit(InternalAction.PersistBitmapSkipped)
                        }*/
                        Timber.d("[After] BitmapChronicle=${bitmapChronicle.toList()}")
                    }
                    is Undo -> {
                        Timber.d("[Before] BitmapChronicle=${bitmapChronicle.toList()}")
                        emit(InternalAction.BitmapLoading)
                        val previousState = bitmapChronicle.undo()
                        emit(InternalAction.BitmapLoaded(previousState))
                        emit(
                            InternalAction.StepsCountUpdated(
                                bitmapChronicle.forwardSteps,
                                bitmapChronicle.backwardSteps
                            )
                        )
                        Timber.d("[After] BitmapChronicle=${bitmapChronicle.toList()}")
                    }
                    is Redo -> {
                        Timber.d("[Before] BitmapChronicle=${bitmapChronicle.toList()}")
                        emit(InternalAction.BitmapLoading)
                        val futureState = bitmapChronicle.redo()
                        emit(InternalAction.BitmapLoaded(futureState))
                        emit(
                            InternalAction.StepsCountUpdated(
                                bitmapChronicle.forwardSteps,
                                bitmapChronicle.backwardSteps
                            )
                        )
                        Timber.d("[After] BitmapChronicle=${bitmapChronicle.toList()}")
                    }
                    is PeekFirst -> {
                        Timber.d("[Before] BitmapChronicle=${bitmapChronicle.toList()}")
                        emit(InternalAction.BitmapLoading)
                        val current = state.value.currentBitmap
                        val firstState = bitmapChronicle.peekFirst()
                        emit(InternalAction.BitmapLoaded(firstState))
                        kotlinx.coroutines.delay(500L)
                        emit(InternalAction.BitmapLoaded(current))
                        Timber.d("[After] BitmapChronicle=${bitmapChronicle.toList()}")
                    }
                    else -> {
                    }
                }
            }

        }
    }
}
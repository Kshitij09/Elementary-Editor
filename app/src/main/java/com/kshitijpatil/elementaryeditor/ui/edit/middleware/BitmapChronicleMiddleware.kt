package com.kshitijpatil.elementaryeditor.ui.edit.middleware

import android.graphics.Bitmap
import com.kshitijpatil.elementaryeditor.data.EditPayload
import com.kshitijpatil.elementaryeditor.ui.edit.contract.*
import com.kshitijpatil.elementaryeditor.util.chronicle.Chronicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow

class BitmapChronicleMiddleware(
    private val bitmapChronicle: Chronicle<Pair<Bitmap, EditPayload?>>
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
                        bitmapChronicle.add(Pair(action.bitmap, action.editPayload))
                        emit(
                            InternalAction.StepsCountUpdated(
                                bitmapChronicle.forwardSteps,
                                bitmapChronicle.backwardSteps
                            )
                        )
                    }
                    is Undo -> {
                        emit(InternalAction.BitmapLoading)
                        val previousState = bitmapChronicle.undo()
                        emit(InternalAction.BitmapLoaded(previousState.first))
                        emit(
                            InternalAction.StepsCountUpdated(
                                bitmapChronicle.forwardSteps,
                                bitmapChronicle.backwardSteps
                            )
                        )
                    }
                    is Redo -> {
                        emit(InternalAction.BitmapLoading)
                        val futureState = bitmapChronicle.redo()
                        emit(InternalAction.BitmapLoaded(futureState.first))
                        emit(
                            InternalAction.StepsCountUpdated(
                                bitmapChronicle.forwardSteps,
                                bitmapChronicle.backwardSteps
                            )
                        )
                    }
                    is PeekFirst -> {
                        emit(InternalAction.BitmapLoading)
                        val firstState = bitmapChronicle.peekFirst()
                        emit(InternalAction.BitmapLoaded(firstState.first))

                    }
                    is LoadLatest -> {
                        emit(InternalAction.BitmapLoading)
                        emit(InternalAction.BitmapLoaded(bitmapChronicle.current?.first))
                    }
                    else -> {
                    }
                }
            }

        }
    }
}
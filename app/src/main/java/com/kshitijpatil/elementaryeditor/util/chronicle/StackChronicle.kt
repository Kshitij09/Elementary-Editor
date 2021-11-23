package com.kshitijpatil.elementaryeditor.util.chronicle

/** Stack based implementation of [Chronicle] */
class StackChronicle<T>(override val maxSteps: Int) : Chronicle<T> {
    private val undoStack: ArrayDeque<T> = ArrayDeque()
    private val redoStack: ArrayDeque<T> = ArrayDeque()
    private var _current: T? = null

    override val current: T?
        get() = _current

    override val backwardSteps: Int
        get() = undoStack.size

    override val forwardSteps: Int
        get() = redoStack.size

    override val isEmpty: Boolean
        get() = (backwardSteps == 0)

    /**
     * 0. If [maxSteps] are reached, remove first element from the [undoStack]
     * 1. if [current] != null, put it in the [undoStack]
     * 2. Change [current] state to [newState]
     * 3. Reset the [redoStack]
     */
    override fun add(newState: T) {
        if (undoStack.size == maxSteps) {
            undoStack.removeFirstOrNull()
        }
        current?.let { undoStack.add(it) }
        _current = newState
        redoStack.clear()
    }

    /**
     * 0. If [current] != null, put it in the [redoStack]
     * 1. Assign element popped out from the [undoStack] to [current]
     */
    override fun undo(): T {
        check(current != null && undoStack.isNotEmpty()) {
            "No backward steps available"
        }
        redoStack.add(current!!)
        _current = undoStack.removeLast()
        return current!!
    }

    override fun peekFirst(): T {
        check(undoStack.isNotEmpty()) {
            "No History found"
        }
        return undoStack.first()
    }


    /**
     * 0. If [current] != null, put it in the [undoStack]
     * 1. Assign element popped out from the [redoStack] to [current]
     */
    override fun redo(): T {
        check(current != null && redoStack.isNotEmpty()) {
            "No forward steps available"
        }
        undoStack.add(current!!)
        _current = redoStack.removeLast()
        return current!!
    }

    override fun reset() {
        undoStack.clear()
        redoStack.clear()
    }

    override fun toList(): List<T> {
        val stateList = mutableListOf<T>()
        stateList.addAll(undoStack)
        stateList.addAll(redoStack)
        return stateList.toList()
    }
}
package com.kshitijpatil.elementaryeditor.util.chronicle

class SynchronizedChronicle<T>(private val decorated: Chronicle<T>) : Chronicle<T> {
    private val lock = Object()

    override val maxSteps: Int
        get() = decorated.maxSteps

    override val current: T?
        get() = synchronized(lock) { decorated.current }
    override val backwardSteps: Int
        get() = synchronized(lock) { decorated.backwardSteps }
    override val forwardSteps: Int
        get() = synchronized(lock) { decorated.forwardSteps }

    override fun add(newState: T) {
        synchronized(lock) {
            decorated.add(newState)
        }
    }

    override fun undo(): T {
        return synchronized(lock) {
            decorated.undo()
        }
    }

    override fun redo(): T {
        return synchronized(lock) {
            decorated.redo()
        }
    }

    override fun reset() {
        synchronized(lock) {
            decorated.reset()
        }
    }

    override fun toList(): List<T> {
        return synchronized(lock) {
            decorated.toList()
        }
    }

    override fun peekFirst(): T {
        return synchronized(lock) {
            decorated.peekFirst()
        }
    }
}
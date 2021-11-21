package com.kshitijpatil.elementaryeditor.util.chronicle

/**
 * A Fixed size, Journal Keeping 'Originator' from
 * the Memento pattern with undo and redo APIs
 */
interface Chronicle<T> {
    /** Fixed number of elements to maintain, potentially by dropping the older ones */
    val maxSteps: Int

    /** The latest state from the maintained journal of intermediaries */
    val current: T?

    /** Number of elements available in the Back Stack from the [current] state */
    val backwardSteps: Int

    /** Number of elements available in the Future from the [current] state */
    val forwardSteps: Int

    val isEmpty: Boolean
        get() = (backwardSteps == 0)

    /** Enqueue new state to the journal of events */
    fun add(newState: T)

    /** Rollback to the Penultimate State
     * @throws IllegalStateException If this method was called with [backwardSteps] = 0
     */
    fun undo(): T

    /** Returns first entry from the back stack (if available) */
    fun peekFirst(): T?

    /** Advance one step future in the series of recorded states,
     * @throws IllegalStateException If this method was called with [forwardSteps] = 0
     */
    fun redo(): T

    /** Remove all the journal entries */
    fun reset()

    /** Return the modifications journal as [List] in a order shown below
     *
     * ---- undo stack -- current -- redo stack ----
     */
    fun toList(): List<T>
}
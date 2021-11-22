package com.kshitijpatil.elementaryeditor.util.chronicle

fun <T> Chronicle<T>.synchronized(): Chronicle<T> {
    return SynchronizedChronicle(this)
}

fun <T> createChronicle(maxSize: Int, threadSafe: Boolean = false): Chronicle<T> {
    val stackChronicle = StackChronicle<T>(maxSize)
    return if (threadSafe) stackChronicle.synchronized() else stackChronicle
}
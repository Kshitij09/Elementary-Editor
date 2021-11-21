package com.kshitijpatil.elementaryeditor

import com.kshitijpatil.elementaryeditor.util.chronicle.Chronicle
import com.kshitijpatil.elementaryeditor.util.chronicle.StackChronicle
import com.kshitijpatil.elementaryeditor.util.chronicle.createChronicle
import org.junit.Assert.assertEquals
import org.junit.Test


class ChronicleTest {
    @Test
    fun chronicleWorking() {
        val chronicle = StackChronicle<Int>(10)
        chronicle.add(1)
        chronicle.add(2)
        chronicle.add(3)
        assertEquals(chronicle.current, 3)
        assertEquals(chronicle.undo(), 2)
        assertEquals(chronicle.redo(), 3)
        assertEquals(chronicle.undo(), 2)
        assertEquals(chronicle.undo(), 1)
        chronicle.add(4)
        assertEquals(chronicle.current, 4)
        assertEquals(chronicle.undo(), 1)
        chronicle.add(5)
        assertEquals(chronicle.current, 5)
        chronicle.add(6)
        assertEquals(chronicle.current, 6)
        assertEquals(chronicle.undo(), 5)
        assertEquals(chronicle.undo(), 1)
        assertEquals(chronicle.redo(), 5)
        assertEquals(chronicle.redo(), 6)
    }

    @Test
    fun chronicleThreadSafety() {
        val intChronicle: Chronicle<Int> = createChronicle(1_000_000, threadSafe = true)
        val thread1 = Thread {
            1.rangeTo(500_000).forEach {
                intChronicle.add(it)
            }
        }

        val thread2 = Thread {
            1.rangeTo(500_000).forEach {
                intChronicle.add(it)
            }
        }
        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()

        assertEquals(intChronicle.backwardSteps, 1_000_000)
    }
}
package com.ncy.notechaty.data.sync.event

import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ReceiptLogTest {
    @get:Rule val temp = TemporaryFolder()

    @Test
    fun `receipts are sequential, idempotent and survive reopening`() {
        val log = ReceiptLog(temp.root)
        assertTrue(log.fresh)
        assertEquals(1L, log.record("chat", "a"))
        assertEquals(2L, log.record("chat", "b"))
        assertEquals("an event is numbered once", 1L, log.record("chat", "a"))

        val reopened = ReceiptLog(temp.root)
        assertFalse(reopened.fresh)
        assertEquals(log.epoch, reopened.epoch)
        assertEquals(2L, reopened.receiptOf("b"))
        assertEquals(listOf("b"), reopened.after(1L).map { it.eventId })
    }

    @Test
    fun `a lost epoch starts a new sequence`() {
        val log = ReceiptLog(temp.root)
        log.record("chat", "a")
        File(temp.root, ReceiptLog.EPOCH_FILE).delete()

        val reopened = ReceiptLog(temp.root)
        assertTrue(reopened.fresh)
        assertNotEquals(log.epoch, reopened.epoch)
        assertEquals("old receipts belong to the old epoch", 0L, reopened.latest())
    }

    @Test
    fun `a torn final line neither breaks the log nor swallows the next record`() {
        ReceiptLog(temp.root).record("chat", "a")
        FileOutputStream(File(temp.root, ReceiptLog.LOG_FILE), true).use {
            it.write("""{"s":2,"c":"chat",""".toByteArray())
        }

        val reopened = ReceiptLog(temp.root)
        assertFalse(reopened.fresh)
        assertEquals(2L, reopened.record("chat", "b"))
        assertEquals(2L, ReceiptLog(temp.root).receiptOf("b"))
    }

    @Test
    fun `the same layout as ncy-core is read back`() {
        File(temp.root, ReceiptLog.EPOCH_FILE).writeText("epoch-1")
        File(temp.root, ReceiptLog.LOG_FILE).writeText(
            """{"s":1,"c":"chat","e":"a"}""" + "\n" + """{"s":2,"c":"chat","e":"b"}""" + "\n"
        )
        val log = ReceiptLog(temp.root)
        assertEquals("epoch-1", log.epoch)
        assertEquals(listOf(1L, 2L), log.after(0L).map { it.seq })
    }
}

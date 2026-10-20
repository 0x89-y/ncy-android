package com.ncy.notechaty.data.sync.transport

import com.ncy.notechaty.data.sync.event.HlcTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Receipt fields (§10.9) travel when set, and stay out of frames for older peers. */
class SyncWireReceiptTest {
    private val codec = SyncWireCodec()
    private val hlc = HlcTimestamp(1_000L, 0, "node")

    @Test
    fun `receipt fields round-trip`() {
        val hello = SyncWireMessage.Hello("phone", "auth", hlc, receiptEpoch = "", sinceReceipt = 7L)
        val decoded = codec.decode(codec.encode(hello)) as SyncWireMessage.Hello
        assertEquals("", decoded.receiptEpoch)
        assertEquals(7L, decoded.sinceReceipt)

        val ack = codec.decode(codec.encode(SyncWireMessage.Ack(hlc, upToReceipt = 9L))) as SyncWireMessage.Ack
        assertEquals(9L, ack.upToReceipt)

        val welcome = codec.decode(codec.encode(SyncWireMessage.Welcome("hub", "Hub", "epoch"))) as SyncWireMessage.Welcome
        assertEquals("epoch", welcome.receiptEpoch)
    }

    @Test
    fun `frames without receipts look exactly as before`() {
        val hello = codec.encode(SyncWireMessage.Hello("phone", "auth", hlc))
        assertFalse(hello.contains("receipt"))
        assertFalse(codec.encode(SyncWireMessage.Ack(hlc)).contains("Receipt"))

        val legacy = codec.decode("""{"type":"Welcome","deviceId":"hub","name":"Hub"}""") as SyncWireMessage.Welcome
        assertNull(legacy.receiptEpoch)
    }
}

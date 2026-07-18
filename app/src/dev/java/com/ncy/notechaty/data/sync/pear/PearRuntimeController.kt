package com.ncy.notechaty.data.sync.pear

import android.util.Log
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import to.holepunch.bare.kit.IPC
import to.holepunch.bare.kit.Worklet

/**
 * Owns the experimental Bare worklet in Ncy Dev.
 *
 * This first stage is deliberately network-free: it proves native loading, IPC, and Android
 * lifecycle behavior before Hyperswarm is bundled. Construction has no side effects; Bare's
 * native library is loaded only after the user explicitly enables Pear and [start] is called.
 */
@Singleton
class PearRuntimeController @Inject constructor() {
    private var worklet: Worklet? = null
    private var ipc: IPC? = null

    @Synchronized
    fun start() {
        if (worklet != null) return

        runCatching {
            val nextWorklet = Worklet(Worklet.Options().memoryLimit(MEMORY_LIMIT_BYTES))
            val nextIpc = IPC(nextWorklet)
            nextIpc.readable {
                while (true) {
                    val data = nextIpc.read() ?: break
                    Log.i(TAG, "Bare IPC: ${data.toUtf8String()}")
                }
            }
            nextWorklet.start(
                "/ncy-pear-probe.js",
                WORKLET_SOURCE,
                StandardCharsets.UTF_8,
                emptyArray(),
            )
            worklet = nextWorklet
            ipc = nextIpc
        }.onFailure { error ->
            Log.e(TAG, "Could not start Bare worklet", error)
            stop()
        }
    }

    @Synchronized
    fun stop() {
        val activeIpc = ipc
        val activeWorklet = worklet
        ipc = null
        worklet = null
        runCatching { activeIpc?.close() }
        runCatching { activeWorklet?.terminate() }
    }

    private fun ByteBuffer.toUtf8String(): String {
        val bytes = ByteArray(remaining())
        get(bytes)
        return bytes.toString(StandardCharsets.UTF_8)
    }

    private companion object {
        const val TAG = "NcyPear"
        const val MEMORY_LIMIT_BYTES = 24 * 1024 * 1024
        val WORKLET_SOURCE = """
            const { IPC } = BareKit

            Bare.on('uncaughtException', (error) => {
              console.error(error)
              IPC.write(Buffer.from(JSON.stringify({ type: 'error', message: error.message })))
            })
            Bare.on('unhandledRejection', (error) => {
              console.error(error)
              IPC.write(Buffer.from(JSON.stringify({ type: 'error', message: String(error) })))
            })
            Bare.on('suspend', () => IPC.unref())
            Bare.on('resume', () => IPC.ref())
            IPC.on('data', (data) => IPC.write(data))
            IPC.write(Buffer.from(JSON.stringify({ type: 'ready' })))
        """.trimIndent()
    }
}

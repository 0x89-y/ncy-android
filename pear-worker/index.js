'use strict'

const Hyperswarm = require('hyperswarm')
const b4a = require('b4a')
const { IPC } = BareKit

let swarm = null
let buffered = ''
let dispatchQueue = Promise.resolve()
let pendingDispatches = 0
let backpressureActive = false
// BareKit's IPC has no pause(), so the worker cannot stop the host writing. Instead
// it reports congestion and the host throttles at the source. Without this a large
// blob is enqueued in full before any of it reaches the wire.
const DISPATCH_HIGH_WATER = 4
const DISPATCH_LOW_WATER = 1
let nextConnectionId = 1
const connections = new Map()
const allowedPeers = new Set()
const MAX_IPC_CHARS = 2 * 1024 * 1024
const MAX_FRAME_BYTES = 1400 * 1024
/** Cap on waiting for a stream to drain — see `writeBackpressured`. */
const WRITE_DRAIN_TIMEOUT_MS = 15000

/**
 * Write, and report whether the caller has to wait.
 *
 * `write()` returning false means the stream's buffer is over its high-water mark
 * and the writer is expected to stop until 'drain'. Ignoring that is fine for one
 * small frame and fatal for a sustained multi-chunk blob transfer: the buffer grows
 * without bound and the stream stops making progress, taking every other frame on
 * the same connection down with it. Returns null when there is room, or a promise
 * that settles once there is.
 */
function writeBackpressured(stream, payload) {
  if (stream.write(payload)) return null
  // Bounded wait. A stream whose peer has gone away accepts the write, returns
  // false, and then never drains — so an unbounded await parks the serial dispatch
  // queue forever and the host's send window never reopens. Resolving on timeout
  // keeps things moving; the window still limits how fast a dead link is fed.
  return new Promise((resolve) => {
    let settled = false
    const finish = () => {
      if (settled) return
      settled = true
      clearTimeout(timer)
      if (typeof stream.off === 'function') stream.off('drain', finish)
      resolve()
    }
    const timer = setTimeout(() => {
      reply({ type: 'log', message: 'write drain timed out' })
      finish()
    }, WRITE_DRAIN_TIMEOUT_MS)
    stream.once('drain', finish)
    if (stream.destroyed) finish()
  })
}

function reply(message) {
  return writeBackpressured(IPC, b4a.from(JSON.stringify(message) + '\n'))
}

/** Tell the host to stop or resume feeding commands as the queue fills and drains. */
function signalBackpressure() {
  if (!backpressureActive && pendingDispatches > DISPATCH_HIGH_WATER) {
    backpressureActive = true
    reply({ type: 'backpressure', active: true })
  } else if (backpressureActive && pendingDispatches <= DISPATCH_LOW_WATER) {
    backpressureActive = false
    reply({ type: 'backpressure', active: false })
  }
}

async function start(message) {
  if (swarm) return
  const seed = b4a.from(message.seed, 'base64')
  if (seed.byteLength !== 32) throw new Error('Pear seed must be exactly 32 bytes')

  swarm = new Hyperswarm({ seed, maxPeers: 8, maxParallel: 2 })
  swarm.on('connection', (connection, info) => {
    const publicKey = b4a.toString(info.publicKey, 'hex')
    // Outgoing targets must come from authenticated pairing data. Incoming Noise
    // streams still have to pass Ncy's pairing-secret handshake above this layer.
    if (info.client && !allowedPeers.has(publicKey)) {
      connection.destroy(new Error('Unpaired Pear peer'))
      return
    }
    const id = String(nextConnectionId++)
    connections.set(id, { connection, publicKey: info.publicKey })
    let frames = b4a.alloc(0)
    reply({ type: 'connected', id, publicKey, client: Boolean(info.client) })
    connection.on('data', (chunk) => {
      frames = b4a.concat([frames, chunk])
      while (frames.byteLength >= 4) {
        const size = frames.readUInt32BE(0)
        if (size > MAX_FRAME_BYTES) {
          connection.destroy(new Error('Pear frame exceeds limit'))
          return
        }
        if (frames.byteLength < size + 4) return
        const frame = frames.subarray(4, size + 4)
        frames = frames.subarray(size + 4)
        // Frames already in `frames` are in memory, so finish decoding them; but if
        // the IPC channel is congested, stop pulling *new* data off the peer until
        // it clears. Without this a large inbound blob outruns the host's reader.
        const draining = reply({ type: 'frame', id, data: b4a.toString(frame, 'base64') })
        if (draining) {
          connection.pause()
          draining.then(() => connection.resume())
        }
      }
    })
    connection.on('close', () => {
      connections.delete(id)
      if (info.client) swarm?.leavePeer(info.publicKey)
      reply({ type: 'disconnected', id, publicKey })
    })
    connection.on('error', (error) => {
      reply({ type: 'connection-error', id, publicKey, message: error.message })
    })
  })
  swarm.on('error', (error) => reply({ type: 'error', message: error.message }))
  await swarm.listen()
  reply({
    type: 'ready',
    publicKey: b4a.toString(swarm.keyPair.publicKey, 'hex')
  })
}

async function stop() {
  const active = swarm
  swarm = null
  connections.clear()
  if (active) await active.destroy()
  reply({ type: 'stopped' })
}

async function dispatch(message) {
  switch (message.type) {
    case 'start': return start(message)
    case 'stop': return stop()
    case 'allow':
      allowedPeers.clear()
      for (const key of message.publicKeys || []) allowedPeers.add(key)
      return
    case 'connect': {
      if (!swarm) throw new Error('Pear is not ready')
      if (!allowedPeers.has(message.publicKey)) throw new Error('Pear peer is not paired')
      swarm.joinPeer(b4a.from(message.publicKey, 'hex'))
      return
    }
    case 'cancel-connect': {
      if (swarm) swarm.leavePeer(b4a.from(message.publicKey, 'hex'))
      return
    }
    case 'send': {
      const entry = connections.get(String(message.id))
      if (!entry) throw new Error('Unknown Pear connection')
      const frame = b4a.from(message.data, 'base64')
      if (frame.byteLength > MAX_FRAME_BYTES) throw new Error('Pear frame exceeds limit')
      const header = b4a.alloc(4)
      header.writeUInt32BE(frame.byteLength, 0)
      // Awaited: the dispatch queue is serial, so waiting here propagates real
      // backpressure all the way back to the host instead of buffering forever.
      await writeBackpressured(entry.connection, b4a.concat([header, frame]))
      // Acknowledge only once the frame is actually on the wire. The host keeps a
      // bounded number of sends unacknowledged, which is the only thing that stops
      // it handing over a whole multi-megabyte blob faster than the link can carry
      // it — an advisory "slow down" signal arrives far too late to help.
      reply({ type: 'sent', id: String(message.id) })
      return
    }
    case 'close': {
      const entry = connections.get(String(message.id))
      if (entry) {
        swarm?.leavePeer(entry.publicKey)
        entry.connection.end()
      }
      return
    }
    default: throw new Error(`Unknown command: ${message.type}`)
  }
}

IPC.on('data', (chunk) => {
  buffered += b4a.toString(chunk)

  while (true) {
    const newline = buffered.indexOf('\n')
    if (newline === -1) break
    const line = buffered.slice(0, newline)
    buffered = buffered.slice(newline + 1)
    if (!line) continue
    if (line.length > MAX_IPC_CHARS) {
      reply({ type: 'error', message: 'IPC command exceeds limit' })
      continue
    }
    // Serialized, not `Promise.resolve().then(...)` per command: `send` awaits
    // drain, and independent chains would let a later frame's write overtake an
    // earlier one. The stream is length-prefixed, so one reordered write
    // desynchronizes framing for the rest of the connection.
    pendingDispatches++
    signalBackpressure()
    dispatchQueue = dispatchQueue.then(async () => {
      const message = JSON.parse(line)
      try {
        await dispatch(message)
      } catch (error) {
        reply({
          type: 'command-error',
          command: message.type,
          id: message.id,
          publicKey: message.publicKey,
          message: error.message
        })
      }
    }).catch((error) => reply({ type: 'error', message: error.message }))
      .then(() => {
        pendingDispatches--
        signalBackpressure()
      })
  }

  // Only an unfinished command remains here. Complete adjacent commands may
  // legitimately total more than this limit in one IPC read and were drained
  // above; the bound applies to one command, never the whole read buffer.
  if (buffered.length > MAX_IPC_CHARS) {
    buffered = ''
    reply({ type: 'error', message: 'IPC command exceeds limit' })
  }
})

Bare.on('suspend', async () => {
  IPC.unref()
  await stop()
})
Bare.on('resume', () => IPC.ref())
Bare.on('uncaughtException', (error) => reply({ type: 'fatal', message: error.message }))
Bare.on('unhandledRejection', (error) => reply({ type: 'fatal', message: String(error) }))

reply({ type: 'booted' })

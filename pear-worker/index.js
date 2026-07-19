'use strict'

const Hyperswarm = require('hyperswarm')
const b4a = require('b4a')
const { IPC } = BareKit

let swarm = null
let buffered = ''
let nextConnectionId = 1
const connections = new Map()
const allowedPeers = new Set()
const MAX_IPC_CHARS = 2 * 1024 * 1024
const MAX_FRAME_BYTES = 1400 * 1024

function reply(message) {
  IPC.write(b4a.from(JSON.stringify(message) + '\n'))
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
        reply({ type: 'frame', id, data: b4a.toString(frame, 'base64') })
      }
    })
    connection.on('close', () => {
      connections.delete(id)
      if (info.client) swarm?.leavePeer(info.publicKey)
      reply({ type: 'disconnected', id, publicKey })
    })
    connection.on('error', (error) => {
      reply({ type: 'connection-error', message: error.message })
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
    case 'send': {
      const entry = connections.get(String(message.id))
      if (!entry) throw new Error('Unknown Pear connection')
      const frame = b4a.from(message.data, 'base64')
      if (frame.byteLength > MAX_FRAME_BYTES) throw new Error('Pear frame exceeds limit')
      const header = b4a.alloc(4)
      header.writeUInt32BE(frame.byteLength, 0)
      entry.connection.write(b4a.concat([header, frame]))
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
  if (buffered.length > MAX_IPC_CHARS) {
    buffered = ''
    reply({ type: 'error', message: 'IPC command exceeds limit' })
    return
  }

  while (true) {
    const newline = buffered.indexOf('\n')
    if (newline === -1) break
    const line = buffered.slice(0, newline)
    buffered = buffered.slice(newline + 1)
    if (!line) continue
    Promise.resolve()
      .then(() => dispatch(JSON.parse(line)))
      .catch((error) => reply({ type: 'error', message: error.message }))
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

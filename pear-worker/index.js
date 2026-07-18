'use strict'

const Hyperswarm = require('hyperswarm')
const b4a = require('b4a')
const { IPC } = BareKit

let swarm = null
let buffered = ''

function reply(message) {
  IPC.write(b4a.from(JSON.stringify(message) + '\n'))
}

async function start(message) {
  if (swarm) return
  const seed = b4a.from(message.seed, 'base64')
  if (seed.byteLength !== 32) throw new Error('Pear seed must be exactly 32 bytes')

  swarm = new Hyperswarm({ seed, maxPeers: 8, maxParallel: 2 })
  swarm.on('connection', (connection, info) => {
    reply({ type: 'connected', publicKey: b4a.toString(info.publicKey, 'hex') })
    connection.on('close', () => {
      reply({ type: 'disconnected', publicKey: b4a.toString(info.publicKey, 'hex') })
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
  if (active) await active.destroy()
  reply({ type: 'stopped' })
}

async function dispatch(message) {
  switch (message.type) {
    case 'start': return start(message)
    case 'stop': return stop()
    default: throw new Error(`Unknown command: ${message.type}`)
  }
}

IPC.on('data', (chunk) => {
  buffered += b4a.toString(chunk)
  if (buffered.length > 64 * 1024) {
    buffered = ''
    reply({ type: 'error', message: 'IPC command exceeds 64 KiB' })
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

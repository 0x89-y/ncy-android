# Ncy Sync Protocol — v2 (authenticated transport, phases 1, 2a, and 2b)

This document specifies the wire protocol and behavioral semantics for syncing chat
data between the Ncy Android app and a desktop (or any other) client. It is the
authoritative reference: the Kotlin code in `app/src/main/java/com/ncy/notechaty/data/sync/`
and the Python reference server in `scripts/sync_test_server.py` both implement what
is described here.

> **Scope:**
> - Phase 1: one-way push, phone → desktop, on the same LAN, while the phone app is
>   foregrounded.
> - Phase 2a: same wire protocol, both sides may send `Event` and reply with `Ack`.
>   `Hello.sinceHlc` semantics are sharpened (see §6.2). Adds explicit loop-prevention
>   rules (§10.6).
> - Phase 2b: content-addressed media blob transfer over the same socket (see §10.8
>   and the `Blob*` frames in §6.7–6.9). Voice and image only; avatars/backgrounds/
>   inline rich-text images are deferred to a 2b polish step.
> - Authenticated LAN payload encryption is mandatory. Off-LAN relay and TLS metadata
>   privacy are still deferred; protected-chat encryption remains the at-rest layer.

## 1. Architecture

```
   ┌─────────────────────┐                ┌─────────────────────┐
   │  Phone (Android)    │                │  Desktop client     │
   │  WebSocket *client* │  ws://lan:port │  WebSocket *server* │
   │                     │ ─────────────▶ │                     │
   │  EventLog →  push   │   one-way      │  receives events,   │
   │  Bus     →  push    │   in phase 1   │  sends Acks         │
   └─────────────────────┘                └─────────────────────┘
```

- The **desktop is the server**. It binds a TCP port and announces a pairing payload
  out-of-band (QR code, pasted JSON, etc.).
- The **phone is the client**. It connects on demand (when the app is foregrounded
  and a paired peer exists), authenticates, streams events, and disconnects.
- All chat state lives in an **append-only event log** on the phone. The desktop
  receives events and reconstructs the same state via the same materialization rules.
  The desktop does **not** push events back in phase 1.

## 2. Pairing payload

Out-of-band exchange. Format (JSON, UTF-8):

```json
{
  "v": 1,
  "deviceId": "8e2bd4f3-2b13-4a4a-9b2c-a1b0d8e2f4a6",
  "name": "My Laptop",
  "host": "192.168.1.42",
  "port": 8765,
  "secret": "RnFTM05OdndNb1plUkRwY3FRRzRlZ0FUWlBNQ0JLN3M=",
  "availability": "foreground"
}
```

| Field      | Type    | Notes                                                    |
|------------|---------|----------------------------------------------------------|
| `v`        | int     | Pairing format version. Currently `1`. Reject mismatch.  |
| `deviceId` | string  | Stable UUID for this desktop. Reused across reconnects.  |
| `name`     | string  | Human-readable label shown to the user on the phone.     |
| `host`     | string  | Host the phone should dial (typically a LAN IPv4). Empty only when a valid `pearPublicKey` supplies the sole endpoint. |
| `port`     | int     | TCP port. Range 1–65535, or `0` with a Pear-only endpoint. |
| `secret`   | string  | Base64-encoded pre-shared key. **At least 32 bytes** of  |
|            |         | cryptographic randomness. Used to HMAC the auth nonce.   |
| `pearPublicKey` | string, optional | Exact 32-byte Pear/Noise public key as 64 lowercase hex characters. |
| `availability` | string, optional | `foreground` means the host is reachable only while its app is open. Absence means continuously available. |

A new pairing payload **must** generate a fresh `secret`. The phone treats the QR scan
as trust-on-first-use: it stores the (`deviceId`, `secret`) pair and uses them for
every subsequent connection. Removing a peer on the phone fully forgets the secret.
When the payload supplies only `pearPublicKey`, importing it initiates the same authenticated
sync handshake directly with that exact Pear identity. Ncy performs no Pear peer search or
pairing discovery. The host must not expose or persist chat data until the invitation secret
has authenticated successfully, and must retain the same one-device invitation rotation rules.

Pairing QR codes and JSON are bearer credentials. Implementations must label them as
sensitive, keep claimed credentials hidden by default, avoid logging them, exclude
them from OS/cloud backup, and minimize clipboard lifetime. Reference clients clear
an unchanged copied payload after 60 seconds and clear a successfully consumed paste
immediately; they first compare clipboard contents so newer user data is never erased.
Android blocks screenshots on its Sync devices surface. Secret-bearing Core state
files are created with owner-only `0600` mode on Unix; Windows deployments rely on
the per-user application-data ACL. Headless CLI output must warn operators before
rendering a credential, especially when stdout may be redirected to a file.

Device IDs, peer secrets, and sync watermarks are **installation-local state** and
must be excluded from operating-system backup and device-transfer facilities. Chat
events may be transferred, retaining their original `originDevice` stamps, but the
restored installation must generate a new local device ID and pair again.
Hosts should treat a displayed payload as a one-device invitation: once a new
`deviceId` authenticates with its secret, bind that credential to the device and
rotate the displayed invitation. A credential already bound to one device must not
authenticate a different device ID.
Replacing an **unclaimed invitation** invalidates only copies of that invitation;
it must not revoke independently claimed device credentials. Rotating a claimed
pairing slot replaces its secret, clears its bound device and sync watermark, keeps
the visible slot/label and all chat/event data, and requires that one device to pair
again. Removing a slot revokes it and removes the host-side record, still without
deleting chat data. Hosts should send authenticated `pairing_revoked` before closing
an active rotated/revoked session; headless implementations that manage credentials
offline must state exactly when a restart/reload makes the change effective.
Clients must treat `pairing_revoked`, `bad_auth`/`auth_failed`, or an HMAC mismatch as
a persistent trust failure: stop timed retries, show **Pairing revoked or changed —
remove and pair again**, and retain local chats until the user explicitly removes the
peer. Connection refused, timeouts, foreground-host absence, and blocked LAN traffic
remain temporary failures with capped retry.
After authentication, a host must reject `Hello.deviceId` with
`identity_collision` when it equals the host's own ID or another live authenticated
session already claims it. The live claim is released when that session ends. A
client must likewise reject `Welcome.deviceId` when it equals its local ID, and use
`identity_mismatch` when it differs from the stable ID in the pairing record. These
errors are persistent configuration/identity failures, so automatic timed retries
should pause until pairing or peer state changes.
`Hello.deviceName` and `Welcome.name` are mutable presentation metadata, not trust
identity. After the secret and expected stable device ID authenticate, receivers
should trim a nonblank name, cap it at 80 Unicode characters, and persist it as the
latest advertised device name. Unauthenticated discovery, pairing requests, failed
handshakes, and blank names must not rename a stored peer. User-assigned pairing-slot
or operator labels are separate metadata and must never be overwritten by a remote
name. A system-derived outbound label may follow the authenticated name only while it
equals the previously stored authenticated name; once it differs, it is an alias and
must remain unchanged. For legacy records with no prior authenticated-name field,
store the first authenticated name separately instead of guessing that the existing
label is safe to replace.
The stored `host` and `port` are the last **authenticated** endpoint rather than
identity. A peer may advertise `_ncy-sync._tcp` with its stable `deviceId`, but a
differing result is only an in-memory candidate. Clients try the authenticated
endpoint first, expire candidates after five minutes, and accept at most one changed
candidate per device every 30 seconds. A candidate is persisted only after its
encrypted handshake authenticates the pairing secret and expected `Welcome.deviceId`;
failure discards it. Discovery never replaces credentials or progress state.
Clients may store a local `disabled` flag alongside a paired peer. This flag is not
transmitted and does not revoke the pairing: it suppresses that client's outbound
connection/retry task while retaining the secret and sync watermarks. Missing legacy
values mean enabled. Network-aware clients should similarly suspend retry timers when
no usable LAN exists and resume enabled peers immediately when connectivity returns.
Clients do not scan address ranges. On a usable LAN, each enabled pairing may try its
single last-authenticated endpoint while mDNS passively resolves that stable device
ID. Public/guest Wi-Fi, enterprise VLANs, and access points with client isolation may
block peer TCP or multicast even when devices show the same SSID. Implementations
should explain this ambiguity, recommend a private LAN/hotspot, and retain capped
backoff rather than multiplying probes. A failed stale endpoint must never be treated
as proof that the saved peer or pairing is invalid.
Clients may persist local, secret-free diagnostics alongside a peer: last passive
discovery time, last successful authenticated endpoint/time, bounded last error, and
next retry time. These fields never affect identity or watermarks. A live authenticated
session proves direct LAN reachability; a connection failure does **not** prove client
isolation because an offline app, firewall, stale endpoint, and blocked LAN look alike.
User interfaces must say *possibly blocked* and must not add subnet scans or diagnostic
probes merely to classify that ambiguity. Repeated discovery writes should be throttled.
An active host must refresh both its pairing payload and mDNS registration when its
reachable LAN address changes. Android selects active Wi-Fi/Ethernet before hotspot
fallbacks and excludes cellular/VPN candidates. Core prefers physical Wi-Fi/Ethernet
over virtual and VPN adapters. IPv4 is preferred for compatibility, followed by IPv6
ULA/global addresses on IPv6-only LANs. Link-local addresses are not placed in pairing
payloads because their scope identifiers are machine-local. IPv6 literals are stored
unbracketed in `host`, then bracketed when constructing URLs; any zone `%` is encoded
as `%25`. Hosts bind the wildcard matching the selected address family. Android
withdraws advertising while no suitable address exists and restores it when one
appears. If its default port is occupied, it may choose an available fallback, but
every payload and service record must contain the port actually bound. Configured
Desktop/Server deployments instead report a bind failure so operator-selected ports
are never changed silently.

The mDNS TXT record includes `pairingMode`. A value of `request` means the host
supports the interactive `PairRequest` flow; `qr` means pairing must happen through
the host's QR/JSON payload. Clients must not offer `PairRequest` to a `qr` host.
For compatibility, an absent value is treated as `request`. A phone host also sets
`availability=foreground`. Paired clients retain lifecycle capability from their
credential-bearing pairing payload; unauthenticated mDNS TXT data may guide the
unpaired discovery UI but must not overwrite persisted paired-peer capabilities.
The DNS-SD instance name is multicast-visible metadata and must not contain the
configured device name, OS hostname, phone model, account name, or profile name.
Reference implementations advertise `Ncy <short installation-id>` where the ID is
random and installation-local. The TXT `deviceId` remains the full random stable ID
required for authenticated endpoint recovery; friendly names travel only in explicit
pairing data or direct peer handshakes. Discovery UI should identify the instance as
a privacy label so users do not mistake it for a lost friendly name.
Disabling host advertising does not invalidate credentials or prevent QR/JSON
pairing at the payload's explicit endpoint. It does remove automatic recovery when
that endpoint becomes stale: clients must not scan for a replacement. Every setting
or configuration surface that disables discoverability should state this tradeoff
and recommend stable DNS/IP or manual endpoint repair where mDNS is unsuitable.

## 3. Connection & transport

- URL: `ws://<host>:<port>/sync`
- Frames: WebSocket **text** frames. UTF-8 JSON. One JSON object per frame.
- Ncy Dev may, after explicit opt-in, carry those same complete UTF-8 frames over a
  length-prefixed Pear/Hyperswarm Noise stream. Its installation-local connection policy is
  one of **Local only**, **Pear only**, **Local preferred**, or **Pear preferred**. A preferred
  transport may fall back to the other transport only while establishing a connection; an
  established session is never silently moved between transports.
- The connection is long-lived: phone keeps it open while the app is foregrounded.
- Closing codes: `1000` (normal), `1001` (host going away), `1002` (protocol error).
  A foreground-only phone host closes with reason `phone_host_left_foreground` before
  stopping. Clients retry with capped backoff; resolving the trusted stable device ID
  over mDNS wakes that delay immediately, even when host and port are unchanged.

The outer WebSocket remains `ws://`, so network addresses, timing, frame sizes, and
the initial `Challenge`/`Hello` metadata remain observable. Message content, events,
acknowledgements, errors, and media are protected by mandatory PSK-derived
AES-256-GCM envelopes. Identity and transport negotiation are authenticated by the
pairing-secret HMAC, not by the network address or a Web PKI certificate.
The discovery-initiated flow is the only pre-PSK exception: `PairRequest` and
`PairChallenge` expose device labels and ephemeral P-256 public keys, while the
credential-bearing `PairAccept` payload is already protected by its ECDH-derived
AES-GCM key. Normal sync never falls back to plaintext application frames.

Pear is disabled by default and absent from the production flavor. **Local only** is the
default and guarantees that the Pear runtime, DHT discovery, and Pear connection attempts
are not started. Selecting any Pear-capable policy requires explicit privacy consent.
**Pear only** never attempts the saved LAN endpoint. **Local preferred** tries LAN first and
uses Pear when LAN is unavailable or the initial LAN attempt fails. **Pear preferred** does
the inverse. When enabled, public
DHT bootstrap/discovery infrastructure can observe a device's Pear public key, IP address,
timing, and connection attempts. Peers connect directly where NAT permits and may use
Holepunch relay infrastructure where required; Ncy does not send chat plaintext to a
storage service. Hyperswarm's Noise encryption protects the stream, and the normal Ncy
pairing-secret handshake plus AES-GCM envelopes remain mandatory inside it. Outgoing Pear
targets come only from authenticated pairing payloads; unsolicited incoming streams are
bounded and rejected by the Ncy authentication handshake.

**Roles are symmetric.** The protocol only defines a *client* (connects out,
signs the challenge, sends `Hello`) and a *server* (listens, issues `Challenge`,
replies `Welcome`). Nothing is phone- or desktop-specific. The desktop
implements **both** roles, so two desktops sync the same way a phone syncs to a
desktop: one runs the server, the other connects as a client by pasting the
server's pairing payload (§2). Multi-device deployments use a **hub** — one
device is the server, the rest are clients; pairwise links are real-time
bidirectional, and a client converges with another client's events via the
server's catch-up stream (§7) on (re)connect rather than via server-side relay.

## 4. Handshake & message order

```
T0  C ───── (TCP/WebSocket connect) ─────▶ S
T1  C ◀──── Challenge { nonce, transport }  S
T2  C ───── Hello { deviceId, clientNonce, transport, auth, sinceHlc } ─▶ S
T3  C ◀──── Secure(seq=0, Welcome { deviceId, name }) S

then, repeating in any order:
    C ───── Secure(seq=n, Event { event }) ─────▶ S
    C ◀──── Secure(seq=n, Ack { upToHlc })       S
```

- The server **must** send `Challenge` as the first frame.
- The client **must** require `transport="psk-aead-v1"` and reply with a transcript-
  authenticated `Hello`. A missing/different transport is an upgrade error.
- The server **must** send `Welcome` as encrypted sequence zero. Every subsequent
  application message is a `Secure` envelope; plaintext is a protocol error.
- Once authenticated, the client streams `Event` frames. The server sends `Ack` frames
  back as it durably receives each event.
- Either side may send `Error` and close at any time; after authenticated key
  derivation the error is itself carried inside `Secure`.

## 5. Authentication

Pre-shared-key challenge/response with HMAC-SHA256, followed by directional AEAD.

1. Server generates 16 random bytes, base64-encodes them: `nonce`.
2. Server sends `Challenge { nonce, transport:"psk-aead-v1" }`.
3. Client computes:
   ```
   secretBytes  = base64decode(secret)         // from pairing payload
   nonceBytes   = base64decode(nonce)
   clientNonce  = 32 fresh random bytes
   transcript   = "ncy-sync-auth-v2\0" || nonceBytes || clientNonce ||
                  UTF8(deviceId) || 0x00 || UTF8("psk-aead-v1")
   signature    = HMAC-SHA256(secretBytes, transcript)
   auth         = base64encode(signature)
   ```
4. Client sends `Hello { deviceId, clientNonce, transport, auth, sinceHlc }`.
5. Server recomputes the transcript HMAC and verifies
   `expected == base64decode(auth)` using a constant-time comparison.
6. On match, both sides derive 64 bytes with HKDF-SHA256 using `salt=nonceBytes`,
   `IKM=secretBytes`, and `info="ncy-sync-transport-v1\0" || clientNonce`.
   Bytes 0–31 are the client→server key; bytes 32–63 are the server→client key.
7. The server encrypts `Welcome` as its sequence-zero frame. On mismatch it sends
   `Error { code:"bad_auth", message:"..." }` and closes with code `1002`.

Each direction starts at sequence zero and accepts exactly the next sequence. The
AES-GCM nonce is four zero bytes followed by the big-endian `u64` sequence; AAD is
`"ncy-sync-secure-v1\0" || bigEndian(seq)`. Directional keys prevent nonce reuse
across directions. Replays, gaps, reordering, nested `Secure` frames, and plaintext
post-Hello frames are rejected.

### Bytestring conventions

- All `secret`, `nonce`, `clientNonce`, `auth`, and `Secure.data` values are
  **standard base64**
  (RFC 4648 §4, no URL-safe alphabet). The Kotlin client encodes with `Base64.NO_WRAP`;
  decoders should accept padded strings without trailing newlines.

## 6. Wire messages (catalog)

Every wire frame is a JSON object with a top-level `"type"` field. Unknown
fields **must** be ignored (forward-compatibility). Unknown `type` values **must**
be treated as a protocol error.

### 6.1 `Challenge` (server → client)

```json
{ "type": "Challenge", "nonce": "<base64-16>", "transport": "psk-aead-v1" }
```

### 6.2 `Hello` (client → server)

```json
{
  "type": "Hello",
  "deviceId": "<phone uuid>",
  "deviceName": "Pixel 9",
  "transport": "psk-aead-v1",
  "clientNonce": "<base64-32>",
  "auth": "<base64 transcript HMAC>",
  "sinceHlc": { "physicalMs": 1714000000000, "logical": 3, "nodeId": "<phone uuid>" }
}
```

`sinceHlc` is the **client's inbound watermark** — the highest HLC it has already
received and applied from this peer. The peer should resume sending events with HLC
strictly greater than this. **`null` means "send everything from the beginning."**
A fresh pairing always sends `null`. This client declaration is authoritative: a
server-side cached acknowledgement must never replace it or raise it. In particular,
`null` requests a complete recovery replay after reinstall, restore, or local reset.
`deviceName` is an optional friendly label. Receivers must accept it when absent for
compatibility with older clients.

> Note for phase 1 servers: an old phase-1 phone client sent its *outbound* watermark
> here (a holdover from the unidirectional days). Phase-1-only servers ignored the
> field entirely, so the meaning was unobserved. Phase 2 phones send the inbound
> watermark, which is the spec-correct value. Phase 2 servers should treat this as
> "send me everything past this HLC."

### 6.2a `Secure` (either direction after `Hello`)

```json
{ "type": "Secure", "seq": 0, "data": "<base64 AES-256-GCM ciphertext+tag>" }
```

`data` decrypts to one complete serialized inner wire message. `Welcome` is the
server's first inner message. Inner messages may not themselves be `Secure`.

### 6.3 `Welcome` (server → client, inside `Secure`)

```json
{ "type": "Welcome", "deviceId": "<desktop uuid>", "name": "My Laptop" }
```

### 6.4 `Event` (either direction, inside `Secure`)

```json
{ "type": "Event", "event": { /* see §7 */ } }
```

### 6.5 `Ack` (server → client)

```json
{
  "type": "Ack",
  "upToHlc": { "physicalMs": 1714000123456, "logical": 0, "nodeId": "<phone uuid>" }
}
```

The HLC inside `upToHlc` **must** be the HLC of the just-received `event`. The phone
treats this as "you have durably stored every event ≤ this HLC." It is acceptable
for the server to ack lazily (one ack per N events), but in phase 1 it acks one per
event for simplicity.

The phone refuses to regress its watermark — out-of-order acks for older HLCs are
ignored, never destructive.

### 6.6 `Error` (either side)

```json
{ "type": "Error", "code": "bad_auth", "message": "HMAC mismatch" }
```

`code` is a short snake_case string; `message` is human-readable. Suggested codes:
`bad_auth`, `bad_hello`, `bad_event`, `version_mismatch`, `internal`. The party
sending an `Error` **should** close the connection immediately afterward.

### 6.7 `BlobRequest` (either side)

```json
{ "type": "BlobRequest", "hash": "<sha256-hex>", "mediaType": "voice" | "image" | "file" }
```

Either side asks the other for the bytes of a content-addressed blob whose hash
appeared in a previously-applied event. `mediaType` is a hint for the responder's
file-extension convention (`voice` → `.m4a`, `image` → `.jpg`, opaque generic
`file` → `.bin`). The original filename and MIME type for a generic file live in
the event metadata, never in the storage path. Recipients **must**
respond with one or more `BlobChunk` frames totaling exactly the blob's bytes, or
a single `BlobMissing` if they don't have it.

### 6.8 `BlobChunk` (either side, in response to a `BlobRequest`)

```json
{
  "type": "BlobChunk",
  "hash": "<sha256-hex>",
  "mediaType": "voice" | "image" | "file",
  "offset": 0,
  "totalSize": 12345,
  "data": "<base64>",
  "isLast": false
}
```

- `offset` is the byte offset of *this* chunk in the assembled blob; chunks **must**
  be sent in strictly increasing order with no gaps.
- `totalSize` is the assembled blob's full byte length and is the same in every
  chunk for the same hash.
- `data` is base64 (RFC 4648 §4) of this chunk's raw bytes.
- `isLast` is `true` exactly once per request, on the final chunk.

The reference implementations chunk at 64 KiB pre-base64. Receivers **must** verify
that `SHA-256(assembled bytes)` matches `hash` before persisting; on mismatch the
fetch is dropped and the requester may retry by sending a fresh `BlobRequest`.

Resource ceilings are part of the protocol defense: text messages are limited to
1 MiB, decoded chunks to 64 KiB, assembled blobs to 100 MiB, and pending downloads
to eight per peer. A receiver must discard a transfer whose declared total, encoded
payload, decoded chunk, offset, or cumulative size exceeds these bounds.

Implementations stream outgoing blobs from disk and incoming chunks into a temporary
file; they do not retain the assembled blob in memory. The receiver verifies the
temporary file's size and SHA-256 before moving it into the content-addressed store.
Invalid or interrupted transfers must remove their temporary files, and stale files
left by process termination must be cleaned on a later launch.
An incoming transfer with no new chunk for 60 seconds is stalled and must be
discarded, closing and deleting its temporary file. Reference implementations
check for stalled transfers every 15 seconds.

### 6.9 `BlobMissing` (either side, in response to a `BlobRequest`)

```json
{ "type": "BlobMissing", "hash": "<sha256-hex>" }
```

Sender doesn't have the blob (e.g. the underlying file was deleted, or the chat was
tombstoned). Receiver should drop its pending fetch and fall back to a placeholder
in the UI.

## 7. ChatEvent

This is the unit of replication. It is **byte-for-byte identical** to one line of
the on-disk `events.ndjson` log, so a desktop client can serialize/deserialize the
same way for storage and transport.

```json
{
  "v": 1,
  "type": "MessageCreated",
  "eventId": "f7b9...",
  "hlc": { "physicalMs": 1714000000000, "logical": 0, "nodeId": "<phone uuid>" },
  "originDevice": "<phone uuid>",
  "chatId": "<chat uuid>",
  "profileId": "<profile uuid>",
  "profileName": "Work",
  "payload": { /* see §7.2 */ }
}
```

| Field          | Type    | Notes                                                           |
|----------------|---------|-----------------------------------------------------------------|
| `v`            | int     | Wire version. Currently `1`.                                    |
| `type`         | string  | Op discriminator. See §7.1.                                     |
| `eventId`      | string  | UUIDv4. Receivers **must** dedupe by this id (idempotency).     |
| `hlc`          | object  | Hybrid Logical Clock. See §8.                                   |
| `originDevice` | string  | UUID of the device that authored the event.                     |
| `chatId`       | string  | UUID of the chat the event belongs to.                          |
| `profileId`    | string  | Profile sync. Phone profile this chat belongs to. **Optional**; absent/empty ⇒ the legacy "default" bucket. See §7.3. |
| `profileName`  | string  | Profile sync. Display name for `profileId` at author time. Optional; advisory (the receiver may re-resolve). |

`MessageCreated` materialization stamps `message.authorDeviceId` from the surrounding
event's authenticated `originDevice` when the payload omits it. `MessageEdited` must
preserve the existing author and must never replace it with the editing device. New
payloads carry `authorDeviceId` so compaction can discard superseded content; when a
legacy latest edit lacks it, compaction retains the original creation as a minimal
author seed. This field identifies an installation, not a person.

`ChatMetadata.remoteMessagesOnLeft` is an optional, default-false presentation setting.
When enabled, clients may derive a stable theme-aware bubble tint from `authorDeviceId`;
the tint is presentation-only and does not add synchronized per-device color state.
When enabled, a client renders messages whose nonblank `authorDeviceId` differs from
its local installation ID as incoming/left-aligned bubbles. Unknown legacy authors
remain on the default right side. The setting affects presentation only and does not
change sync, ownership, editing permissions, or conflict resolution.
| `payload`      | object  | Op-specific. Shape depends on `type`. See §7.2.                 |

### 7.1 Op types

```
MessageCreated        - a new message was added to a chat
MessageEdited         - an existing message was rewritten (full new content)
MessageTombstoned     - a message was deleted; the id is dead forever
ChatMetadataChanged   - the chat's title/avatar/flags/etc. changed
ChatTombstoned        - the entire chat was deleted (graveyard form, phase 2+)
```

Any future `type` not in this list **must** be ignored by phase-1 receivers.

### 7.2 Payload shapes

#### `MessageCreated` / `MessageEdited`

```json
"payload": {
  "id": "<message uuid>",
  "chatId": "<chat uuid>",
  "content": [ { /* RichTextBlock — see §9.3 */ } ],
  "voiceFile": null,
  "voiceDurationMs": null,
  "createdAt": 1714000000000,
  "editedAt": null,
  "type": "TEXT",
  "isStarred": false,
  "imageFiles": [],
  "tags": [],
  "reminderAt": null,
  "checklistItems": []
}
```

The payload is the **complete** post-edit message — there is no per-field diff. This
keeps merging trivial: latest-write-wins.

#### `MessageTombstoned`

```json
"payload": { "messageId": "<message uuid>" }
```

A tombstone applies forever to that id. Any subsequent `MessageCreated` or
`MessageEdited` with the same id **must** be ignored by the materializer.
An implementation may move the tombstone from the active event log into a compact
exact ledger containing its event ID, HLC, origin, chat/profile identity, and message
ID. Reads and catch-up must reconstruct the original `MessageTombstoned` wire event;
inbound create/edit bodies covered by the ledger must be rejected before persistence
or media fetching.

#### `ChatMetadataChanged`

```json
"payload": { /* ChatMetadata — see §9.2 */ }
```

Whole-record LWW. The fields shown in §9.2 are authoritative.

#### `ChatTombstoned`

```json
"payload": { "chatId": "<chat uuid>" }
```

Phase 1 doesn't emit this — chats are physically deleted on the phone today.
A receiver should still handle it: yields a "no chat" materialized state.

### 7.3 Profile sync

The phone groups chats into **profiles** (separate spaces a user can switch
between). The desktop mirrors all of them so the chat list can be scoped by
profile, matching the phone.

- The phone stamps `profileId`/`profileName` on **every** event it authors,
  taken from the profile of the chat the event targets.
- The snapshot/catch-up walk reads **all** `profile_<id>` directories, not just
  the active one, so a paired peer receives every profile in one stream.
- Both fields are **optional and back-compat**. Events authored before profile
  sync (or chats that still live under the pre-migration root) carry an empty
  `profileId`; receivers MUST treat empty/absent as a single **"default"
  bucket**. A receiver backfilling a legacy event from the directory it
  physically lives in is allowed but never overrides a non-empty stamp.
- `profileName` is **advisory** — a human label captured at author time. A stable
  `profileId` identifies a profile within one installation generation, but restore
  and reinstall can produce several historical IDs for the same logical default
  profile. A receiver may re-resolve both fields locally and SHOULD tolerate an
  empty name.
- Android treats profile names as local labels: an incoming advisory rename never
  overwrites a locally chosen label. Core clients without a local profile registry
  display the name from the highest-HLC event stamp. Concurrent phone renames may
  therefore remain different labels while identity/content converge by `profileId`.
- A receiver must route a non-empty valid `profileId` independently of its currently
  selected profile, including downloaded media. An unknown ID creates a local profile
  using `profileName` only as its initial fallback label.
- Android receivers map historical IDs for equal-name **unprotected** profiles to one
  canonical local profile, preferring the active matching profile and otherwise the
  oldest. They merge duplicate chat logs by `eventId` before materialization and
  route media to the same canonical directory. PIN-protected profiles never merge by
  name and require an exact ID because equal labels do not imply equal PIN boundaries.
- When reading a canonical physical profile directory for catch-up, Android may
  restamp historical events with that directory's current ID/name. This prevents
  obsolete reinstall IDs from multiplying again without changing `eventId`, HLC,
  payload, or tombstone semantics.
- A chat's effective profile is the stamp on its **highest-HLC** event carrying
  a non-empty `profileId`. Desktop-authored events for an existing chat inherit
  that same profile so a round-trip to the phone groups correctly.
- Profiles themselves are **not** a synced entity — there is no profile
  create/rename/delete op. Profile identity travels only as these per-event
  stamps. Renaming a profile changes later advisory stamps for Core/fallback display,
  but does not overwrite another phone's locally chosen label.

## 8. Hybrid Logical Clock (HLC)

```
hlc = { "physicalMs": <int64>, "logical": <int32>, "nodeId": <string> }
```

| Field        | Notes                                                                |
|--------------|----------------------------------------------------------------------|
| `physicalMs` | Wall-clock millis when the event was created or causally received.   |
| `logical`    | Monotonic counter when multiple events share the same `physicalMs`.  |
| `nodeId`     | The originating device's UUID. Used as a tiebreaker.                 |

### 8.1 Comparison

```
(p1, l1, n1) < (p2, l2, n2)  iff
   p1 < p2
   OR (p1 == p2 AND l1 < l2)
   OR (p1 == p2 AND l1 == l2 AND n1 < n2)   // lexicographic
```

`compareTo` is total. There are no equal HLCs from different events — an event's
HLC is unique by construction.

### 8.2 `tick(previous, nowMs, nodeId)` — local clock advance

```
if nowMs > previous.physicalMs:
    return (nowMs, 0, nodeId)
else:
    return (previous.physicalMs, previous.logical + 1, nodeId)
```

Used when authoring a fresh event.

### 8.3 `receive(local, remote, nowMs, nodeId)` — clock fold on inbound event

```
maxPhysical = max(nowMs, local.physicalMs, remote.physicalMs)
if maxPhysical == local.physicalMs and maxPhysical == remote.physicalMs:
    logical = max(local.logical, remote.logical) + 1
elif maxPhysical == local.physicalMs:
    logical = local.logical + 1
elif maxPhysical == remote.physicalMs:
    logical = remote.logical + 1
else:
    logical = 0
return (maxPhysical, logical, nodeId)
```

Phase 1 desktop clients only receive events; they don't author any, so a strict
implementation of `receive` isn't required to pass the tests. But for phase 2
(bidirectional) the desktop clock **must** call `receive` on every inbound event
to keep its own clock causally ahead.

### 8.4 Remote clock validation

Every untrusted HLC position—event `hlc`, `Hello.sinceHlc`, and `Ack.upToHlc`—must
be validated before it affects a log, materialized state, HLC clock, or watermark.
`physicalMs` and `logical` must be non-negative, `nodeId` must be non-empty, and
`physicalMs` may be no more than five minutes ahead of the receiver's current wall
clock. The five-minute boundary is accepted; anything beyond it is rejected with
`Error.code = "clock_skew"` and the session closes without acknowledging the value.

Remote timestamps are not clamped: different receivers would clamp at different
instants and could materialize conflicting order. A sender retries after its system
clock is corrected. Persisted peer/pairing watermarks from older builds that violate
the rule are cleared so catch-up restarts safely; historical event logs are never
silently rewritten.

## 9. Embedded data models

These are the JSON shapes embedded inside `payload`. They mirror the on-disk
`chat.json` schema produced by the materializer.

### 9.1 `Message`

```typescript
{
  id: string,                    // uuid
  chatId: string,                // uuid
  content: RichTextBlock[],      // see §9.3
  voiceFile: string | null,      // filename inside chat dir's voice/
  voiceDurationMs: number | null,
  createdAt: number,             // epoch millis
  editedAt: number | null,       // epoch millis or null
  type: "TEXT" | "VOICE" | "IMAGE" | "CHECKLIST" | "FILE",
  isStarred: boolean,
  imageFiles: string[],          // filenames inside chat dir's images/
  fileAttachments: FileAttachment[],
  tags: string[],
  reminderAt: number | null,     // epoch millis or null
  checklistItems: ChecklistItem[]
}
```

#### `ChecklistItem`
```typescript
{ id: string, text: string, checked: boolean }
```

#### `FileAttachment`
```typescript
{
  file: string,                 // "files/<sha256>.bin"
  name: string,                 // original display name; not a storage path
  mimeType: string | null,
  sizeBytes: number | null
}
```

Notes for desktop implementers:

- `voiceFile`, `imageFiles`, and `fileAttachments[].file` are content-addressed
  references. Blob frames transfer their bytes after the event; clients render a
  downloading/missing placeholder until the verified blob is present.
- Generic files are opaque and require no preview support. Clients should show the
  original name and size and offer an OS open/save action. A filename is display
  metadata only and must never be joined directly to a storage directory.
- `editedAt` is informational; HLC ordering is authoritative for "which version wins."

### 9.2 `ChatMetadata`

```typescript
{
  id: string,                    // uuid (same as the chat's chatId)
  name: string,
  avatarFile: string | null,     // "avatar.jpg" by convention; in chat dir
  backgroundFile: string | null, // "background.jpg" by convention; in chat dir
  createdAt: number,             // epoch millis
  isPinned: boolean,
  bubbleColor: string | null,    // hex like "#FFAA00", or named, or null
  bubbleGradient: boolean,
  isArchived: boolean,
  tags: string[],
  isProtected: boolean,          // see §11
  hideName: boolean
}
```

### 9.3 `RichTextBlock` and `RichTextSpan`

```typescript
RichTextBlock = {
  type: "PARAGRAPH" | "HEADING1" | "HEADING2" | "HEADING3" | "BULLET_LIST" | "CODE_BLOCK",
  spans: RichTextSpan[]
}

RichTextSpan = {
  text: string,
  bold: boolean,
  italic: boolean,
  underline: boolean,
  strikethrough: boolean,
  color: string | null,        // hex string or named
  link: string | null,
  inlineImage: string | null   // filename in chat dir's images/
}
```

A renderer that doesn't care about formatting can flatten with
`block.spans.map(s => s.text).join("")` and join blocks with `"\n"`.

## 10. Sync semantics

### 10.1 Catchup + live forwarding (sender side)

When a phone session reaches `Welcome`, two things happen:

1. **Live subscription** to the in-process event bus is established **first**, so
   that any event appended during step 2 isn't dropped.
2. **Catchup snapshot** is read from disk: every event with HLC > `lastAckedHlc`,
   sorted by HLC across all chats in the active profile.

Both producers feed a single send path that:

- Skips events whose HLC ≤ `lastAckedHlc` (cheap dedupe).
- Otherwise sends them.

Duplicates may briefly be emitted at the catchup/live overlap; dedupe by `eventId`
on the receiver if you want to avoid double-applying.

### 10.2 Watermark advance (sender side)

`lastAckedHlc` advances **only on `Ack`**. A failed mid-stream send leaves the
watermark intact, so on the next reconnect the un-acked event is re-sent.

The phone refuses to regress the watermark: an `Ack` with an HLC ≤ the current
watermark is ignored.

### 10.3 Receiver responsibilities

A correct phase-1 desktop client **must**:

1. Authenticate every incoming connection via the HMAC handshake (§5).
2. After `Welcome`, accept a stream of `Event` frames.
3. For each `Event`, persist the event to its own log (or apply directly), then
   send `Ack { upToHlc: event.hlc }`.
4. Deduplicate by `eventId` if it has seen the event before. Re-acking is fine.
5. Tolerate connections that close abruptly (TCP reset, app backgrounded). On
   reconnect, the client resends from the last acked HLC.

A correct phase-1 desktop client **should**:

- Respect tombstones (§7.2). A `MessageTombstoned` for id `X` means: drop the
  current message `X` if any, and ignore any later `MessageCreated`/`MessageEdited`
bearing id `X`. Persist the tombstone — it's load-bearing across restarts.
Whole-chat tombstones may be moved from per-chat event logs into a compact exact
graveyard. Graveyard entries remain ordinary original `ChatTombstoned` events during
catch-up and backup; receivers must consult the graveyard before accepting any event
for that chat ID. Automatic age-based deletion is forbidden. A user may explicitly
purge graveyard entries only after a warning that stale devices/backups can then
resurrect those chats; this action is local, irreversible, and never implied by
compaction or peer acknowledgement.
Message tombstones may likewise live in a compact exact ledger outside the active
chat log. They remain ordinary original `MessageTombstoned` events during catch-up
and portable backup. The explicit dangerous purge covers both chat and message
deletion ledgers and must warn that either kind of deleted content may reappear.
- Apply events in HLC order when materializing the visible state, regardless of
  arrival order. The reference algorithm is in §10.4.
- Persist the `events.ndjson` log as the source of truth, and treat any rendered
  view as a derived cache. The phone does the same.

### 10.4 Materialization algorithm

Pure function `events → ChatData?`. Identical on both sides.

```
def materialize(events):
    sorted = events sorted by HLC
    metadata = None
    messages = ordered map<id, Message>()  # preserves insertion order
    tombstoned_messages = set<id>()
    chat_tombstoned = False

    for e in sorted:
        op = e.op
        if op.type == "ChatTombstoned":
            chat_tombstoned = True
        elif op.type == "ChatMetadataChanged":
            metadata = op.payload                       # whole-record LWW
        elif op.type == "MessageTombstoned":
            tombstoned_messages.add(op.payload.messageId)
            messages.remove(op.payload.messageId)
        elif op.type == "MessageCreated" or op.type == "MessageEdited":
            if op.payload.id not in tombstoned_messages:
                messages[op.payload.id] = op.payload    # last-write-wins by HLC

    if chat_tombstoned or metadata is None:
        return None                                     # chat does not exist
    return ChatData(chat=metadata, messages=messages.values sorted by createdAt)
```

Conflict rules summary:
- **Concurrent edits** to the same message: HLC wins, lexicographic `nodeId` breaks
  ties. The losing version remains in the append-only log until compaction but is not
  shown as a separate user-visible conflict.
- **Delete vs. edit**: tombstone always wins (regardless of HLC).
- **Chat metadata**: whole-record LWW — never per-field merge. A late stale
  `ChatMetadataChanged` simply doesn't get applied (its HLC is older).
- **Protection metadata**: whole-record LWW selects the effective protected state
  and salt. If the winner is protected, plaintext events and ciphertext that cannot
  be opened with the winning key render only as locked placeholders; content must
  never be exposed as a conflict fallback.

The reference UX resolves these automatically without a merge dialog. The Sync
devices screen explains the rules. A permanent delete cannot be undone by an edit
from an offline device; recreating content requires a new UUID.

### 10.5 Idempotency

The materialization rules above make repeated application of the same event a no-op:
the same `MessageCreated` overwrites with itself, a `MessageTombstoned` is set-add.
This is what allows the sender to re-send un-acked events safely.

A receiver **must** also dedupe by `eventId` at the *log* level — i.e. if an event
with the same `eventId` is already in the local log, drop it and still send an `Ack`.
The phone implements this in `FileChatRepository.applyExternalEvent`.

### 10.6 Loop prevention (phase 2a+)

Once any party can both send and receive, naïve forwarding produces echo loops.
The rules:

- **On the receiver:** if an inbound event's `originDevice` equals your own
  `deviceId`, still offer it to the idempotent event store. A normal echo is rejected
  by `eventId`; a reinstalled/restored device that lost its own historical event can
  recover it. Ack either outcome.
- **On the sender:** when forwarding events from a local event-bus to a peer,
  skip events whose `originDevice` equals that peer's `deviceId`. This prevents
  events received from peer A from being immediately pushed back to A. The exception
  is a complete recovery replay requested with `sinceHlc: null`: it includes every
  origin because the peer has explicitly declared empty inbound state.

The combination is sufficient for two parties. For a future multi-peer mesh
(phase 3+), the same rules give correct gossip: events from peer A reach peers
B, C, … exactly once, never returning to A.

### 10.7 Watermark persistence on the server (phase 2a+)

Once both sides can author events, *both* sides need to persist an outbound
watermark per peer so reconnects don't replay everything from scratch.

| Side    | Field name (reference impl)                | Meaning                                                        |
|---------|--------------------------------------------|----------------------------------------------------------------|
| Phone   | `SyncPeer.lastSentHlc{Ms,Logical,Node}`    | "Highest HLC the desktop has acked from me." Drives catchup.   |
| Phone   | `SyncPeer.lastReceivedHlc{Ms,Logical,Node}`| "Highest HLC I've applied from the desktop." Sent as `Hello.sinceHlc`. |
| Desktop | `Pairing.lastSentToPhoneHlc`               | Cached highest ack for diagnostics/optimization only; never overrides `Hello.sinceHlc`. |
| Desktop | (none persisted)                           | The phone announces its inbound watermark via `Hello.sinceHlc`; the server doesn't need to remember it. |

A server **must** advance `lastSentToPhoneHlc` only on `Ack` (never on send) and
**must not** regress it during ordinary sessions. On reconnect, however,
`Hello.sinceHlc` alone selects catch-up. If it is `null`, the cached server value is
cleared and every event is replayed, including events originally authored by that
client. This is what makes reinstall and stale-backup recovery complete.

The sender must subscribe to its live event source **before** reading the catch-up
snapshot. Events published during that read remain queued and are processed after
the sorted snapshot. Snapshot/live overlap is suppressed by `eventId`, not merely a
scalar HLC: a distinct event appended concurrently must still be sent even if its HLC
sorts below the snapshot maximum. Receivers must acknowledge
duplicate event IDs, but must not re-materialize them or start another media fetch.
This ordering turns every overlap into either one delivery or harmless wire-level
replay; it never creates a gap between snapshot and live forwarding.

The asymmetry — phone tracks two watermarks, server tracks one — is because the
phone is always the WebSocket *client* in phase 2. If a future deployment lets a
device act as both client and server, it will need both watermarks per peer.

#### Restore invariant

Sync watermarks are installation-local progress markers and must never be included
in a portable user-data backup. A manual restore must stop live sync, durably clear
all local sent and received watermarks before mutating event state, validate the
entire imported event stream, apply events idempotently, and then restore the prior
sync-service state on both success and failure. If the process stops after the
watermark reset, the next connection performs extra catch-up; it must never skip
events because of progress recorded for the pre-restore event set.

### 10.8 Media blob transfer (phase 2b)

Events carry only *references* to media on disk — never the bytes. Phase 2b moves
the bytes lazily, on demand, after an event has been applied.

**File-naming convention.** New media authored on a phase-2b device is stored under
its SHA-256 hash:

| Field on `Message`         | On-disk path on author          | Wire reference          |
|----------------------------|---------------------------------|-------------------------|
| `voiceFile`                | `<chat dir>/voice/<hash>.m4a`   | `"voice/<hash>.m4a"`    |
| `imageFiles[i]`            | `<chat dir>/images/<hash>.jpg`  | `"images/<hash>.jpg"`   |
| `RichTextSpan.inlineImage` | `<chat dir>/images/<hash>.jpg`  | `"images/<hash>.jpg"`   |
| `fileAttachments[i].file`  | `<chat dir>/files/<hash>.bin`   | `"files/<hash>.bin"`    |

The `<hash>` is lowercase hex, 64 chars. A receiver extracts the hash by stripping
the leading directory and the trailing extension, then validating the regex
`^[0-9a-f]{64}$`. Anything that doesn't match is treated as a *legacy* random-named
reference — the receiver **must not** issue a `BlobRequest` for it, since no peer
will have the bytes under a content-addressed name.

**Fetch flow** (receiver-driven):

1. Receiver applies an inbound `Event` (see §10.4).
2. Receiver scans the event's payload for media references whose filenames match
   the content-addressed pattern.
3. For each `(hash, mediaType)` pair the receiver doesn't already have on disk,
   it sends a `BlobRequest` and tracks a pending-fetch entry.
4. Sender (the device that authored the media) looks up its local file by hash,
   streams `BlobChunk` frames in order until `isLast: true`, OR sends a single
   `BlobMissing` if the file is gone.
5. Receiver concatenates chunks, verifies the assembled SHA-256 against the
   expected `hash`, and persists. On mismatch or out-of-order chunks, the fetch
   is dropped silently — the requester is free to retry on the next reconnect.

**Idempotency and dedup.** Blob storage on each device is content-addressed, so
two events referencing the same hash share storage and a re-applied event triggers
no new fetch. A receiver that already has the blob on disk **must not** request it
again.

**Concurrency.** The reference implementations send chunks serially per blob and
multiple blobs serially per connection — the WebSocket is full-duplex but ordering
across concurrent chunked transfers is the responder's prerogative. Receivers
**must** tolerate any interleaving of `Event` / `Ack` / `BlobChunk` frames as long
as chunks for the same hash arrive in offset-monotonic order.

**Out of scope for phase 2b first cut:** chat avatars, backgrounds, and inline
rich-text images. Their references and on-disk layout do not yet use the
content-addressed naming convention; they remain placeholder-rendered on the
receiver. The protocol above is forward-compatible — once the author also
content-addresses those files, no wire change is needed.

## 11. Protected chats

A chat is "protected" when `ChatMetadata.isProtected == true`. Through phases 1
and 2 those chats existed in the protocol but their content was not encrypted —
they synced in plaintext or, depending on the client, not at all. Phase 3
introduces per-chat encryption so protected content can sync without a peer ever
seeing the plaintext.

### 11.1 Threat model

This is **casual-attacker protection**, not enterprise-grade secrecy. The goal is:

- An attacker with the encrypted event log alone cannot read protected content
  without the user's PIN.
- A passive observer on the LAN (someone with a packet capture, no device access)
  also can't read it.

What it does **not** defend against: an attacker with both the encrypted log and
GPU access offline-brute-forcing a 4-digit PIN. The PBKDF2 iteration count is sized
to make casual attempts slow, not to be GPU-hard. Argon2id would be stronger and
remains an option for a future revision; it's deferred to avoid a JNI dep on
Android.

### 11.2 Key derivation

```
key = PBKDF2-HMAC-SHA256(
    password = pin (UTF-8 bytes),
    salt     = ChatMetadata.protectionSalt (base64-decoded, 16 random bytes),
    iter     = 250000,
    dkLen    = 32 bytes
)
```

- The salt is generated **once**, when the user first marks a chat protected, and
  travels in plaintext inside `ChatMetadata` (salts are not secrets — only the PIN
  is). Both devices that share the same PIN derive the same 32-byte key.
- Pairing-time key exchange is **not needed**. The salt arrives via
  `ChatMetadataChanged` like any other metadata; the desktop derives the key
  locally once the user types the PIN.

### 11.3 Encrypted payload envelope

When a protected chat's `MessageCreated` / `MessageEdited` event is written to
disk or sent over the wire, the *payload* field of the event becomes the sealed
envelope instead of the plaintext message:

```json
{
  "v": 1,
  "type": "MessageCreated",
  "eventId": "...",
  "hlc": {...},
  "originDevice": "...",
  "chatId": "...",
  "payload": { "v": 1, "enc": "AES-256-GCM", "nonce": "<base64-12>", "ct": "<base64>", "messageId": "<uuid>" }
}
```

- `nonce`: 12 random bytes per encryption, base64-encoded.
- `ct`: AES-256-GCM ciphertext **with the 128-bit tag appended** (the standard
  format both Kotlin's `javax.crypto.Cipher` and Rust's `aes-gcm` crate produce).
- `messageId`: stable message UUID in the cleartext envelope. It reveals no body,
  but lets a locked receiver apply LWW edits, compaction, and permanent tombstones.
  Optional only for backward compatibility with envelopes from older builds.
- `supersedes` *(optional, string)*: the `eventId` of a **plaintext** message
  event this sealed event replaces. Present only when a chat is protected *after*
  it already synced in plaintext to a peer — see §11.7. Absent for
  normally-authored encrypted messages. It rides in the cleartext envelope (not
  inside `ct`) so a peer with no key can still act on it; it is advisory and not
  covered by the GCM tag.
- The associated-data field is **not** used — the surrounding `eventId`/`hlc`/
  `chatId` are already authenticated by the higher layer's idempotency (a peer
  who mutates them would also have to forge a matching ciphertext, which the GCM
  tag prevents).

What's encrypted vs plaintext:

| Op                       | Encrypted?                                          |
|--------------------------|-----------------------------------------------------|
| `MessageCreated`         | Yes — payload is the sealed envelope.               |
| `MessageEdited`          | Yes — same.                                         |
| `MessageTombstoned`      | No — only carries a UUID, no content.               |
| `ChatMetadataChanged`    | No — peers must see `name`, `isPinned`, `tags`, etc. to render the chat list. |
| `ChatTombstoned`         | No — only carries the chat id.                      |

This is a deliberate UX trade-off: a paired peer sees that the chat exists, knows
its name, sees the timing of edits/deletes, but can't read message content.
Hiding the chat name itself would prevent unlocked listing — phase 3 doesn't.

### 11.4 Receiver behavior

A receiver — phone or desktop — handles protected events as follows:

1. **Inbound `Event`**: parse the envelope, store the event verbatim on disk
   (still in its encrypted form), `Ack` by HLC like any other event.
2. **Materialization**: if the chat is protected and no key is currently in
   memory, the message body materializes as a `[locked]` placeholder. The chat
   still appears in the chat list with its plaintext metadata.
3. **Unlock**: when the user enters the PIN, derive the key via §11.2, cache it
   in memory keyed by `chatId`. The materializer re-runs (or the open chat view
   re-renders) and now decrypts each payload via AES-GCM. A tag-failure (wrong
   PIN) leaves the message as `[locked]` and the cache is cleared.
4. **Outbound**: when authoring into a protected chat, encrypt under the cached
   key before appending to the event log. The plaintext message never leaves the
   authoring device.

A receiver **must not** discard events it can't decrypt — they may be readable
later, after the user unlocks the chat, or by another peer that has the PIN
cached.

### 11.5 New `ChatMetadata` field (phase 3+)

```typescript
ChatMetadata = {
  // ... fields from §9.2 ...
  protectionSalt: string | null   // base64 of 16 random bytes, or null
}
```

Generated the first time a chat is marked protected. Null for non-protected chats
and for pre-phase-3 protected chats (which won't sync until re-marked, since
without a salt there's no way to derive a key).

### 11.6 Backwards compatibility

A phase-1/phase-2 client that reads a phase-3-encrypted event will fail to parse
the payload as `Message`. Such clients should either:

- Treat the chat as unreadable (display a lock placeholder and refuse to render
  the message), or
- Drop the event entirely.

The on-disk format change is contained to `payload` — `eventId`, `hlc`,
`originDevice`, and `chatId` remain in their familiar shape, so ordering,
idempotency, and the watermark protocol still work end-to-end.

### 11.7 Protecting an already-synced chat (catch-up)

Marking a chat protected re-seals its messages **locally** in place (same
`eventId`/`hlc`). That alone does *not* reach a peer that already synced the
plaintext: peers dedupe by `eventId`, so the re-sealed event is dropped on
arrival and the plaintext copy lingers in the peer's at-rest log — defeating the
lock on that device.

To close that, the protecting device additionally **re-issues** each affected
message as a *fresh* event:

1. New random `eventId`, and an `hlc` ticked past every existing event in the
   chat (so it is genuinely new to every peer and wins LWW by `messageId`).
2. Payload is the sealed envelope (§11.3) with `supersedes` set to the **old
   plaintext event's `eventId`**.
3. The old plaintext events are dropped from the issuer's own log (the fresh
   sealed events carry the same `messageId` + content, higher `hlc`, so the
   materializer replaces them); the fresh events are published live and re-stream
   on catch-up like any other event.

A receiver, on applying an event whose envelope carries `supersedes: E`, **must
remove event `E` from its local at-rest log** (atomic rewrite, mirroring
`replaceAll`). Because `supersedes` is in the cleartext envelope this works
without the chat key — a locked peer still scrubs its plaintext. It is
conservative by construction: plaintext is only dropped once its encrypted
replacement has been durably stored, so no content is lost; a peer that never
receives the re-issue (e.g. an older build) simply keeps rendering the plaintext
as a `[locked]` placeholder per §11.4, exactly as before. Re-running the scrub is
idempotent (the superseded id is already gone).

## 12. Implementation checklist for a desktop client

Minimum viable receiver:

- [ ] Bind a TCP port; accept WebSocket connections at `/sync`.
- [ ] Generate a stable `deviceId` (UUID) once; reuse forever.
- [ ] Generate a fresh 32-byte `secret` for each pairing; render as base64 in the QR/JSON.
- [ ] On connect: send `Challenge` with 16 fresh random bytes (base64).
- [ ] Require `psk-aead-v1`; verify the v2 transcript HMAC in constant time.
- [ ] Derive directional HKDF keys and enforce exact per-direction sequence numbers.
- [ ] On success: send encrypted `Welcome { deviceId, name }` as sequence zero.
- [ ] Receive `Event` frames; for each:
  - Append to a local event log (one JSON object per line, NDJSON), keyed by `chatId`
    if you want the same on-disk layout as the phone, or one global log if you don't.
  - Send `Ack { upToHlc: event.hlc }`.
- [ ] Implement the materializer (§10.4). Drive UI from materialized snapshots, not
      from raw events.
- [ ] Persist tombstones across restarts.
- [ ] Tolerate duplicate `eventId` on input — they're idempotent.

Production-quality additions:

- [ ] Multi-peer support: keep a separate event log per phone `originDevice`, or a
      single log keyed by `(originDevice, eventId)` for dedup.
- [ ] Per-phone `lastReceivedHlc` so you can show "last sync" timestamps.
- [ ] Graceful UI for disconnects, auth failures, and version mismatches.

## 13. Reference implementations

- **Phone (sender):** `app/src/main/java/com/ncy/notechaty/data/sync/transport/SyncClient.kt`
  and `app/src/main/java/com/ncy/notechaty/data/sync/SyncManager.kt`.
- **Wire codec:** `app/src/main/java/com/ncy/notechaty/data/sync/transport/SyncWireCodec.kt`
  (Kotlin) and `scripts/sync_test_server.py` (Python).
- **Materializer:** `app/src/main/java/com/ncy/notechaty/data/sync/event/MaterializedView.kt`.
- **HLC:** `app/src/main/java/com/ncy/notechaty/data/sync/event/HlcTimestamp.kt`.

## 14. Worked example

A reconnect after a phase-2a session has been live for a while. Phone has chat-1
with two text messages it authored; desktop has authored a `MessageEdited` (a star
toggle) and a `MessageTombstoned` (a delete) since the phone last connected. Phone
also has a voice message whose blob the desktop has never fetched.

```
T0   Phone opens TCP/WS to ws://laptop:8765/sync
T1   S → C   {"type":"Challenge","nonce":"6t/3o6y8B7e9..."}
T2   C → S   {"type":"Hello",
              "deviceId":"phone-uuid","auth":"<base64(hmac)>",
              "sinceHlc":{"physicalMs":1714000050000,"logical":0,"nodeId":"laptop-uuid"}}
T3   S → C   {"type":"Welcome","deviceId":"laptop-uuid","name":"My Laptop"}

   ----- catchup phase, both directions -----
   Phone sends events with HLC > its lastSentHlc (none new in this run).
   Desktop sends events with HLC > Hello.sinceHlc; null requests every event.

T4   S → C   {"type":"Event","event":{
                "v":1,"type":"MessageEdited","eventId":"e3",
                "hlc":{"physicalMs":1714000060000,"logical":0,"nodeId":"laptop-uuid"},
                "originDevice":"laptop-uuid","chatId":"chat-1",
                "payload":{"id":"msg-1", ..., "isStarred":true, "editedAt":1714000060000}
             }}
T5   C → S   {"type":"Ack","upToHlc":{"physicalMs":1714000060000,"logical":0,"nodeId":"laptop-uuid"}}

T6   S → C   {"type":"Event","event":{
                "v":1,"type":"MessageTombstoned","eventId":"e4",
                "hlc":{"physicalMs":1714000070000,"logical":0,"nodeId":"laptop-uuid"},
                "originDevice":"laptop-uuid","chatId":"chat-1",
                "payload":{"messageId":"msg-2"}
             }}
T7   C → S   {"type":"Ack","upToHlc":{"physicalMs":1714000070000,"logical":0,"nodeId":"laptop-uuid"}}

   ----- live phase: phone sends a new voice message -----

T8   C → S   {"type":"Event","event":{
                "v":1,"type":"MessageCreated","eventId":"e5",
                "hlc":{"physicalMs":1714000080000,"logical":0,"nodeId":"phone-uuid"},
                "originDevice":"phone-uuid","chatId":"chat-1",
                "payload":{"id":"msg-3","chatId":"chat-1",
                           "voiceFile":"voice/<sha256>.m4a","voiceDurationMs":3500,
                           "createdAt":1714000080000,"type":"VOICE",
                           "content":[],"imageFiles":[],"tags":[],"checklistItems":[]}
             }}
T9   S → C   {"type":"Ack","upToHlc":{"physicalMs":1714000080000,"logical":0,"nodeId":"phone-uuid"}}

   ----- phase 2b: desktop doesn't have this voice blob yet, asks for it -----

T10  S → C   {"type":"BlobRequest","hash":"<sha256>","mediaType":"voice"}
T11  C → S   {"type":"BlobChunk","hash":"<sha256>","mediaType":"voice",
              "offset":0,"totalSize":117344,"data":"<base64-65536-bytes>","isLast":false}
T12  C → S   {"type":"BlobChunk","hash":"<sha256>","mediaType":"voice",
              "offset":65536,"totalSize":117344,"data":"<base64-51808-bytes>","isLast":true}
        (desktop verifies SHA-256 of assembled bytes == hash, persists to blobs/<hash>.m4a)

   ----- phone goes background -----

T13  Phone closes WebSocket with code 1000
```

On the next connect, the phone's `Hello.sinceHlc` becomes the HLC at T7 (`e4` was
the highest acked-from-desktop event), and the desktop's `lastSentToPhoneHlc` for
this pairing is the HLC at T9. Each side sends only what's truly new.

## 15. Versioning

The pairing format and wire envelope each carry their own `v`. **Phase 1 is `v: 1`
everywhere.** Future revisions may introduce new ops (e.g. `MediaBlob`) and new
top-level message types (e.g. `Heartbeat`); a receiver implementing this spec will
ignore them gracefully because of §6's unknown-field rule.

A `version_mismatch` error code is reserved for cases where the server actively
refuses an older client. Phase 1 servers should accept any `v: 1` client.

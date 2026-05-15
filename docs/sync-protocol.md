# Ncy Sync Protocol — v1 (Phases 1, 2a, and 2b)

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
> - Off-LAN relay, TLS (phase 3), and protected-chat encryption (phase 3) are still
>   deferred.

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
  "secret": "RnFTM05OdndNb1plUkRwY3FRRzRlZ0FUWlBNQ0JLN3M="
}
```

| Field      | Type    | Notes                                                    |
|------------|---------|----------------------------------------------------------|
| `v`        | int     | Pairing format version. Currently `1`. Reject mismatch.  |
| `deviceId` | string  | Stable UUID for this desktop. Reused across reconnects.  |
| `name`     | string  | Human-readable label shown to the user on the phone.     |
| `host`     | string  | Host the phone should dial (typically a LAN IPv4).       |
| `port`     | int     | TCP port. Range 1–65535.                                 |
| `secret`   | string  | Base64-encoded pre-shared key. **At least 32 bytes** of  |
|            |         | cryptographic randomness. Used to HMAC the auth nonce.   |

A new pairing payload **must** generate a fresh `secret`. The phone treats the QR scan
as trust-on-first-use: it stores the (`deviceId`, `secret`) pair and uses them for
every subsequent connection. Removing a peer on the phone fully forgets the secret.

## 3. Connection & transport

- URL: `ws://<host>:<port>/sync`
- Frames: WebSocket **text** frames. UTF-8 JSON. One JSON object per frame.
- The connection is long-lived: phone keeps it open while the app is foregrounded.
- Closing codes: `1000` (normal), `1002` (protocol error). The phone will not retry
  automatically in phase 1 — the user taps "Sync now" or backgrounds + foregrounds.

Phase 1 is plaintext over LAN. The phone manifest declares
`android:usesCleartextTraffic="true"`. Desktop clients should accept any client
hostname; identity is established via the HMAC handshake, not by network address.

## 4. Handshake & message order

```
T0  C ───── (TCP/WebSocket connect) ─────▶ S
T1  C ◀──── Challenge { nonce }            S
T2  C ───── Hello { deviceId, auth, sinceHlc } ─▶ S
T3  C ◀──── Welcome { deviceId, name }     S      (or: Error → close)

then, repeating in any order:
    C ───── Event { event } ─────▶ S
    C ◀──── Ack { upToHlc }       S
```

- The server **must** send `Challenge` as the first frame.
- The client **must** reply with `Hello` containing a valid HMAC.
- The server **must** send `Welcome` to confirm authentication, or `Error` and close.
- Once authenticated, the client streams `Event` frames. The server sends `Ack` frames
  back as it durably receives each event.
- Either side may send `Error` and close at any time.

## 5. Authentication

Pre-shared-key challenge/response with HMAC-SHA256.

1. Server generates 16 random bytes, base64-encodes them: `nonce`.
2. Server sends `Challenge { nonce }`.
3. Client computes:
   ```
   secretBytes  = base64decode(secret)         // from pairing payload
   nonceBytes   = base64decode(nonce)
   signature    = HMAC-SHA256(secretBytes, nonceBytes)
   auth         = base64encode(signature)
   ```
4. Client sends `Hello { deviceId, auth, sinceHlc }`.
5. Server recomputes `expected = HMAC-SHA256(secretBytes, nonceBytes)` and verifies
   `expected == base64decode(auth)` using a constant-time comparison.
6. On match, server sends `Welcome`. On mismatch, server sends
   `Error { code:"bad_auth", message:"..." }` and closes with code `1002`.

Each connection generates a fresh `nonce`. Replays are useless because the server
discards used nonces with the connection lifetime.

### Bytestring conventions

- All `secret`, `nonce`, and `auth` values on the wire are **standard base64**
  (RFC 4648 §4, no URL-safe alphabet). The Kotlin client encodes with `Base64.NO_WRAP`;
  decoders should accept padded strings without trailing newlines.

## 6. Wire messages (catalog)

Every wire frame is a JSON object with a top-level `"type"` field. Unknown
fields **must** be ignored (forward-compatibility). Unknown `type` values **must**
be treated as a protocol error.

### 6.1 `Challenge` (server → client)

```json
{ "type": "Challenge", "nonce": "<base64-16>" }
```

### 6.2 `Hello` (client → server)

```json
{
  "type": "Hello",
  "deviceId": "<phone uuid>",
  "auth": "<base64 of HMAC-SHA256(secret, base64decode(nonce))>",
  "sinceHlc": { "physicalMs": 1714000000000, "logical": 3, "nodeId": "<phone uuid>" }
}
```

`sinceHlc` is the **client's inbound watermark** — the highest HLC it has already
received and applied from this peer. The peer should resume sending events with HLC
strictly greater than this. **`null` means "send everything from the beginning."**
A fresh pairing always sends `null`.

> Note for phase 1 servers: an old phase-1 phone client sent its *outbound* watermark
> here (a holdover from the unidirectional days). Phase-1-only servers ignored the
> field entirely, so the meaning was unobserved. Phase 2 phones send the inbound
> watermark, which is the spec-correct value. Phase 2 servers should treat this as
> "send me everything past this HLC."

### 6.3 `Welcome` (server → client)

```json
{ "type": "Welcome", "deviceId": "<desktop uuid>", "name": "My Laptop" }
```

### 6.4 `Event` (client → server)

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
{ "type": "BlobRequest", "hash": "<sha256-hex>", "mediaType": "voice" | "image" }
```

Either side asks the other for the bytes of a content-addressed blob whose hash
appeared in a previously-applied event. `mediaType` is a hint for the responder's
file-extension convention (`voice` → `.m4a`, `image` → `.jpg`). Recipients **must**
respond with one or more `BlobChunk` frames totaling exactly the blob's bytes, or
a single `BlobMissing` if they don't have it.

### 6.8 `BlobChunk` (either side, in response to a `BlobRequest`)

```json
{
  "type": "BlobChunk",
  "hash": "<sha256-hex>",
  "mediaType": "voice" | "image",
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
  type: "TEXT" | "VOICE" | "IMAGE" | "CHECKLIST",
  isStarred: boolean,
  imageFiles: string[],          // filenames inside chat dir's images/
  tags: string[],
  reminderAt: number | null,     // epoch millis or null
  checklistItems: ChecklistItem[]
}
```

#### `ChecklistItem`
```typescript
{ id: string, text: string, checked: boolean }
```

Notes for desktop implementers:

- `voiceFile` and `imageFiles` reference files **on the phone**. Phase 1 does **not**
  transfer media — only the references. A phase-2 wire message will hand off blobs
  by content-hash. For now, expect missing media to render as placeholders.
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
  ties. Loser-side is silently overwritten.
- **Delete vs. edit**: tombstone always wins (regardless of HLC).
- **Chat metadata**: whole-record LWW — never per-field merge. A late stale
  `ChatMetadataChanged` simply doesn't get applied (its HLC is older).

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
  `deviceId`, you can ack it (so the peer's outbound watermark advances) but you
  **must not** apply it. It's your own event coming back at you.
- **On the sender:** when forwarding events from a local event-bus to a peer,
  skip events whose `originDevice` equals that peer's `deviceId`. This prevents
  events received from peer A from being immediately pushed back to A.

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
| Desktop | `Pairing.lastSentToPhoneHlc`               | "Highest HLC the phone has acked from me." Drives catchup.     |
| Desktop | (none persisted)                           | The phone announces its inbound watermark via `Hello.sinceHlc`; the server doesn't need to remember it. |

A server **must** advance `lastSentToPhoneHlc` only on `Ack` (never on send) and
**must not** regress it. On (re)connect it streams everything past
`max(Hello.sinceHlc, lastSentToPhoneHlc)` so a phone that wiped local state still
gets a fresh full replay if it sends `sinceHlc: null`.

The asymmetry — phone tracks two watermarks, server tracks one — is because the
phone is always the WebSocket *client* in phase 2. If a future deployment lets a
device act as both client and server, it will need both watermarks per peer.

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
  "payload": { "v": 1, "enc": "AES-256-GCM", "nonce": "<base64-12>", "ct": "<base64>" }
}
```

- `nonce`: 12 random bytes per encryption, base64-encoded.
- `ct`: AES-256-GCM ciphertext **with the 128-bit tag appended** (the standard
  format both Kotlin's `javax.crypto.Cipher` and Rust's `aes-gcm` crate produce).
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

## 12. Implementation checklist for a desktop client

Minimum viable receiver:

- [ ] Bind a TCP port; accept WebSocket connections at `/sync`.
- [ ] Generate a stable `deviceId` (UUID) once; reuse forever.
- [ ] Generate a fresh 32-byte `secret` for each pairing; render as base64 in the QR/JSON.
- [ ] On connect: send `Challenge` with 16 fresh random bytes (base64).
- [ ] Receive `Hello`; verify HMAC-SHA256(secret, nonceBytes) == base64decode(auth) in
      constant time. On failure: `Error{code:"bad_auth"}` and close.
- [ ] On success: send `Welcome { deviceId, name }`.
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
   Desktop sends events with HLC > max(Hello.sinceHlc, lastSentToPhoneHlc).

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

# Ncy sync — test scripts

## Building APKs without Android Studio

Double-click `build-apks.bat` in the repository root, or run it from a terminal:

```bat
build-apks.bat
build-apks.bat debug
build-apks.bat release
build-apks.bat all
```

The default `debug` mode produces installable production APKs for `arm64-v8a`,
`armeabi-v7a`, and `x86_64`, then opens `release\android\<version>\`. Release APKs
are optimized but remain unsigned until a release keystore is configured.

APK builds stay split by ABI so each release has one stable `arm64-v8a` file name.
`arm64-v8a` is the primary physical-device artifact, `armeabi-v7a` supports legacy
32-bit phones, and `x86_64` supports emulators.

Sync runs over the local network only. Internet sync through Pear/Bare Kit was removed
in 0.7.9; it made up about 90% of the APK and needed its own native build.

## `sync_test_server.py`

A minimal Python WebSocket server that implements the phase-1 sync wire protocol.
Use it to verify the protocol from a laptop on the same LAN as the phone, before
there is a real desktop client.

### Setup

```
pip install websockets qrcode[pil]
```

`qrcode` is optional, but recommended: with it the script also writes
`pairing-qr.png` next to itself, which is much easier to scan from the phone than
the ASCII version printed in the terminal. The `[pil]` extra installs Pillow,
which is required for the PNG output.

### Run

```
python sync_test_server.py
```

The server prints a pairing JSON blob and (if `qrcode` is installed) an ASCII QR.
Paste the JSON into Ncy → Settings → **Sync devices** → "Paste pairing JSON" → Pair.

While Ncy is in the foreground, every event you generate (new message, edit, delete,
chat metadata change) shows up in the terminal as a one-line summary.

### When the wrong IP is auto-selected

If you're on a VPN (Tailscale, ZeroTier, corporate VPN, …) the script may
advertise the VPN's `10.x.x.x` instead of your actual LAN. It now prints every
local IPv4 it finds, ranks `192.168.x.x` and `172.16-31.x.x` above `10.x.x.x`,
and flags `10.x.x.x` candidates as likely VPN. If the chosen one is wrong, just
re-run with the right one:

```
python sync_test_server.py --ip 192.168.1.42
```

### Flags

- `--host 0.0.0.0` — interface to bind on (default).
- `--port 8765` — TCP port.
- `--name "My Laptop"` — friendly name shown on the phone (defaults to the machine hostname).
- `--ip 192.168.1.42` — override the IP advertised in the pairing JSON.
- `--qr-png pairing-qr.png` — path to write the QR PNG. Pass `--qr-png ""` to skip.

### What it does and doesn't do

- ✅ Authenticates clients via HMAC-SHA256 of a server-issued nonce + the shared secret.
- ✅ Acks every event so the phone advances its `lastSentHlc` watermark.
- ❌ Does not persist anything. Disconnect, reconnect → the phone sends everything again.
- ❌ Does not send events back. Phase 1 is one-way: phone → desktop.

### Wire protocol (reference)

JSON over WebSocket text frames. Server is the listener; phone is the client.

```
S → C   {"type":"Challenge","nonce":"<base64-16>"}
C → S   {"type":"Hello","deviceId":"...","auth":"<HMAC-SHA256(secret, nonce) b64>","sinceHlc":{...}|null}
S → C   {"type":"Welcome","deviceId":"...","name":"..."}        on success
   or   {"type":"Error","code":"...","message":"..."}            on auth failure (then close)
C → S   {"type":"Event","event":{...ChatEvent...}}               (repeated)
S → C   {"type":"Ack","upToHlc":{"physicalMs":N,"logical":N,"nodeId":"..."}}
```

The `ChatEvent` payload format is documented inline in
`app/src/main/java/com/ncy/notechaty/data/sync/event/ChatEventCodec.kt`.

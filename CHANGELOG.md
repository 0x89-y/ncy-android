# Changelog

## Unreleased — 2026-07-11

### Sync hardening progress

- [x] Recover paired devices after LAN IP-address changes using stable device IDs
  and mDNS endpoint refresh.
- [x] Advertise pairing capabilities so clients never offer an unsupported
  discovery-pairing flow.
- [x] Use one-device Android host invitations with per-device credentials and
  individual revocation.
- [x] Exclude installation-local identity, credentials, and watermarks from
  Android cloud backup and device transfer.
- [x] Bound authenticated frames, blobs, chunks, and pending transfers.
- [x] Stream media without buffering complete files in memory.
- [x] Expire stalled media transfers after an inactivity timeout.
- [x] React safely to Android host network/interface and server-port changes.
- [ ] Add clock-skew, identity-collision, and backup-convergence safeguards.
- [ ] Harden pairing-secret handling and encrypt unprotected LAN traffic.

The complete prioritized backlog and completion criteria are maintained in
[`docs/sync-edge-cases.md`](docs/sync-edge-cases.md).

### Added

- Added direct phone-to-phone synchronization over the same local Wi-Fi network.
- Added an Android WebSocket sync server that uses Ncy's existing authentication,
  event synchronization, catch-up, acknowledgement, and media-transfer protocol.
- Added a **Share this phone** section to the Sync devices screen.
- Added generation and display of a scannable phone-pairing QR code.
- Added an option to copy the raw pairing JSON for manual transfer.
- Added a persistent **Allow phone-to-phone connections** toggle.
  Phone hosting is disabled by default and runs only while Ncy is open in the
  foreground.
- Added a persistent phone-host pairing identity and shared secret.
- Changed Android host pairing codes into one-device invitations that rotate
  automatically after being claimed.
- Added persisted per-device host credentials and individual **Revoke** controls.
- Added a visible migration label for older development pairings that shared the
  host credential, allowing them to be removed and paired again independently.
- Excluded device IDs, pairing credentials, peer watermarks, and Android host
  authorization records from cloud backup and device-to-device transfer, preventing
  restored phones from cloning sync identity while preserving user data backups.
- Added shared Android sync limits: 1 MiB text frames, 100 MiB blobs, 64 KiB decoded
  chunks, bounded base64 payloads, and at most eight pending downloads per peer.
- Media sync now reads outgoing files in bounded chunks and writes incoming data
  directly to temporary files before SHA-256 verification and atomic persistence.
- Interrupted and invalid Android transfers delete their temporary files; stale
  crash leftovers are cleaned after 24 hours.
- Incoming transfers now expire after 60 seconds without a chunk, with a 15-second
  cleanup sweep on Android client/server and both Core roles.
- Added an option to invalidate existing phone-host pairings and generate a new
  pairing code.
- Added mDNS advertisement for a phone hosting the sync server. Paired devices
  now rediscover it by stable device ID and automatically replace a stale IP
  address or port without re-pairing.
- Android phone hosting now monitors connectivity and link-property changes,
  refreshes its pairing QR/JSON endpoint, and re-registers mDNS without an app
  restart after Wi-Fi, Ethernet, interface, or DHCP-address changes.
- Phone hosting can start while no LAN is available and becomes shareable when
  a suitable network appears instead of remaining failed until the app restarts.
- If port 8765 is occupied, the Android host selects an available fallback port,
  includes it in pairing and mDNS data, and explains the fallback in the UI.
- Added mDNS pairing capabilities so discovered phone/headless hosts direct the
  user to QR scanning instead of offering an unsupported confirmation request.
- Added the Java-WebSocket dependency for hosting the Android sync endpoint.

### Changed

- Updated Sync devices instructions and scanner wording to cover both phone and
  desktop pairing.
- Wired the phone sync server into the activity lifecycle so it starts and stops
  with the foreground app when hosting is enabled.
- Pairing JSON can now be serialized directly from `PairingPayload`.
- Sync handshakes now include the connecting phone's model name when available.
- LAN discovery now runs with foreground sync instead of depending on the Sync
  devices screen being open.

### Fixed

- The server phone now records phones that authenticate against it and displays
  them in **Paired devices** with connected or last-connected status. Previously,
  only the phone that scanned the pairing code displayed the relationship.

### Battery behavior

- When phone hosting is disabled, no sync server thread or listening socket is
  started.
- When enabled, the server is available only while Ncy is foregrounded. Idle use
  is limited to the listening server and connected keep-alives; active event or
  media transfers use additional battery and network resources.

### Verification

- Forced a complete Kotlin recompilation after the network/interface changes.
- Successfully compiled the Kotlin sources.
- Successfully built the debug APK with `:app:assembleDebug`.
- Passed `git diff --check`.

# Sync edge cases and hardening backlog

Last reviewed: 2026-07-12

This document tracks reliability, security, privacy, and resource edge cases
across Ncy Android, `ncy-core`, `ncy-desktop`, and `ncy-server`.

Priority meanings:

- **P0** — credible data-loss or security problem; address before broad release.
- **P1** — likely broken workflow, serious reliability issue, or denial-of-service risk.
- **P2** — important hardening or degraded experience in less common environments.
- **P3** — optimization or polish after correctness work.

## P1 — tackle first

- [x] **Make phone discovery pairing capability-aware.** Android phone servers
  advertise `_ncy-sync._tcp`, so unpaired phones discover them, but the current
  **Pair** action sends the desktop-only ECDH confirmation flow. Either implement
  that flow on Android hosts or advertise a capability that makes the client
  direct the user to QR/JSON pairing instead. Completion: no discovered device
  offers a pairing action it cannot complete. Resolved with the mDNS
  `pairingMode` capability: interactive desktops advertise `request`, while
  Android and headless servers advertise `qr`.
- [x] **Use independent credentials per paired client.** Android pairing codes are
  now one-device invitations. The first authenticated device claims the credential,
  the displayed code rotates automatically, and each host-side record can be
  revoked independently. Records migrated from the development shared-secret
  format are visibly marked and can be removed/re-paired to receive an independent
  credential.
- [x] **Prevent cloned device identities after Android backup/restore.** Android
  cloud backup and device transfer now exclude the install device ID, outbound
  peer secrets/watermarks, host invitation identity, and hosted-client credentials.
  Chats, profiles, and ordinary settings remain transferable. This prevents new
  clones. Devices already cloned by an older build are now rejected explicitly and
  must regenerate identity by reinstalling/clearing app data, then pair again.
- [x] **Stream media with explicit limits.** Android and Core use bounded streaming
  disk I/O, clean interrupted/stale temporary files, and expire transfers after
  60 seconds without an incoming chunk.
- [x] **Bound all authenticated input.** Android and Core now enforce 1 MiB text
  frames, 100 MiB blobs, 64 KiB decoded chunks, bounded base64 payloads, and eight
  pending blob downloads per peer. Inconsistent offsets/totals and cumulative
  overflows discard the transfer. Streaming and transfer timeouts remain separate
  backlog items.

## P1 — network and lifecycle reliability

- [x] **React to network changes while hosting.** Android now monitors network
  and link-property callbacks, refreshes its pairing payload, and re-registers
  mDNS when the LAN endpoint changes. It may start without a LAN and recover when
  one appears. The callback lifecycle and endpoint refresh compile in the complete
  Android application; physical Wi-Fi/DHCP transition testing remains useful.
- [x] **Select the correct interface deterministically.** Android prefers active
  Wi-Fi/Ethernet, recognizes hotspot interfaces, rejects cellular/VPN candidates,
  ranks multiple addresses consistently, and supports IPv6 ULA/global addresses.
  Core ranks physical interfaces ahead of virtual/VPN adapters and falls back to
  IPv6 on IPv6-only hosts. Both clients bracket IPv6 URL literals and encode zone
  identifiers. Link-local addresses are deliberately not shared because a scope
  identifier is local to one machine; mDNS remains the safe way to resolve them.
- [x] **Handle Android phone-host server-port conflicts.** Android now probes port
  8765, selects an ephemeral fallback when it is occupied, publishes the actual
  port in pairing and mDNS data, and explains the fallback in the UI. Configured
  Desktop/Server ports continue to fail explicitly rather than silently overriding
  operator configuration.
- [x] **Make foreground-only availability explicit.** Android phone pairing/mDNS
  data declares `availability=foreground`; clients show an actionable **Open Ncy /
  enable phone sync** state for transport failures while keeping authentication and
  protocol errors distinct.
  Hosts send a background close reason, and trusted mDNS return wakes reconnects.
- [x] **Stop wasteful retries.** Android and Core persist a local per-peer disabled
  flag that defaults to enabled for old records. Disabled peers have no connection
  task but retain trust and watermarks. Android also closes sessions, cancels retry
  timers, and shows a waiting state while no Wi-Fi/Ethernet/hotspot LAN exists;
  enabled peers resume immediately when LAN connectivity returns.

## P2 — discovery behavior

- [ ] **Explain client-isolated and multicast-blocked networks.** Guest Wi-Fi,
  enterprise VLANs, and access-point isolation can block direct traffic or mDNS
  even when devices show the same SSID.
- [x] **Handle mDNS spoofing and endpoint flapping.** Android and Core retain the
  last authenticated endpoint as the first choice. A differing mDNS result becomes
  an in-memory candidate for five minutes, changes at most once per 30 seconds, and
  receives one attempt only after the trusted endpoint fails. Host/port promotion
  occurs only after encrypted authentication and identity verification; a failed
  candidate is discarded. Unauthenticated TXT capabilities are never persisted.
- [ ] **Define discoverability requirements consistently.** Desktop/server peers
  with advertising disabled cannot recover from a stale LAN address. Surface this
  tradeoff wherever discoverability is toggled.
- [ ] **Avoid unnecessary device-name exposure.** Document that mDNS service names
  are visible to the LAN and consider a privacy-preserving label or setting.

## P0/P1 — data convergence

- [x] **Canonicalize reinstall-generated profile generations.** Historical
  unprotected profiles with the same local label now resolve to one canonical
  Android profile. Existing directories are merged on startup by stable event ID,
  media is retained, and the combined log is materialized only after all copies are
  present so chat tombstones remain dominant. Imported tombstone-only logs are kept
  even when they have no `chat.json`. PIN-protected profiles require an exact ID and
  are never merged by label.

- [x] **Recover a reinstalled or stale-restored device completely.** The client's
  `Hello.sinceHlc` is authoritative; `null` clears cached server progress and requests
  every event. Full replay includes events originally authored by that same device,
  while idempotent receivers accept own-origin recovery without reapplying normal
  echoes. Android phone hosting queues live events until its snapshot completes and
  deduplicates overlap by event ID, preventing a high-HLC live event from hiding
  older chats or deletion tombstones.

- [x] **Detect unreasonable clock jumps.** Android and Core accept at most five
  minutes of remote future skew and reject negative/malformed HLC components across
  events, `Hello.sinceHlc`, and acknowledgements before logs or watermarks change.
  Clients report `clock_skew`; Desktop shows the peer error and Server logs it.
  Legacy poisoned peer watermarks are cleared on session/load. Historical event-log
  repair remains deliberately separate because silently rewriting HLC order would
  break deterministic convergence.
- [x] **Make backup/restore watermarks atomic with event state.** Portable backups
  exclude installation-local identity, trust, and progress state. Before importing
  events, Android and Desktop stop live sync and durably clear all local progress
  watermarks; imported clocks are validated before event mutation and the previous
  sync services are restored on success or failure. If import or process execution
  is interrupted, stable event IDs make the resulting full replay idempotent rather
  than allowing a stale watermark to suppress catch-up.
- [x] **Test identity collisions explicitly.** Android and Core hosts reject their
  own ID and atomically allow only one authenticated live session per remote ID.
  Clients verify that `Welcome.deviceId` is both distinct from their local identity
  and equal to the paired identity. Claimed one-device credentials cannot switch
  IDs, collision/mismatch errors are visible, and automatic retry pauses until peer
  state changes instead of repeatedly reconnecting. Core covers claim lifetime and
  all client identity outcomes with regression tests.
- [x] **Exercise catch-up/live overlap.** Core client and server roles subscribe to
  live broadcasts before reading the event snapshot, then use the session watermark
  to suppress snapshot/queue overlap and advance it after every live send. Android
  installs its live collector synchronously, queues events during snapshot reading,
  and drains them afterward. Receivers still acknowledge duplicate event IDs but no
  longer re-materialize them or start duplicate media fetches. Deterministic Core
  tests cover both a post-snapshot live event and an event present in both sources.
- [x] **Define conflict UX.** Simultaneous edits resolve automatically by total HLC
  order (`physicalMs`, `logical`, `nodeId`) with no conflict dialog/history; permanent
  message/chat tombstones dominate edits at any HLC. Chat metadata is whole-record
  LWW. Android profile labels remain device-local, while Core uses the highest-HLC
  advisory name stamp; stamped events/media route to the correct profile regardless
  of the profile currently open. Final protected metadata forces every unresolved
  plaintext body to a locked placeholder. New encrypted envelopes expose only the
  message UUID so locked edits compact by identity and tombstones still work. Android
  and Core include three-device ordering, delete, protection, and compaction tests.

## P1/P2 — pairing-secret handling

- [x] **Reduce pairing-secret exposure.** Android marks the entire Sync devices
  screen secure against screenshots, warns beside every QR/JSON entry point, clears
  unchanged copied invitations after 60 seconds, and clears successfully pasted JSON
  immediately. Desktop gives equivalent warnings and conditional timed/immediate
  clipboard cleanup; claimed QR data remains hidden by default. Android OS backup
  excludes every identity/credential store, normal logs contain no secrets, Android
  phone invitations rotate after one claim, Server CLI output carries an explicit
  handling warning, and Core creates secret-bearing JSON with Unix owner-only `0600`
  permissions (Windows relies on the user's inherited app-data ACL).
- [x] **Encrypt unprotected LAN traffic.** Every authenticated session now binds a
  mandatory `psk-aead-v1` negotiation into the pairing-secret HMAC, derives separate
  client→server and server→client AES-256-GCM keys with HKDF-SHA256, and encrypts
  `Welcome` plus every event, acknowledgement, error, and media frame. Directional
  sequence numbers reject replay, gaps, and reordering. Missing/legacy transport
  negotiation fails with `upgrade_required`; there is no plaintext downgrade.
- [ ] **Define secret rotation behavior.** Rotation should clearly identify which
  devices are revoked, what remains visible in the UI, and how reconnect errors
  differ from temporary network failures.

## P2/P3 — product behavior

- [ ] **Disambiguate duplicate device names.** Show a stable short fingerprint when
  multiple peers share the same model or configured name.
- [ ] **Refresh renamed peers.** Decide when an authenticated device-name change
  updates the stored label and whether user-assigned labels always win.
- [x] **Improve reconnect latency.** Android restarts unavailable/failed sessions
  and Core wakes a reconnecting peer task when a trusted stable device ID is
  resolved over mDNS, even if its endpoint is unchanged.
- [ ] **Expose useful diagnostics.** Show last discovery, last authenticated endpoint,
  retry state, protocol error, and whether direct LAN traffic is blocked.

## Suggested order

1. Capability-aware phone discovery pairing.
2. Per-client Android host credentials and revocation. *(completed)*
3. Android backup/identity protection. *(completed for future restores)*
4. Media streaming and protocol resource limits.
5. Network-change/interface/port handling.
6. Clock and backup convergence safeguards.
7. Transport encryption and invitation-secret hardening.
8. Remaining discovery, diagnostics, and UX work.

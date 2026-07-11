# Sync edge cases and hardening backlog

Last reviewed: 2026-07-11

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
  clones; devices already cloned by an older build still require identity-collision
  detection and manual re-pairing.
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
- [ ] **Handle mDNS spoofing and endpoint flapping.** A spoofed stable device ID
  cannot pass HMAC authentication, but it can redirect attempts. Rate-limit
  endpoint changes and prefer a recently authenticated address.
- [ ] **Define discoverability requirements consistently.** Desktop/server peers
  with advertising disabled cannot recover from a stale LAN address. Surface this
  tradeoff wherever discoverability is toggled.
- [ ] **Avoid unnecessary device-name exposure.** Document that mDNS service names
  are visible to the LAN and consider a privacy-preserving label or setting.

## P0/P1 — data convergence

- [x] **Detect unreasonable clock jumps.** Android and Core accept at most five
  minutes of remote future skew and reject negative/malformed HLC components across
  events, `Hello.sinceHlc`, and acknowledgements before logs or watermarks change.
  Clients report `clock_skew`; Desktop shows the peer error and Server logs it.
  Legacy poisoned peer watermarks are cleared on session/load. Historical event-log
  repair remains deliberately separate because silently rewriting HLC order would
  break deterministic convergence.
- [ ] **Make backup/restore watermarks atomic with event state.** Restoring peer
  watermarks without the corresponding events can suppress required catch-up.
- [ ] **Test identity collisions explicitly.** Two live devices with one device ID
  must be detected and rejected rather than silently triggering loop guards.
- [ ] **Exercise catch-up/live overlap.** Duplicate events are idempotent, but tests
  should verify no event loss and avoid duplicate media fetches during the handoff.
- [ ] **Define conflict UX.** Document and test concurrent edits, deletes, profile
  renames, and protected-chat changes across three or more devices.

## P1/P2 — pairing-secret handling

- [ ] **Reduce pairing-secret exposure.** Warn that QR images and JSON contain a
  credential; avoid unnecessary clipboard lifetime, screenshots, logs, and cloud
  backup. Consider short-lived invitation credentials exchanged for per-client
  secrets.
- [ ] **Encrypt unprotected LAN traffic.** HMAC authenticates peers but does not hide
  plaintext messages from network observers. Define a TLS/Noise migration with
  certificate/key pinning tied to the pairing identity.
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

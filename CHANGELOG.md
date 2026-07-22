# Changelog

## 0.7.1 — 2026-07-22

### Added

- Added interactive Markdown task markers to ordinary messages on Android and Desktop.
- Added a compact Desktop formatting toolbar for bold, italic, bullet-list, and checklist actions.
- Added one-click Desktop voice-transcription setup with progress reporting, verified Whisper downloads, and runtime-version and speech-model diagnostics.
- Added Fast, Balanced, and Accurate Desktop transcription models with multilingual and English-only variants.
- Cached downloaded Desktop transcription models for instant switching and added cleanup for inactive models.

### Changed

- New checklists are stored and synchronized as ordinary text messages, while existing structured
  checklist messages remain supported without automatic conversion.
- Combined Desktop attachments, voice recording, and the formatting-toolbar toggle into one composer
  add menu.
- Removed the dedicated Desktop checklist composer; task markers are now inserted directly into the
  normal message field.
- Consolidated Desktop profile, tag, and archived-chat filters into one menu.
- Replaced the dedicated Desktop search view with an inline chat-and-message search bar beside a compact filter button.
- Refreshed the Desktop chat sidebar with an icon-only segmented view selector and an edge-to-edge
  layout.
- Moved Desktop chat organization, appearance, privacy, and deletion controls from the overflow
  menu into a dedicated chat-settings panel.
- Added Desktop automatic backups with preset or custom intervals, selectable folders,
  configurable retention, manual runs, and last-run status.
- Moved deletion-record cleanup from Backup into advanced sync data maintenance.
- Reworked Android and Desktop changelogs into release-series views so long version histories no
  longer expand indefinitely inside Settings.
- Unified Android's home screen around bottom Chats, Starred, and Reminders navigation with an expandable, inline chat-and-message search mode.
- Unified archive visibility across Android and Desktop: archived chats are opt-in and appear in a separate section.
- Aligned Android and Desktop composers around one add menu, a multiline message field, and matching formatting actions.
- Standardized message actions across clients, including contextual Copy and reminder labels.
- Standardized chat settings around Organization, Appearance, Privacy, and Data sections.
- Reorganized Android Settings into General, Sync, Interface, Transcription, Backup, Changelog, and About pages.
- Standardized user-facing terms around Favorites, Reminders, Archived chats, Paired devices, Voice transcription, connection states, and last-synchronized times.
- Restricted Desktop LAN discovery advertisements to the selected network interface, preventing recurring errors from unrelated virtual adapters.
- Aligned empty states and retained native Android and Desktop interaction patterns.
- Reworked Desktop Settings into an edge-to-edge navigation rail and content pane instead of a
  centered floating panel.
- Moved Android's detailed device-sync guidance behind an information button.

### Fixed

- Desktop Pear connection choices now save immediately and remain selected when runtime startup
  fails, allowing a later retry without silently reverting to local-only mode.
- Desktop pairing codes in a Pear-capable mode now wait for a Pear public key instead of silently
  producing a local-only invitation.

## 0.7.0 — 2026-07-19

### Added

- Added optional Pear synchronization so explicitly paired devices can connect across
  different networks while retaining Ncy's authenticated, encrypted sync protocol.
- Added **Local only**, **Pear only**, **Local preferred**, and **Pear preferred** routing
  policies. Pear is off by default and requires explicit user consent.
- Added QR/JSON first-time pairing across Pear without exposing an unknown-device discovery
  mechanism.
- Added Pear support to Android, Desktop, and the headless server, including stable
  installation identities and Pear-aware pairing payloads.

### Changed

- Promoted Pear from the side-by-side Android development flavor into the normal Ncy app.
- Android releases now produce dedicated `arm64-v8a`, `armeabi-v7a`, and `x86_64`
  artifacts, with arm64 as the primary phone download and no legacy 32-bit x86/fat APK.
- Desktop Windows x64 installers now package their version-pinned Bare runtime, worker, and
  native addons with NSIS LZMA compression.
- Fixed large adjacent Pear IPC frames being discarded, which could interrupt an encrypted
  session with an unexpected sequence error.

### Privacy

- Local-only mode guarantees that the Pear runtime and its network activity do not start.
- Pear connects only identities obtained through Ncy pairing; it does not discover unknown
  Ncy devices.

## Unreleased — 2026-07-11

### Added

- Incoming bubbles now use a stable, theme-aware color derived from their originating
  device. Messages from the same device keep the same color without syncing additional
  configuration, while local and unknown legacy messages retain the normal bubble style.

### Sync hardening progress

- [x] Compacted active-chat message deletions into exact per-chat ledgers. Catch-up
  and backups reconstruct their original tombstone events, restart-safe lookup rejects
  stale bodies before storage/media fetch, and the warned purge now covers deleted
  chats and messages.

- Deferred selective per-profile/per-chat sync to the hardening backlog. Its design
  requires deletion graveyards to bypass content filters, complete catch-up after
  re-enabling, and no implicit deletion of copies already present on another device.

- Started the second-pass event-log growth project. Compaction is now regression-
  tested against a later-HLC stale restore: exact chat tombstones remain dominant
  and cannot be aged away merely because current peers acknowledged them.
- Added a staged compact-graveyard/history-generation design in
  `docs/sync-growth-hardening.md`; time-only tombstone deletion is explicitly forbidden.
- Deleted chats now move into one exact per-profile graveyard instead of retaining a
  directory and event log each. Graveyards sync normally, survive full backup/restore,
  use a cached ID index to reject stale history, and remain permanent by default.
- Added an advanced **Forget deletion records** action with an irreversible warning
  that stale devices or backups may restore deleted chats or messages afterward.

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
- [x] Select deterministic LAN endpoints and support IPv6-safe sync URLs.
- [x] Explain foreground-only phone hosts and reconnect immediately on return.
- [x] Stop wasteful retries with per-peer controls and LAN-aware suspension.
- [x] Reject future-skewed remote clocks before they affect events or watermarks.
- [x] Keep backup restoration and sync watermarks safely convergent.
- [x] Detect cloned and mismatched live device identities explicitly.
- [x] Close the catch-up/live-event handoff without duplicate media work.
- [x] Define and test deterministic three-device conflict behavior.
- [x] Reduce pairing-secret exposure in UI, clipboards, backups, and local files.
- [x] Encrypt authenticated LAN sync traffic without plaintext downgrade.
- [x] Prevent unauthenticated mDNS endpoint replacement and rapid endpoint flapping.
- [x] Make reinstall/full-restore catch-up replay all chats and deletion tombstones.
- [x] Consolidate reinstall-generated duplicate profiles and preserve tombstones
  during backup restoration.
- [x] Explain guest-network client isolation and multicast-blocked discovery.
- [x] Define the discoverability/address-recovery tradeoff consistently.
- [x] Remove phone models and friendly device names from mDNS service labels.
- [x] Define pairing-secret rotation, revocation, and retry behavior.
- [x] Disambiguate identically named paired devices with stable short ID fingerprints.
- [x] Refresh renamed peers only from authenticated handshakes while preserving user labels.
- [x] Expose passive, secret-free connection diagnostics without adding network probes.

The complete prioritized backlog and completion criteria are maintained in
[`docs/sync-edge-cases.md`](docs/sync-edge-cases.md).

### Added

- Added a per-chat **Other devices on the left** appearance setting. Messages created
  on another Ncy installation use an incoming left-aligned bubble, while messages
  created locally stay on the right. The setting is off by default and syncs with chat
  metadata.
- Messages now retain the creating installation's stable ID as presentation metadata.
  Remote edits, stars, reactions, transcription, encryption, and compaction preserve
  the original author; legacy compaction keeps a minimal creation seed when necessary.

- Added generic file attachments from Android's system document picker. Any file
  type up to the existing 100 MB blob limit can be sent without a preview; messages
  preserve the original filename, MIME type, and size, sync the bytes through the
  encrypted content-addressed blob protocol, and open through an installed Android
  app after download.
- Sync devices now explains that public/guest Wi-Fi can isolate clients or block
  multicast despite a matching SSID. Offline/error cards recommend a private LAN
  or hotspot and clarify that Ncy never scans the subnet.
- Defined discovery-off behavior across clients: explicit-address pairing continues,
  but a stale host address requires mDNS to recover automatically or manual re-pairing.
- Android phone hosting now advertises a privacy-preserving label derived from its
  random installation ID instead of broadcasting the phone model. Discovered-device
  cards explain that the friendly name appears after pairing.
- Android now distinguishes replacing an unused one-device invitation from revoking
  a claimed device. Revocation names the affected peer, preserves chats, sends an
  authenticated `pairing_revoked`, and makes rejected-credential clients stop retrying
  with a clear remove-and-pair-again action.
- Paired-device cards now append an eight-character stable device-ID fingerprint
  when two incoming or outgoing peers have the same name, ignoring case and outer
  whitespace. Unique names stay uncluttered, and secrets/IP addresses are not used.
- Android now persists changed peer names received in an encrypted authenticated
  handshake. Blank or failed-handshake names are ignored, names are capped at 80
  characters, and host-side claimed device names follow the same rule.
- Paired-device cards now offer expandable diagnostics showing last passive discovery,
  last authenticated endpoint/time, retry scheduling, and the bounded last error.
  LAN failures are correctly labelled as possibly blocked rather than definitive.

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
- Added deterministic Android LAN-address selection: active Wi-Fi and Ethernet
  are preferred, hotspot interfaces are supported as a fallback, cellular/VPN
  endpoints are avoided, and IPv6 ULA/global addresses work on IPv6-only LANs.
- Added IPv6-safe WebSocket and endpoint formatting, including brackets around
  literals and percent-encoded scope identifiers for manually supplied hosts.
- Android restarts the phone-host listener when a network transition changes the
  required IPv4/IPv6 socket family.
- Android phone pairing data and mDNS now declare foreground-only availability.
  Transport failures show **Open Ncy / enable phone sync** instead of a generic error;
  authentication and protocol failures remain visibly distinct.
- Phone hosts send an explicit WebSocket close reason before leaving the foreground,
  and a trusted mDNS reappearance resets reconnect delay even at the same endpoint.
- Added a persisted **Automatic sync** switch to each paired outbound device. Pausing
  stops its socket and retry task without forgetting credentials or watermarks;
  resuming triggers an immediate connection.
- Android now suspends peer sockets and retry timers while no suitable Wi-Fi,
  Ethernet, or hotspot interface exists, shows **Waiting for Wi-Fi or Ethernet**,
  and reconnects enabled peers immediately when LAN connectivity returns.
- Added a shared five-minute future-clock tolerance for remote events, catch-up
  watermarks, and acknowledgements. Invalid HLCs are rejected before persistence,
  materialization, acknowledgement, or watermark advancement.
- Android surfaces clock mismatch as a real sync error and clears legacy future-
  skewed peer watermarks on upgrade so a corrected clock can catch up again.
- Manual backup import now pauses live client and phone-host sync, durably clears
  installation-local progress watermarks before changing event state, validates
  all imported event clocks, and resumes the prior sync services even after an
  import error. A crash can therefore cause harmless replay, not missed catch-up.
- Portable Android backups continue to contain user data only; device identity,
  trust credentials, host authorizations, and sync watermarks are never restored.
- Android client and phone-host roles now reject a remote using the local device
  ID, a second simultaneous connection using an already-active ID, and a server
  whose authenticated identity differs from the paired ID. These failures remain
  visible and suspend automatic retries until peer state changes.
- Android outbound sync now subscribes synchronously and queues live local events
  before reading its catch-up snapshot, so an event authored during the handoff is
  delivered afterward. Duplicate events are acknowledged without re-materializing
  state or launching another media download.
- Added explicit conflict guidance to Sync devices: simultaneous message edits use
  total HLC order, tombstones remain permanent, protected conflicts never expose
  unresolved plaintext, and profile names remain local labels on each phone.
- Incoming Android events and downloaded media now route by their stamped profile ID
  instead of whichever profile is currently open; unstamped legacy events retain the
  existing active/default-profile behavior.
- Protected message envelopes now expose only the stable message UUID alongside the
  ciphertext. Locked peers can collapse concurrent edits and apply deletes without
  learning message content; legacy envelopes remain compatible.
- Android now applies the same safety rule as Core: if final chat metadata is protected,
  older or concurrent plaintext bodies materialize only as locked placeholders.
- Android's Sync devices screen is now screenshot-protected. QR/JSON views warn that
  they contain a credential, unchanged copied invitations clear after 60 seconds, and
  successfully pasted pairing JSON is removed from the clipboard immediately.
- Added mDNS pairing capabilities so discovered phone/headless hosts direct the
  user to QR scanning instead of offering an unsupported confirmation request.
- Added the Java-WebSocket dependency for hosting the Android sync endpoint.
- Added mandatory `psk-aead-v1` transport encryption. The pairing secret now
  authenticates both handshake nonces, device identity, and transport version;
  HKDF-SHA256 derives directional AES-256-GCM keys for every session.
- Welcome, events, acknowledgements, errors, and media frames now travel only inside
  strictly sequenced authenticated envelopes. Replay, gaps, reordering, tampering,
  nested envelopes, and post-handshake plaintext are rejected.
- Added a deterministic Core/Android encryption vector and Android round-trip/replay
  regression tests. Older plaintext builds fail with an explicit upgrade error.
- Hardened paired-peer rediscovery against mDNS spoofing. Android keeps changed
  endpoints as expiring, rate-limited candidates, tries the last authenticated
  address first, and persists a replacement only after the encrypted handshake
  confirms both the pairing secret and expected device ID. Failed candidates are
  discarded, and mDNS TXT capabilities no longer overwrite paired state.
- Fixed reinstall recovery so `sinceHlc: null` cannot be overridden by a stale
  server watermark. Full replay now includes the device's own historical events,
  allowing restored chats and tombstones to reconstruct correctly.
- Android phone hosting now queues live events until its initial snapshot finishes
  and deduplicates snapshot/live overlap by event ID, preventing a newer live HLC
  from suppressing older chats during the next reconnect.
- Android now treats historical UUIDs for equal-name, unprotected profiles as
  generations of one local profile. Existing duplicate profile directories are
  merged automatically at startup, event IDs are deduplicated, media is retained,
  and empty obsolete profile records are removed. PIN-protected profiles are never
  merged by name.
- Incoming events and their media are canonicalized to the same local profile before
  persistence, preventing a chat or its tombstone from being split across profile
  directories. Outgoing snapshots restamp events with their canonical physical
  profile so obsolete IDs do not spread again.

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
- Backup import no longer deletes an event directory when its materialized view is
  empty. Tombstone-only histories remain available to suppress stale chats restored
  from an older backup or replayed by another device.
- Fixed a crash when opening chats containing legacy, restored, or desktop-authored
  messages whose newer collection fields (notably `reactions`) were absent or JSON
  `null`. Chat snapshots, sync events, and decrypted messages now normalize missing
  collections and enum defaults before repository or UI code sees them.

### Battery behavior

- When phone hosting is disabled, no sync server thread or listening socket is
  started.
- When enabled, the server is available only while Ncy is foregrounded. Idle use
  is limited to the listening server and connected keep-alives; active event or
  media transfers use additional battery and network resources.

### Verification

- Forced a complete Kotlin recompilation after the network/interface changes.
- Passed Android endpoint-formatting unit tests.
- Passed compatibility tests for pairing payloads with and without availability.
- Verified legacy peer records default to automatic sync enabled.
- Added HLC boundary and malformed-component tests.
- Added a shared watermark-reset regression test that preserves pairing trust.
- Added Core regression tests for cloned, mismatched, and valid peer identities.
- Added deterministic Core tests for events created during snapshot handoff and
  events present in both the snapshot and queued live stream.
- Passed six Android conflict/envelope tests covering three-device edit ordering,
  delete dominance, protected plaintext safety, locked edit/delete convergence,
  trusted in-memory decryption, wire compatibility, and compaction.
- Passed all 12 Android unit tests after a forced rebuild.
- Passed all 14 Android unit tests, including Core-compatible transport encryption.
- Passed all 17 Android unit tests, including endpoint flapping, expiry, and
  refresh-starvation coverage.
- Passed all 21 Android unit tests, including authoritative empty-watermark,
  own-origin recovery, stale tombstone, and snapshot/live overlap coverage.
- Passed all 24 Android unit tests, including canonical unprotected-profile routing
  and protected-profile isolation across differing historical IDs.
- Passed all 26 Android unit tests, including legacy chat JSON and sync-event
  compatibility with null or absent message fields.
- Passed all 27 Android unit tests, including portable generic-file message
  metadata, then rebuilt the debug APK.
- Passed all 28 Android unit tests, including deterministic privacy-preserving
  mDNS labels, then rebuilt the debug APK.
- Passed all 30 Android unit tests, including permanent revoked-credential handling
  versus retryable network failures, then rebuilt the debug APK.
- Passed all 32 Android unit tests, including case-insensitive duplicate-name
  detection and stable device-ID fingerprint formatting, then rebuilt the debug APK.
- Passed all 34 Android unit tests, including authenticated rename normalization,
  blank-name preservation, and length bounds, then rebuilt the debug APK.
- Passed all 35 Android unit tests, including stale-history resurrection protection
  after chat-tombstone compaction, then rebuilt the debug APK.
- Passed all 36 Android unit tests, including exact graveyard persistence, deduplication,
  lookup, and explicit purge behavior, then rebuilt the debug APK.
- Passed all 37 Android unit tests, including original-author preservation across a
  different device's edit and event-log compaction, then rebuilt the debug APK.
- Passed all 38 Android unit tests, including active-message tombstone compaction,
  exact wire reconstruction, restart-safe stale-history rejection, and explicit purge;
  then rebuilt the debug APK.
- Rebuilt Android after the screenshot and clipboard hardening changes.
- Successfully compiled the Kotlin sources.
- Successfully built the debug APK with `:app:assembleDebug`.
- Passed `git diff --check`.

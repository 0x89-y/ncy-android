# Changelog

## 0.7.8 — 2026-10-21

### Changed

- Settings, its pages, chat settings, sync and licenses now run edge to edge like the chat
  list, starting directly below the top bar.

### Fixed

- Fixed the lines between rows being hard to see in the dark theme at everyday screen
  brightness.

## 0.7.7 — 2026-10-21

### Added

- Added more reminder choices: in 15 minutes, in 1 or 3 hours, this evening, tomorrow
  morning and next week, each showing the time it rings. **Pick date & time** sets any
  moment with a calendar and a clock.
- Added **Remove reminder**. A message that already has a reminder shows when it is set
  for, and removing it removes it on every paired device.
- Archived chats now open as their own list from the filter menu, next to Favorites,
  instead of a section under the chats.

### Changed

- Favorites moved into the filter menu. The Reminders row above the chats shows only
  while a reminder is upcoming.
- Reminder alarms now follow the reminders stored in the chats: reminders set on another
  device ring on this phone too, and every alarm is set again after a restart. Tapping a
  reminder notification opens its chat. Reminders in protected chats or profiles only say
  "Reminder" in the notification.
- The chat list, archived chats and profiles now run edge to edge, and profile avatars
  match the size of chat avatars.
- Profile tags under All chats use each profile's own colour.
- A chat created from All chats picks its profile from a dropdown rather than a row of
  chips.
- Bottom sheets open fully instead of half way.
- Removed descriptions that only repeated their setting's name.

### Fixed

- Fixed Favorites and Reminders only updating after Ncy restarted.
- Fixed removed reminders, and reminders of deleted messages, chats or profiles, still
  ringing.
- Fixed the "Editing message" banner and other accent-tinted labels being hard to read.

## 0.7.6 — 2026-10-21

### Added

- Added **All chats**, which lists the chats of every profile together. Each chat shows a
  tag naming its profile, and search, Favorites and Reminders cover every profile too.
  A protected profile's chats appear only while it is unlocked. A chat created from All
  chats asks which profile it belongs in, defaulting to the one used last.
- Added a profile switcher sheet listing All chats and each profile with its chat count
  and lock, replacing the plain profile menu.

### Changed

- Ncy now reopens in the profile, or in All chats, that it was left in.
- Favorites and Reminders moved from the bottom bar to rows above the chats, each with a
  count. They appear only when they hold something, and the bottom bar is gone.
- Deleting a profile now deletes its chats on every paired device, so they no longer
  linger on the desktop. A deleted profile can no longer be brought back by a leftover
  folder or by a synced note.

### Fixed

- Fixed release builds forgetting the profile list on every launch. Renamed profiles got
  their old names back, profile PINs and icons were dropped, an extra empty Personal
  profile appeared, and Ncy did not reopen in the last profile. Renames, PINs and icons
  lost this way have to be set once more after updating.

## 0.7.5 — 2026-10-21

### Added

- Added a voice message button in place of Send while the message field is empty.
- Added a menu to the chat header with Chat settings and Select messages. Tapping the chat
  name still opens chat settings too.
- Added hints to empty chats, Favorites and Reminders saying what belongs there.
- Added a Privacy & security settings page for the PIN, biometrics, app lock and re-lock
  period.
- Added a Wallpaper accent that follows the system's Material You colours.

### Changed

- The chat list now shows the profile name next to its avatar, and the bottom bar shows
  icons only. The "Chats" heading appears only when archived chats are shown.
- New chats open as soon as they are created, and the name field takes focus with the
  keyboard's Done key creating the chat.
- Formatting moved out of the + menu into the message field as an Aa button. The + menu
  now holds Camera, Gallery and File.
- The message menu shows a preview of the message it is for, puts Reply, Copy, Edit and
  Select in one row at the top, and sets Delete apart at the bottom.
- The Dynamic accent, which was a fixed violet, is now named Violet. Installs that had it
  selected keep the same colour.
- The settings list is grouped into cards, every choice shows a check when selected, and
  the back gesture returns from a settings page to the list.
- Sync settings use plainer wording: pairing text instead of JSON, Unpair instead of
  Revoke, and connection modes named Same network only, Same network first, Internet first
  and Internet only.
- Date headers follow the device's locale, and the app's text is now kept in string
  resources so it can be translated.

### Fixed

- Fixed white text being hard to read on light accent colours in your own messages; the
  bubble is darkened just enough to keep it legible.
- Fixed links in messages always using one fixed blue; they are now underlined in the
  bubble's text colour.

## 0.7.4 — 2026-10-20

### Added

- Added a Codes match step to pairing with a nearby desktop. Nothing is paired until both
  safety codes are confirmed, and the code can no longer be forced to match by a device
  relaying between the two.
- Images, voice notes and files in protected chats are now encrypted with the chat's PIN,
  on this phone and on paired devices. Existing attachments are encrypted the next time
  the chat is unlocked.
- The app lock and protected chats now lock again once Ncy has been in the background
  longer than the re-lock period (at least a minute).
- Wrong PIN entries now trigger a growing wait after the fourth attempt, up to 30 minutes.

### Security

- Paired devices can no longer request files from outside a chat's media, and identifiers
  received from peers or backups are checked before they become folder names.
- Phone sync hosting now limits connections that have not authenticated yet and the size
  of incoming messages.
- A device waiting for pairing approval can no longer receive messages before it is
  approved.

### Changed

- Reminders without exact-alarm permission now fire within minutes instead of possibly
  hours late.

### Fixed

- Fixed Change PIN and Remove PIN leaving every protected chat unreadable. Both now ask
  for the current PIN and re-encrypt or decrypt the chats.
- Fixed notes written on a device while it was offline never reaching devices that had
  synced in the meantime. Requires the updated desktop app too.
- Fixed messages that could be skipped for good when sync was interrupted partway through.
- Fixed a refusal that arrives before the connection is encrypted stopping sync for good;
  it now shows a warning and keeps retrying.
- Fixed interrupted downloads never finishing when this phone hosts sync, and Download
  failing for images and voice notes in that case.
- Fixed unlocking the app inside a protected chat asking for the PIN twice.

## 0.7.3 — 2026-07-27

### Added

- Added installation-owned device names that default from the OS, can be edited locally,
  and refresh automatically on authenticated paired devices.
- Added transfer progress for incoming media with a button to stop a download, and
  per-message removal of a downloaded file that keeps the message and can be downloaded
  again at any time.
- Added optional per-chat automatic deletion of messages older than a chosen age, and a
  media storage panel that reports how much space downloads use and reclaims files no
  message refers to any more.
- Added a "Force full resync" action under Troubleshooting that asks paired devices to
  send their whole history again. This has to be run here rather than on the desktop,
  which treats this device's record of what it already has as authoritative.

### Security

- Protection and profile PINs are now stretched before they are stored, so the saved
  verifier no longer falls to a trivial offline guess. Existing PINs keep working and are
  upgraded the next time they are entered.
- Restoring a backup now rejects archive entries that point outside the import folder, and
  refuses archives that would expand without bound.
- Deleting a message now removes its text from this device's records rather than only
  hiding it. This previously waited for a log to reach several thousand events, so an
  ordinary chat kept deleted notes readable on disk indefinitely; existing ones are
  cleaned up on the next launch.

### Changed

- Consolidated Android sync into one screen for adding and sharing devices, managing paired
  devices, choosing the connection mode, and configuring automatic file downloads.
- Nearby unpaired devices now appear separately at the top of Add device, while pairing and
  sharing JSON are tucked behind advanced options.
- Release APK builds now request signing passwords through hidden prompts instead of storing
  them in the signing properties file.

### Fixed

- Fixed voice messages longer than about three seconds, and files of any appreciable size,
  failing to send over Pear.
- Fixed images and voice messages reporting that the other device no longer had a file it
  was still holding, because every manual download asked for it as a file attachment.
- Fixed a removed voice message closing the app when played, a missing image rendering as
  an empty gap, and a removed file offering no way to fetch it back.
- Fixed removed and arriving media only updating after leaving and re-entering a chat.
- Gave images, voice messages and file attachments one shared presentation when they are
  not on this device, in place of three different ones.
- Fixed files larger than roughly nine megabytes ending the sync session on a local network
  instead of transferring.
- Fixed sending a large attachment stalling every other message until it finished, and
  ignoring the receiver's request to stop.

## 0.7.2 — 2026-07-24

### Added

- Added configurable automatic file-download limits on Android, Desktop, and Server, with
  an explicit download action for files above the receiving device's limit.
- Added signed Android release builds, per-ABI APK packaging, signature verification, and
  SHA-256 checksums for GitHub and Obtainium distribution.

### Changed

- Increased the temporary absolute file-transfer ceiling to 1 GiB while keeping each
  device's automatic-download preference independent and local.
- Legacy checklist messages now remain compatible while newly edited checklists preserve
  their Markdown task markers as ordinary synchronized messages.
- Removed credential rotation from already-paired device actions; removing and pairing the
  device again remains the explicit replacement workflow.

### Fixed

- Fixed minified Android release builds crashing when enabling Pear by preserving Bare Kit's
  Java/JNI bridge names.
- Fixed release-only Android QR/JSON pairing failures caused by minified reflected models.
- Fixed an Android initial-sync crash caused by reading a materialized chat while it was
  being replaced.
- Fixed chats disappearing after restarting Android by rebuilding derived chat files and
  recovering on-disk profiles whose registry entries were lost.
- Fixed checklist edits dropping list markers and collapsing checkbox labels on Android and
  Desktop.
- Fixed Bare runtimes remaining active after Ncy Desktop or Server had fully exited.
- Removed deprecated generic-array API usage from Core's authentication and encryption code.

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

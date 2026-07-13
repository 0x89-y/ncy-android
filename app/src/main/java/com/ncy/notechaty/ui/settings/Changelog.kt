package com.ncy.notechaty.ui.settings

/**
 * Ncy release notes, shown in Settings → About → What's new. Newest first.
 * Keep entries short and user-facing; bump the top version to match
 * build.gradle's versionName on release. Mirrors ncy-desktop's changelog.
 */
data class ChangelogEntry(
    val version: String,
    /** ISO date (YYYY-MM-DD). */
    val date: String,
    val changes: List<String>,
)

val CHANGELOG: List<ChangelogEntry> = listOf(
    ChangelogEntry(
        version = "0.6.5",
        date = "2026-07-11",
        changes = listOf(
            "Compacted deletion records now have additional stale-restore protection.",
            "Deleted chats now use compact backup-safe records with an optional warned purge.",
            "Chats can show messages created on other devices as incoming bubbles on the left.",
            "Incoming messages use a stable, theme-aware color for each originating device.",
        ),
    ),
    ChangelogEntry(
        version = "0.6.4",
        date = "2026-07-11",
        changes = listOf(
            "Device settings now explain guest-network isolation and blocked discovery.",
            "The discoverability switch now explains its effect on IP-address recovery.",
            "Local discovery no longer broadcasts the phone's friendly name or hostname.",
            "Pairing credentials can now be rotated or revoked with clear re-pairing behavior.",
            "Identically named paired devices now show a stable short ID fingerprint.",
            "Renamed devices now refresh after authenticating without changing pairing labels.",
            "Device cards now show connection, discovery, retry, and LAN diagnostics.",
        ),
    ),
    ChangelogEntry(
        version = "0.6.3",
        date = "2026-07-11",
        changes = listOf(
            "Discovered address changes are now trusted only after the device authenticates.",
            "Reinstalled devices now recover complete chat history and deletion tombstones.",
            "Any file type can now be sent, received, and saved as a chat attachment.",
        ),
    ),
    ChangelogEntry(
        version = "0.6.2",
        date = "2026-07-11",
        changes = listOf(
            "Backup restore now safely replays sync state without stale progress markers.",
            "Sync now detects cloned or mismatched device identities.",
            "Events created during initial catch-up are now delivered without gaps.",
            "Concurrent edits, deletes, profiles, and protected chats now resolve consistently.",
            "Pairing credentials now have clearer warnings and short clipboard lifetimes.",
            "Messages and media are now encrypted while syncing across the local network.",
        ),
    ),
    ChangelogEntry(
        version = "0.6.1",
        date = "2026-07-11",
        changes = listOf(
            "Sync now supports IPv6-only local networks.",
            "Offline phone hosts now explain when Ncy needs to be opened on the phone.",
            "Automatic sync can now be paused for individual devices.",
            "Sync now rejects device clocks that are dangerously far ahead.",
        ),
    ),
    ChangelogEntry(
        version = "0.6.0",
        date = "2026-07-11",
        changes = listOf(
            "Added phone-to-phone sync over local Wi-Fi, paired by scanning a QR code.",
            "Share this phone from Sync devices to host, with per-device invitations you can revoke.",
            "Paired devices reconnect automatically after their LAN address changes.",
            "Sync now rejects oversized frames and streams media with bounded memory use.",
            "Stalled media transfers clean themselves up automatically.",
        ),
    ),
    ChangelogEntry(
        version = "0.5.1",
        date = "2026-07-10",
        changes = listOf(
            "Added replies to messages.",
        ),
    ),
    ChangelogEntry(
        version = "0.5.0",
        date = "2026-07-09",
        changes = listOf(
            "Redesigned the app to match the Ncy desktop look — dotted background, flat divided-list cards, and accent-coloured message bubbles.",
            "Chat and profile avatars are now rounded squares, each with its own colour.",
            "Long-press menus, the reminder and icon pickers, and the create/rename sheets are now bottom sheets.",
            "Reorganised Settings and grouped import, export and auto-backup together under Backup.",
            "Added a \"What's new\" changelog, and refreshed the About and open-source licenses pages.",
            "Removed the top-bar and message-bubble gradient options.",
        ),
    ),
)

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
        version = "0.7.8",
        date = "2026-10-21",
        changes = listOf(
            "The settings screens now run edge to edge, like the chat list.",
            "Fixed the lines between rows being hard to see in the dark theme.",
        ),
    ),
    ChangelogEntry(
        version = "0.7.7",
        date = "2026-10-21",
        changes = listOf(
            "Added more reminder choices, each showing when it rings, and Pick date & time for any moment.",
            "Added Remove reminder for messages that already have one.",
            "Reminders set on another device now ring on this phone too, and tapping one opens its chat.",
            "Archived chats now open as their own list from the filter menu, next to Favorites.",
            "Favorites moved into the filter menu; the Reminders row shows only while a reminder is upcoming.",
            "The chat list and profiles now run edge to edge, and profile tags use each profile's colour.",
            "A new chat in All chats picks its profile from a dropdown.",
            "Fixed Favorites and Reminders only updating after a restart.",
            "Fixed removed reminders and deleted messages still ringing.",
            "Fixed the editing banner and other tinted labels being hard to read.",
        ),
    ),
    ChangelogEntry(
        version = "0.7.6",
        date = "2026-10-21",
        changes = listOf(
            "Added All chats, which lists the chats of every profile together, with a tag naming each chat's profile.",
            "Replaced the profile menu with a profile switcher showing each profile's chat count.",
            "Ncy now reopens in the profile, or in All chats, that you left it in.",
            "Favorites and Reminders moved from the bottom bar to rows above the chats, shown when they hold something.",
            "Deleting a profile now deletes its chats on every paired device, and a deleted profile no longer comes back.",
            "Fixed renamed profiles, profile PINs and profile icons being lost after Ncy restarted.",
            "Fixed an extra empty Personal profile appearing after every restart.",
        ),
    ),
    ChangelogEntry(
        version = "0.7.5",
        date = "2026-10-21",
        changes = listOf(
            "The chat list now shows the profile name at the top and a slimmer, icon-only bottom bar.",
            "New chats open as soon as they are created, and the name field is ready to type in.",
            "Added a voice message button to the empty message field, and moved formatting into the field as an Aa button.",
            "Added a menu to the chat header with Chat settings and Select messages.",
            "The message menu now shows which message it is for, with Reply, Copy, Edit and Select in one row and Delete set apart.",
            "Added hints to empty chats, Favorites and Reminders.",
            "Settings now has a Privacy & security page, and the back gesture returns from a settings page to the list.",
            "Replaced the Dynamic accent with Violet, and added a Wallpaper accent that follows the system colours.",
            "Sync settings now use plainer wording, and connection modes are named by where they connect.",
            "Fixed white text being hard to read on light accent colours in your own messages.",
            "Fixed links in messages using one fixed blue regardless of the bubble colour.",
        ),
    ),
    ChangelogEntry(
        version = "0.7.4",
        date = "2026-10-20",
        changes = listOf(
            "Added a Codes match step to pairing with a nearby desktop, so nothing is paired until both safety codes are confirmed.",
            "Images, voice notes and files in protected chats are now encrypted with the chat's PIN, here and on paired devices.",
            "The app lock and protected chats now lock again after Ncy has been in the background longer than the re-lock period.",
            "Wrong PIN entries now trigger a growing wait after the fourth attempt.",
            "Paired devices can no longer request files from outside a chat's media.",
            "Reminders without exact-alarm permission now fire within minutes instead of possibly hours late.",
            "Fixed Change PIN and Remove PIN leaving every protected chat unreadable.",
            "Fixed notes written while a device was offline never reaching devices that had synced in the meantime.",
            "Fixed messages that could be skipped for good when sync was interrupted partway through.",
            "Fixed a refusal sent before the connection is encrypted stopping sync for good.",
            "Fixed interrupted downloads never finishing when this phone hosts sync.",
            "Fixed unlocking the app inside a protected chat asking for the PIN twice.",
        ),
    ),
    ChangelogEntry(
        version = "0.7.3",
        date = "2026-07-27",
        changes = listOf(
            "Added editable device names that default from the phone and refresh automatically on authenticated paired devices.",
            "Consolidated Android sync into one screen, with nearby devices separated from paired devices and JSON options kept under advanced controls.",
            "Added transfer progress for incoming media, with a button to stop a download.",
            "Added per-message removal of downloaded media that keeps the message and can be downloaded again.",
            "Added a media storage panel that reclaims files no message refers to any more.",
            "Added optional per-chat automatic deletion of messages older than a chosen age, keeping starred messages and upcoming reminders.",
            "Added Force full resync, for a device missing messages or still showing ones deleted elsewhere.",
            "Deleting a message now removes its text from this device's records instead of only hiding it. Messages deleted earlier are cleaned up on the next launch.",
            "Protection and profile PINs are now stretched before they are stored. Existing PINs keep working and are upgraded the next time they are entered.",
            "Restoring a backup now rejects entries pointing outside the import folder, and archives that would expand without bound.",
            "Fixed images and voice messages being reported missing by a device that still held them.",
            "Fixed a removed voice message closing the app when played, and a missing image rendering as an empty gap.",
            "Fixed removed and arriving media only updating after leaving and re-entering a chat.",
            "Fixed voice messages over three seconds and files of any appreciable size failing to send over Pear.",
        ),
    ),
    ChangelogEntry(
        version = "0.7.2",
        date = "2026-07-24",
        changes = listOf(
            "Added configurable automatic file-download limits with an explicit download action for larger files.",
            "Added signed per-architecture Android release APKs with signature verification and SHA-256 checksums.",
            "Fixed release-only pairing, Pear startup, and initial-sync crashes.",
            "Fixed chats disappearing after restart by recovering their on-disk profile and rebuilding derived chat data.",
            "Fixed edited checklists losing their task markers or collapsing their labels.",
        ),
    ),
    ChangelogEntry(
        version = "0.7.1",
        date = "2026-07-22",
        changes = listOf(
            "New checklists are ordinary messages using interactive Markdown task markers, while legacy checklists remain fully supported.",
            "Moved the detailed device-sync guidance behind an information button to reduce visual clutter.",
            "Aligned the Android and Desktop composers, message actions, and chat-settings organization.",
            "Reorganized Settings into dedicated pages and aligned Favorites, Paired devices, connection statuses, and empty-state wording with Desktop.",
            "Grouped the changelog by release series so version history stays manageable as it grows.",
        ),
    ),
    ChangelogEntry(
        version = "0.7.0",
        date = "2026-07-19",
        changes = listOf(
            "Added optional Pear sync for paired devices on different networks.",
            "Added Local only, Pear only, Local preferred, and Pear preferred connection modes.",
            "New devices can pair securely by QR or JSON even when they are not on the same network.",
            "Pear remains off by default and starts only after explicit consent.",
            "Release downloads now target each device architecture instead of including every native runtime.",
        ),
    ),
    ChangelogEntry(
        version = "0.6.5",
        date = "2026-07-11",
        changes = listOf(
            "Compacted deletion records now have additional stale-restore protection.",
            "Deleted chats now use compact backup-safe records with an optional warned purge.",
            "Deleted messages now use compact restart-safe records that remain protected from stale restores.",
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
            "Unified the home screen with bottom Chats, Starred, and Reminders navigation plus an expandable inline chat-and-message search mode.",
            "Made archived chats opt-in and visually separated them from active chats across Android and Desktop.",
            "Reorganised Settings and grouped import, export and auto-backup together under Backup.",
            "Added a \"What's new\" changelog, and refreshed the About and open-source licenses pages.",
            "Removed the top-bar and message-bubble gradient options.",
        ),
    ),
)

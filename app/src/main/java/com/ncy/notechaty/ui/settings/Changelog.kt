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

package com.ncy.notechaty.data.model

import androidx.annotation.StringRes
import com.ncy.notechaty.R

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * The accent. The whole theme follows it: every surface is a neutral grey tinted
 * lightly toward it, and it marks your own notes, the selected filter and actions.
 */
enum class AccentColor(@StringRes val label: Int) {
    INK_BLUE(R.string.accent_ink_blue),
    TEAL(R.string.color_teal),
    MOSS(R.string.accent_moss),
    VIOLET(R.string.accent_violet),
    PLUM(R.string.accent_plum),
    GRAPHITE(R.string.accent_graphite),
    TERRACOTTA(R.string.accent_terracotta);

    companion object {
        /**
         * Read a stored value. Names from before the Notebook redesign map to their
         * nearest accent; the wallpaper option, or anything unknown, to ink blue.
         */
        fun fromStored(name: String?): AccentColor? = when (name) {
            null -> null
            "PURPLE", "DYNAMIC" -> VIOLET
            "BLUE" -> INK_BLUE
            "GREEN" -> MOSS
            "PINK" -> PLUM
            "RED", "ORANGE" -> TERRACOTTA
            else -> entries.firstOrNull { it.name == name } ?: INK_BLUE
        }
    }
}

enum class ChatSortMode(@StringRes val label: Int) {
    LAST_MESSAGE(R.string.sort_last_message),
    NAME(R.string.sort_name),
    CREATED(R.string.sort_date_created)
}

data class AppSettings(
    /** Installation-owned friendly name announced only after explicit pairing/authentication. */
    val deviceName: String = "Android phone",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.INK_BLUE,
    val chatSortMode: ChatSortMode = ChatSortMode.LAST_MESSAGE,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolder: String? = null,
    val autoBackupIntervalHours: Int = 24,
    /** Local-only threshold for automatically fetching generic file attachments. */
    val automaticFileDownloadLimitBytes: Long = 100L * 1024 * 1024,
    /**
     * Media on messages authored before this instant is listed rather than fetched;
     * the user downloads it on demand. Zero fetches everything.
     *
     * Stamped when this device first pairs. Joining an established mesh replays the
     * whole history at once, and without this the phone would immediately try to
     * pull down every photo, recording and attachment ever sent — over mobile data,
     * for content it has never shown. Installation-local, so an always-on peer can
     * keep everything while a phone keeps what it has actually opened.
     */
    val mediaBackfillCutoffMs: Long = 0L,
    val protectionPinHash: String? = null,
    val protectionPinSalt: String? = null,
    val protectionUseBiometric: Boolean = false,
    val lockOnAppStart: Boolean = false,
    val protectionGracePeriodMinutes: Int = 0
)

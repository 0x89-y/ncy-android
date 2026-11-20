package com.ncy.notechaty.data.model

import androidx.annotation.StringRes
import com.ncy.notechaty.R

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/**
 * The accent: one ink on the paper. Muted pigments only, tuned so each carries
 * white text on the light theme and dark text on the dark one.
 */
enum class AccentColor(@StringRes val label: Int) {
    TERRACOTTA(R.string.accent_terracotta),
    INK_BLUE(R.string.accent_ink_blue),
    MOSS(R.string.accent_moss),
    PLUM(R.string.accent_plum),
    GRAPHITE(R.string.accent_graphite);

    companion object {
        /**
         * Read a stored value. The bright presets from before the Notebook redesign
         * (violet, blue, pink, wallpaper…) clash with the paper, so any of them,
         * or anything unknown, falls back to terracotta.
         */
        fun fromStored(name: String?): AccentColor? =
            name?.let { stored -> entries.firstOrNull { it.name == stored } ?: TERRACOTTA }
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
    val accentColor: AccentColor = AccentColor.TERRACOTTA,
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

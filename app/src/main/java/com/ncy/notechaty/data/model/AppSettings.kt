package com.ncy.notechaty.data.model

import androidx.annotation.StringRes
import com.ncy.notechaty.R

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class AccentColor(@StringRes val label: Int) {
    PURPLE(R.string.accent_violet),
    BLUE(R.string.color_blue),
    GREEN(R.string.color_green),
    RED(R.string.color_red),
    ORANGE(R.string.color_orange),
    PINK(R.string.color_pink),
    TEAL(R.string.color_teal),
    /** Material You: the accent follows the wallpaper; neutrals stay Ncy's own. */
    WALLPAPER(R.string.accent_wallpaper);

    companion object {
        /**
         * Read a stored value. "DYNAMIC" was an option that always rendered violet;
         * every install that ever saved settings stored it, chosen or not, so it reads
         * as violet rather than as the wallpaper option that replaced it.
         */
        fun fromStored(name: String?): AccentColor? = when (name) {
            null -> null
            "DYNAMIC" -> PURPLE
            else -> runCatching { valueOf(name) }.getOrNull()
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
    val accentColor: AccentColor = AccentColor.PURPLE,
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

package com.ncy.notechaty.data.model

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class AccentColor(val displayName: String) {
    DYNAMIC("Dynamic"),
    PURPLE("Purple"),
    BLUE("Blue"),
    GREEN("Green"),
    RED("Red"),
    ORANGE("Orange"),
    PINK("Pink"),
    TEAL("Teal")
}

enum class ChatSortMode(val displayName: String) {
    LAST_MESSAGE("Last message"),
    NAME("Name"),
    CREATED("Date created")
}

enum class SyncTransportMode(
    val displayName: String,
    val description: String,
    val usesPear: Boolean,
) {
    LOCAL_ONLY("Local only", "Use Wi-Fi or Ethernet only", false),
    PEAR_ONLY("Pear only", "Always connect through Pear", true),
    LOCAL_PREFERRED("Local preferred", "Try local first, then Pear", true),
    PEAR_PREFERRED("Pear preferred", "Try Pear first, then local", true),
}

data class AppSettings(
    /** Installation-owned friendly name announced only after explicit pairing/authentication. */
    val deviceName: String = "Android phone",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.DYNAMIC,
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
    /** Installation-local policy. LOCAL_ONLY guarantees no Pear/DHT runtime starts. */
    val syncTransportMode: SyncTransportMode = SyncTransportMode.LOCAL_ONLY,
    val protectionPinHash: String? = null,
    val protectionPinSalt: String? = null,
    val protectionUseBiometric: Boolean = false,
    val lockOnAppStart: Boolean = false,
    val protectionGracePeriodMinutes: Int = 0
)

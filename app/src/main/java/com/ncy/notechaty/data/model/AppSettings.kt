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
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.DYNAMIC,
    val chatSortMode: ChatSortMode = ChatSortMode.LAST_MESSAGE,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolder: String? = null,
    val autoBackupIntervalHours: Int = 24,
    /** Installation-local policy. LOCAL_ONLY guarantees no Pear/DHT runtime starts. */
    val syncTransportMode: SyncTransportMode = SyncTransportMode.LOCAL_ONLY,
    val topBarGradient: Boolean = false,
    val protectionPinHash: String? = null,
    val protectionPinSalt: String? = null,
    val protectionUseBiometric: Boolean = false,
    val lockOnAppStart: Boolean = false,
    val protectionGracePeriodMinutes: Int = 0
)

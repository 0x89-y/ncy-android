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

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.DYNAMIC,
    val chatSortMode: ChatSortMode = ChatSortMode.LAST_MESSAGE,
    val autoBackupEnabled: Boolean = false,
    val autoBackupFolder: String? = null,
    val autoBackupIntervalHours: Int = 24
)

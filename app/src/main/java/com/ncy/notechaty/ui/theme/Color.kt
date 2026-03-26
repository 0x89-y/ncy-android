package com.ncy.notechaty.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.ncy.notechaty.data.model.AccentColor

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6750A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

val ChatBubbleColor = Color(0xFFDCF8C6)
val ChatBubbleDark = Color(0xFF005C4B)

// Accent color schemes — must include container colors for the UI to reflect the accent
fun accentLightScheme(accent: AccentColor) = when (accent) {
    AccentColor.PURPLE -> lightColorScheme(
        primary = Color(0xFF6750A4),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF21005D),
        secondary = Color(0xFF625B71),
        secondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFF7D5260),
        tertiaryContainer = Color(0xFFFFD8E4)
    )
    AccentColor.BLUE -> lightColorScheme(
        primary = Color(0xFF1976D2),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFD1E4FF),
        onPrimaryContainer = Color(0xFF001D36),
        secondary = Color(0xFF455A64),
        secondaryContainer = Color(0xFFD7E3EB),
        tertiary = Color(0xFF00695C),
        tertiaryContainer = Color(0xFFB2DFDB)
    )
    AccentColor.GREEN -> lightColorScheme(
        primary = Color(0xFF388E3C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFC8E6C9),
        onPrimaryContainer = Color(0xFF002204),
        secondary = Color(0xFF558B2F),
        secondaryContainer = Color(0xFFDCEDC8),
        tertiary = Color(0xFF00796B),
        tertiaryContainer = Color(0xFFB2DFDB)
    )
    AccentColor.RED -> lightColorScheme(
        primary = Color(0xFFC62828),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410002),
        secondary = Color(0xFF8E2424),
        secondaryContainer = Color(0xFFFFDAD6),
        tertiary = Color(0xFFAD1457),
        tertiaryContainer = Color(0xFFFFD8E4)
    )
    AccentColor.ORANGE -> lightColorScheme(
        primary = Color(0xFFE65100),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDBC8),
        onPrimaryContainer = Color(0xFF331200),
        secondary = Color(0xFFF57C00),
        secondaryContainer = Color(0xFFFFE0B2),
        tertiary = Color(0xFFFF6D00),
        tertiaryContainer = Color(0xFFFFDBC8)
    )
    AccentColor.PINK -> lightColorScheme(
        primary = Color(0xFFD81B60),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFD8E4),
        onPrimaryContainer = Color(0xFF3E001D),
        secondary = Color(0xFF880E4F),
        secondaryContainer = Color(0xFFFFD8E4),
        tertiary = Color(0xFFAD1457),
        tertiaryContainer = Color(0xFFFCE4EC)
    )
    AccentColor.TEAL -> lightColorScheme(
        primary = Color(0xFF00897B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFB2DFDB),
        onPrimaryContainer = Color(0xFF002018),
        secondary = Color(0xFF00695C),
        secondaryContainer = Color(0xFFB2DFDB),
        tertiary = Color(0xFF004D40),
        tertiaryContainer = Color(0xFF80CBC4)
    )
    AccentColor.DYNAMIC -> lightColorScheme()
}

fun accentDarkScheme(accent: AccentColor) = when (accent) {
    AccentColor.PURPLE -> darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        secondaryContainer = Color(0xFF4A4458),
        tertiary = Color(0xFFEFB8C8),
        tertiaryContainer = Color(0xFF633B48)
    )
    AccentColor.BLUE -> darkColorScheme(
        primary = Color(0xFF90CAF9),
        onPrimary = Color(0xFF003258),
        primaryContainer = Color(0xFF0D47A1),
        onPrimaryContainer = Color(0xFFD1E4FF),
        secondary = Color(0xFF90A4AE),
        secondaryContainer = Color(0xFF37474F),
        tertiary = Color(0xFF80CBC4),
        tertiaryContainer = Color(0xFF004D40)
    )
    AccentColor.GREEN -> darkColorScheme(
        primary = Color(0xFFA5D6A7),
        onPrimary = Color(0xFF003910),
        primaryContainer = Color(0xFF1B5E20),
        onPrimaryContainer = Color(0xFFC8E6C9),
        secondary = Color(0xFFC5E1A5),
        secondaryContainer = Color(0xFF33691E),
        tertiary = Color(0xFF80CBC4),
        tertiaryContainer = Color(0xFF004D40)
    )
    AccentColor.RED -> darkColorScheme(
        primary = Color(0xFFEF9A9A),
        onPrimary = Color(0xFF690005),
        primaryContainer = Color(0xFF93000A),
        onPrimaryContainer = Color(0xFFFFDAD6),
        secondary = Color(0xFFE57373),
        secondaryContainer = Color(0xFF7F1D1D),
        tertiary = Color(0xFFF48FB1),
        tertiaryContainer = Color(0xFF7B1F3E)
    )
    AccentColor.ORANGE -> darkColorScheme(
        primary = Color(0xFFFFCC80),
        onPrimary = Color(0xFF4A2800),
        primaryContainer = Color(0xFFBF360C),
        onPrimaryContainer = Color(0xFFFFDBC8),
        secondary = Color(0xFFFFB74D),
        secondaryContainer = Color(0xFF8D4E00),
        tertiary = Color(0xFFFFAB40),
        tertiaryContainer = Color(0xFF793800)
    )
    AccentColor.PINK -> darkColorScheme(
        primary = Color(0xFFF48FB1),
        onPrimary = Color(0xFF5C0032),
        primaryContainer = Color(0xFF880E4F),
        onPrimaryContainer = Color(0xFFFFD8E4),
        secondary = Color(0xFFCE93D8),
        secondaryContainer = Color(0xFF6A1B5D),
        tertiary = Color(0xFFF06292),
        tertiaryContainer = Color(0xFF7B1F3E)
    )
    AccentColor.TEAL -> darkColorScheme(
        primary = Color(0xFF80CBC4),
        onPrimary = Color(0xFF00332C),
        primaryContainer = Color(0xFF00695C),
        onPrimaryContainer = Color(0xFFB2DFDB),
        secondary = Color(0xFF4DB6AC),
        secondaryContainer = Color(0xFF004D40),
        tertiary = Color(0xFF26A69A),
        tertiaryContainer = Color(0xFF003730)
    )
    AccentColor.DYNAMIC -> darkColorScheme()
}

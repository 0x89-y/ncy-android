package com.ncy.notechaty.ui.theme

/*
 * ============================================================================
 *  Ncy "Notebook" design system
 * ============================================================================
 *  Warm paper, black ink and one accent: terracotta, or another paper ink. Names and titles
 *  are set in Newsreader, the interface and notes in Instrument Sans, and times
 *  and labels in JetBrains Mono. See docs/redesign/ for the mockups this follows.
 *
 *  Material's ColorScheme carries the roles it has; the roles it lacks (ink,
 *  hairlines, the soft accent, online green) live in [NcyColors], read through
 *  [NcyTheme.colors]. Text styles beyond Material's scale are in [NcyTheme.type].
 * ============================================================================
 */

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ncy.notechaty.R
import com.ncy.notechaty.data.model.AccentColor
import com.ncy.notechaty.data.model.ThemeMode

// ---------------------------------------------------------------------------
//  Fonts (OFL, bundled under res/font)
// ---------------------------------------------------------------------------
val NcySerif = FontFamily(
    Font(R.font.newsreader_medium, FontWeight.Normal),
    Font(R.font.newsreader_medium, FontWeight.Medium),
    Font(R.font.newsreader_medium, FontWeight.SemiBold),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
)
val NcySans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
    Font(R.font.instrument_sans_semibold, FontWeight.Bold),
)
val NcyMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

// ---------------------------------------------------------------------------
//  Paper and ink
// ---------------------------------------------------------------------------
private object Paper {
    // Light
    val LightBg = Color(0xFFF5F1EA)
    val LightSurface = Color(0xFFFBF9F5)
    val LightRaised = Color(0xFFFFFFFF)
    val LightSunk = Color(0xFFEDE7DD)
    val LightText = Color(0xFF1E1A15)
    val LightMuted = Color(0xFF685F54)
    val LightLine = Color(0xFFE4DDD1)
    val LightLineStrong = Color(0xFFD1C8B9)
    /** Borders that must be seen (fields, unchecked boxes): 3:1 on paper. */
    val LightOutline = Color(0xFF8F8678)
    val LightDanger = Color(0xFFB42318)
    val LightOnline = Color(0xFF2F7D48)

    // Dark
    val DarkBg = Color(0xFF15130F)
    val DarkSurface = Color(0xFF1D1A15)
    val DarkRaised = Color(0xFF25211B)
    val DarkSunk = Color(0xFF100E0B)
    val DarkText = Color(0xFFEFE9DF)
    val DarkMuted = Color(0xFFA89F91)
    val DarkLine = Color(0xFF2E2922)
    val DarkLineStrong = Color(0xFF3E372D)
    val DarkOutline = Color(0xFF6F675B)
    val DarkDanger = Color(0xFFF07A6A)
    val DarkOnline = Color(0xFF7CCB94)
}

/** An accent hue for the light and dark themes. */
private data class Accent(val light: Color, val dark: Color)

private fun accentFor(accent: AccentColor): Accent = when (accent) {
    AccentColor.TERRACOTTA -> Accent(Color(0xFFB4471F), Color(0xFFE8835C))
    AccentColor.INK_BLUE -> Accent(Color(0xFF2F4A7A), Color(0xFF8DA6D6))
    AccentColor.MOSS -> Accent(Color(0xFF4F6B3A), Color(0xFFA3BE84))
    AccentColor.PLUM -> Accent(Color(0xFF7A3B5E), Color(0xFFD094B6))
    AccentColor.GRAPHITE -> Accent(Color(0xFF3D3A36), Color(0xFFCFC8BD))
}

/** Linear sRGB-space mix: [pct] of [a] over [b]. */
fun mix(a: Color, b: Color, pct: Float): Color = Color(
    red = a.red * pct + b.red * (1 - pct),
    green = a.green * pct + b.green * (1 - pct),
    blue = a.blue * pct + b.blue * (1 - pct),
)

/** WCAG 2 contrast ratio between two colours (1 … 21). */
fun contrastRatio(a: Color, b: Color): Float {
    val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (hi + 0.05f) / (lo + 0.05f)
}

/** Ink or white, whichever reads better on [fill]. */
private fun readableOn(fill: Color, ink: Color): Color =
    if (contrastRatio(Color.White, fill) >= contrastRatio(ink, fill)) Color.White else ink

/**
 * [fill] darkened only as far as white text on it needs to reach 4.5:1, the WCAG
 * minimum for body text.
 */
fun withReadableWhiteText(fill: Color): Color {
    var color = fill
    repeat(24) {
        if (contrastRatio(Color.White, color) >= 4.5f) return color
        color = Color(color.red * 0.94f, color.green * 0.94f, color.blue * 0.94f, color.alpha)
    }
    return color
}

// ---------------------------------------------------------------------------
//  Roles Material lacks
// ---------------------------------------------------------------------------
@Immutable
data class NcyColors(
    val isDark: Boolean,
    /** The strongest neutral: primary buttons, selected chips, the FAB. */
    val ink: Color,
    val onInk: Color,
    /** Hairline between rows. */
    val line: Color,
    /** Decorative borders: chips, cards, segmented controls. */
    val lineStrong: Color,
    /** Accent-tinted fill: reminders, highlights, the soft side of the accent. */
    val accentSoft: Color,
    val onAccentSoft: Color,
    /** Fields and anything that sits above a surface. */
    val raised: Color,
    val sunk: Color,
    val online: Color,
    val scrim: Color,
)

private val LocalNcyColors = staticCompositionLocalOf {
    NcyColors(
        isDark = false, ink = Paper.LightText, onInk = Paper.LightSurface,
        line = Paper.LightLine, lineStrong = Paper.LightLineStrong,
        accentSoft = Color(0xFFF3E2D7), onAccentSoft = Color(0xFF7A2E12),
        raised = Paper.LightRaised, sunk = Paper.LightSunk, online = Paper.LightOnline,
        scrim = Color(0x6B1E1A15),
    )
}

// ---------------------------------------------------------------------------
//  Type
// ---------------------------------------------------------------------------
@Immutable
data class NcyType(
    /** Big page title: "Chats", "Settings". */
    val pageTitle: TextStyle,
    /** A chat's name in a list row or a top bar. */
    val name: TextStyle,
    /** Mono uppercase section label. */
    val label: TextStyle,
    /** Mono time stamps and small values. */
    val time: TextStyle,
    /** Body of a note in the chat. */
    val note: TextStyle,
    /** Italic serif: voice transcripts, day headers, chat names in results. */
    val quote: TextStyle,
)

private val NcyTypeDefault = NcyType(
    pageTitle = TextStyle(fontFamily = NcySerif, fontWeight = FontWeight.Medium, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-0.02).em),
    name = TextStyle(fontFamily = NcySerif, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 23.sp),
    label = TextStyle(fontFamily = NcyMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.08.em),
    time = TextStyle(fontFamily = NcyMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp),
    note = TextStyle(fontFamily = NcySans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    quote = TextStyle(fontFamily = NcySerif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
)

private val LocalNcyType = staticCompositionLocalOf { NcyTypeDefault }

private val NcyTypography = Typography().run {
    fun TextStyle.serif() = copy(fontFamily = NcySerif, fontWeight = FontWeight.Medium)
    fun TextStyle.sans() = copy(fontFamily = NcySans)
    copy(
        displayLarge = displayLarge.serif(),
        displayMedium = displayMedium.serif(),
        displaySmall = displaySmall.serif(),
        headlineLarge = headlineLarge.serif().copy(letterSpacing = (-0.02).em),
        headlineMedium = headlineMedium.serif().copy(letterSpacing = (-0.015).em),
        headlineSmall = headlineSmall.serif().copy(letterSpacing = (-0.01).em),
        // Top bar titles and dialog titles read as names, so they are serif.
        titleLarge = titleLarge.serif().copy(fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = titleMedium.serif().copy(fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
        titleSmall = titleSmall.sans().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.sans().copy(fontSize = 15.5.sp, lineHeight = 23.sp, letterSpacing = 0.sp),
        bodyMedium = bodyMedium.sans().copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
        bodySmall = bodySmall.sans().copy(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
        labelLarge = labelLarge.sans().copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.sp),
        labelMedium = labelMedium.sans().copy(fontWeight = FontWeight.Medium),
        labelSmall = labelSmall.sans().copy(fontWeight = FontWeight.Medium),
    )
}

// ---------------------------------------------------------------------------
//  Shape
// ---------------------------------------------------------------------------
private val NcyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// ---------------------------------------------------------------------------
//  Schemes
// ---------------------------------------------------------------------------
private fun lightScheme(a: Color): Pair<ColorScheme, NcyColors> {
    val soft = mix(a, Paper.LightSurface, 0.14f)
    val onSoft = mix(a, Color.Black, 0.6f)
    val scheme = lightColorScheme(
        primary = a, onPrimary = readableOn(a, Paper.LightText),
        primaryContainer = soft, onPrimaryContainer = onSoft,
        secondary = Paper.LightText, onSecondary = Paper.LightSurface,
        secondaryContainer = Paper.LightSunk, onSecondaryContainer = Paper.LightText,
        tertiary = a, onTertiary = readableOn(a, Paper.LightText),
        tertiaryContainer = soft, onTertiaryContainer = onSoft,
        background = Paper.LightBg, onBackground = Paper.LightText,
        surface = Paper.LightBg, onSurface = Paper.LightText,
        surfaceVariant = Paper.LightSunk, onSurfaceVariant = Paper.LightMuted,
        surfaceTint = Color.Transparent,
        surfaceBright = Paper.LightRaised, surfaceDim = Paper.LightSunk,
        surfaceContainerLowest = Paper.LightRaised,
        surfaceContainerLow = Paper.LightSurface,
        surfaceContainer = Paper.LightSurface,
        surfaceContainerHigh = Paper.LightSurface,
        surfaceContainerHighest = Paper.LightSunk,
        inverseSurface = Paper.LightText, inverseOnSurface = Paper.LightSurface, inversePrimary = mix(a, Color.White, 0.5f),
        outline = Paper.LightOutline, outlineVariant = Paper.LightLine,
        error = Paper.LightDanger, onError = Color.White,
        errorContainer = mix(Paper.LightDanger, Paper.LightSurface, 0.12f), onErrorContainer = mix(Paper.LightDanger, Color.Black, 0.55f),
        scrim = Color(0xFF1E1A15),
    )
    return scheme to NcyColors(
        isDark = false, ink = Paper.LightText, onInk = Paper.LightSurface,
        line = Paper.LightLine, lineStrong = Paper.LightLineStrong,
        accentSoft = soft, onAccentSoft = onSoft,
        raised = Paper.LightRaised, sunk = Paper.LightSunk, online = Paper.LightOnline,
        scrim = Color(0x6B1E1A15),
    )
}

private fun darkScheme(a: Color): Pair<ColorScheme, NcyColors> {
    val soft = mix(a, Paper.DarkSurface, 0.18f)
    val onSoft = mix(a, Color.White, 0.55f)
    val scheme = darkColorScheme(
        primary = a, onPrimary = readableOn(a, Paper.DarkBg),
        primaryContainer = soft, onPrimaryContainer = onSoft,
        secondary = Paper.DarkText, onSecondary = Paper.DarkBg,
        secondaryContainer = Paper.DarkRaised, onSecondaryContainer = Paper.DarkText,
        tertiary = a, onTertiary = readableOn(a, Paper.DarkBg),
        tertiaryContainer = soft, onTertiaryContainer = onSoft,
        background = Paper.DarkBg, onBackground = Paper.DarkText,
        surface = Paper.DarkBg, onSurface = Paper.DarkText,
        surfaceVariant = Paper.DarkRaised, onSurfaceVariant = Paper.DarkMuted,
        surfaceTint = Color.Transparent,
        surfaceBright = Paper.DarkRaised, surfaceDim = Paper.DarkSunk,
        surfaceContainerLowest = Paper.DarkSunk,
        surfaceContainerLow = Paper.DarkSurface,
        surfaceContainer = Paper.DarkSurface,
        surfaceContainerHigh = Paper.DarkSurface,
        surfaceContainerHighest = Paper.DarkRaised,
        inverseSurface = Paper.DarkText, inverseOnSurface = Paper.DarkBg, inversePrimary = mix(a, Color.Black, 0.5f),
        outline = Paper.DarkOutline, outlineVariant = Paper.DarkLine,
        error = Paper.DarkDanger, onError = Paper.DarkBg,
        errorContainer = mix(Paper.DarkDanger, Paper.DarkSurface, 0.18f), onErrorContainer = mix(Paper.DarkDanger, Color.White, 0.5f),
        scrim = Color.Black,
    )
    return scheme to NcyColors(
        isDark = true, ink = Paper.DarkText, onInk = Paper.DarkBg,
        line = Paper.DarkLine, lineStrong = Paper.DarkLineStrong,
        accentSoft = soft, onAccentSoft = onSoft,
        raised = Paper.DarkRaised, sunk = Paper.DarkSunk, online = Paper.DarkOnline,
        scrim = Color(0x8C000000),
    )
}

private fun resolveAccent(accentColor: AccentColor): Accent = accentFor(accentColor)

/** What a swatch for [accentColor] should show: the colour it really produces. */
fun accentSwatch(accentColor: AccentColor, dark: Boolean): Color =
    resolveAccent(accentColor).let { if (dark) it.dark else it.light }

/** Entry point for the app's theme. */
@Composable
fun NcyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.TERRACOTTA,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val accent = resolveAccent(accentColor)
    val (scheme, colors) = if (dark) darkScheme(accent.dark) else lightScheme(accent.light)
    CompositionLocalProvider(LocalNcyColors provides colors, LocalNcyType provides NcyTypeDefault) {
        MaterialTheme(colorScheme = scheme, typography = NcyTypography, shapes = NcyShapes, content = content)
    }
}

/** Notebook tokens Material does not carry. */
object NcyTheme {
    val colors: NcyColors
        @Composable @ReadOnlyComposable get() = LocalNcyColors.current
    val type: NcyType
        @Composable @ReadOnlyComposable get() = LocalNcyType.current
}

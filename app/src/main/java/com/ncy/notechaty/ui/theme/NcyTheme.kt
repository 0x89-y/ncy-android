package com.ncy.notechaty.ui.theme

/*
 * ============================================================================
 *  Ncy "Notebook" design system
 * ============================================================================
 *  Neutral paper tinted toward one accent (ink blue by default), black ink. Names and titles
 *  and notes are set in Geist, and times and labels in Geist Mono. See docs/redesign/ for the mockups this follows.
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
//  Fonts: Geist and Geist Mono (OFL, bundled under res/font)
// ---------------------------------------------------------------------------
/** Geist for everything that is read; titles and names are its semibold. */
val NcySans = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_semibold, FontWeight.Bold),
)
/** Geist Mono for times, counts and section labels. */
val NcyMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
)

// ---------------------------------------------------------------------------
//  Tones: neutral greys, each tinted lightly toward the accent, so the whole app
//  takes the accent's temperature. Ink blue gives cool blue-grey paper, moss a
//  green one, and so on; nothing is fixed warm.
// ---------------------------------------------------------------------------
private class Tones(
    val bg: Color, val surface: Color, val raised: Color, val sunk: Color,
    val text: Color, val muted: Color, val line: Color, val lineStrong: Color,
    /** Borders that must be seen (fields, unchecked boxes): 3:1 against [bg]. */
    val outline: Color,
    val danger: Color, val online: Color,
)

private fun lightTones(a: Color) = Tones(
    bg = mix(a, Color(0xFFF6F6F5), 0.05f),
    surface = mix(a, Color(0xFFFBFBFA), 0.03f),
    raised = Color.White,
    sunk = mix(a, Color(0xFFEDEDEC), 0.06f),
    text = mix(a, Color(0xFF18181A), 0.06f),
    muted = mix(a, Color(0xFF5F6066), 0.10f),
    line = mix(a, Color(0xFFE3E3E3), 0.10f),
    lineStrong = mix(a, Color(0xFFD0D0D0), 0.12f),
    outline = mix(a, Color(0xFF86878C), 0.12f),
    danger = Color(0xFFB42318),
    online = Color(0xFF2F7D48),
)

// A dark grey tinted toward the accent, never near-black, with text at about 12:1.
private fun darkTones(a: Color) = Tones(
    bg = mix(a, Color(0xFF1C1C1F), 0.07f),
    surface = mix(a, Color(0xFF242428), 0.08f),
    raised = mix(a, Color(0xFF28282C), 0.08f),
    sunk = mix(a, Color(0xFF161618), 0.06f),
    text = mix(a, Color(0xFFE2E2E5), 0.06f),
    muted = mix(a, Color(0xFF9D9EA4), 0.10f),
    line = mix(a, Color(0xFF323236), 0.10f),
    lineStrong = mix(a, Color(0xFF414146), 0.12f),
    outline = mix(a, Color(0xFF76777D), 0.12f),
    danger = Color(0xFFF07A6A),
    online = Color(0xFF7CCB94),
)

/** An accent hue for the light and dark themes. */
private data class Accent(val light: Color, val dark: Color)

private fun accentFor(accent: AccentColor): Accent = when (accent) {
    AccentColor.INK_BLUE -> Accent(Color(0xFF2F5AA8), Color(0xFF8FB0EA))
    AccentColor.TEAL -> Accent(Color(0xFF0F7069), Color(0xFF6CC9BE))
    AccentColor.MOSS -> Accent(Color(0xFF4A6E33), Color(0xFFA3C487))
    AccentColor.VIOLET -> Accent(Color(0xFF6741C9), Color(0xFFB29BF5))
    AccentColor.PLUM -> Accent(Color(0xFF8A3A66), Color(0xFFDB9BC0))
    AccentColor.GRAPHITE -> Accent(Color(0xFF3F3F46), Color(0xFFCFCFD6))
    AccentColor.TERRACOTTA -> Accent(Color(0xFFB4471F), Color(0xFFDD8662))
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
    /** Accent-tinted fill: your notes, reminders, the selected filter. */
    val accentSoft: Color,
    /** Border of an [accentSoft] fill, so a tinted shape keeps its edge. */
    val accentEdge: Color,
    val onAccentSoft: Color,
    /** Fields and anything that sits above a surface. */
    val raised: Color,
    val sunk: Color,
    val online: Color,
    val scrim: Color,
)

private val LocalNcyColors = staticCompositionLocalOf {
    lightScheme(accentFor(AccentColor.INK_BLUE).light).second
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
    /** Muted secondary text: voice transcripts, day headers, chat names in results. */
    val quote: TextStyle,
)

private val NcyTypeDefault = NcyType(
    pageTitle = TextStyle(fontFamily = NcySans, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.03).em),
    name = TextStyle(fontFamily = NcySans, fontWeight = FontWeight.SemiBold, fontSize = 16.5.sp, lineHeight = 22.sp, letterSpacing = (-0.01).em),
    label = TextStyle(fontFamily = NcyMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.08.em),
    time = TextStyle(fontFamily = NcyMono, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 14.sp),
    note = TextStyle(fontFamily = NcySans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    // Geist has no italic, and a faked one reads worse than the upright face.
    quote = TextStyle(fontFamily = NcySans, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 21.sp),
)

private val LocalNcyType = staticCompositionLocalOf { NcyTypeDefault }

private val NcyTypography = Typography().run {
    fun TextStyle.display() = copy(fontFamily = NcySans, fontWeight = FontWeight.SemiBold)
    fun TextStyle.sans() = copy(fontFamily = NcySans)
    copy(
        displayLarge = displayLarge.display(),
        displayMedium = displayMedium.display(),
        displaySmall = displaySmall.display(),
        headlineLarge = headlineLarge.display().copy(letterSpacing = (-0.02).em),
        headlineMedium = headlineMedium.display().copy(letterSpacing = (-0.015).em),
        headlineSmall = headlineSmall.display().copy(letterSpacing = (-0.01).em),
        // Top bar titles and dialog titles read as names, in the semibold.
        titleLarge = titleLarge.display().copy(fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
        titleMedium = titleMedium.display().copy(fontSize = 17.sp, lineHeight = 23.sp, letterSpacing = (-0.01).em),
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
    val t = lightTones(a)
    val soft = mix(a, Color.White, 0.11f)
    val onSoft = mix(a, Color.Black, 0.62f)
    val scheme = lightColorScheme(
        primary = a, onPrimary = readableOn(a, t.text),
        primaryContainer = soft, onPrimaryContainer = onSoft,
        secondary = t.text, onSecondary = t.surface,
        secondaryContainer = t.sunk, onSecondaryContainer = t.text,
        tertiary = a, onTertiary = readableOn(a, t.text),
        tertiaryContainer = soft, onTertiaryContainer = onSoft,
        background = t.bg, onBackground = t.text,
        surface = t.bg, onSurface = t.text,
        surfaceVariant = t.sunk, onSurfaceVariant = t.muted,
        surfaceTint = Color.Transparent,
        surfaceBright = t.raised, surfaceDim = t.sunk,
        surfaceContainerLowest = t.raised,
        surfaceContainerLow = t.surface,
        surfaceContainer = t.surface,
        surfaceContainerHigh = t.surface,
        surfaceContainerHighest = t.sunk,
        inverseSurface = t.text, inverseOnSurface = t.surface, inversePrimary = mix(a, Color.White, 0.5f),
        outline = t.outline, outlineVariant = t.line,
        error = t.danger, onError = Color.White,
        errorContainer = mix(t.danger, t.surface, 0.12f), onErrorContainer = mix(t.danger, Color.Black, 0.55f),
        scrim = Color(0xFF18181A),
    )
    return scheme to NcyColors(
        isDark = false, ink = t.text, onInk = t.surface,
        line = t.line, lineStrong = t.lineStrong,
        accentSoft = soft, accentEdge = mix(a, Color.White, 0.26f), onAccentSoft = onSoft,
        raised = t.raised, sunk = t.sunk, online = t.online,
        scrim = Color(0x6B18181A),
    )
}

private fun darkScheme(a: Color): Pair<ColorScheme, NcyColors> {
    val t = darkTones(a)
    val soft = mix(a, Color(0xFF28282C), 0.20f)
    val onSoft = mix(a, Color.White, 0.55f)
    val scheme = darkColorScheme(
        primary = a, onPrimary = readableOn(a, t.bg),
        primaryContainer = soft, onPrimaryContainer = onSoft,
        secondary = t.text, onSecondary = t.bg,
        secondaryContainer = t.raised, onSecondaryContainer = t.text,
        tertiary = a, onTertiary = readableOn(a, t.bg),
        tertiaryContainer = soft, onTertiaryContainer = onSoft,
        background = t.bg, onBackground = t.text,
        surface = t.bg, onSurface = t.text,
        surfaceVariant = t.raised, onSurfaceVariant = t.muted,
        surfaceTint = Color.Transparent,
        surfaceBright = t.raised, surfaceDim = t.sunk,
        surfaceContainerLowest = t.sunk,
        surfaceContainerLow = t.surface,
        surfaceContainer = t.surface,
        surfaceContainerHigh = t.surface,
        surfaceContainerHighest = t.raised,
        inverseSurface = t.text, inverseOnSurface = t.bg, inversePrimary = mix(a, Color.Black, 0.5f),
        outline = t.outline, outlineVariant = t.line,
        error = t.danger, onError = t.bg,
        errorContainer = mix(t.danger, t.surface, 0.18f), onErrorContainer = mix(t.danger, Color.White, 0.5f),
        scrim = Color.Black,
    )
    return scheme to NcyColors(
        isDark = true, ink = t.text, onInk = t.bg,
        line = t.line, lineStrong = t.lineStrong,
        accentSoft = soft, accentEdge = mix(a, Color(0xFF28282C), 0.34f), onAccentSoft = onSoft,
        raised = t.raised, sunk = t.sunk, online = t.online,
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
    accentColor: AccentColor = AccentColor.INK_BLUE,
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

package com.ncy.notechaty.ui.desktoptheme

/*
 * ============================================================================
 *  Ncy app theme  —  the "Acy-family" visual identity shared with the desktop.
 * ============================================================================
 *  Applied app-wide via NcyDesktopTheme (see MainActivity). Matches the Ncy
 *  desktop app (ncy-desktop/src/app.css): zinc neutrals, violet accent, small
 *  radii, no elevation, hairline dividers. Colour tokens below are copied
 *  verbatim from app.css so the two clients read as the same product.
 *
 *  This package (DesktopTheme/Background/Avatar/Components) is the single home
 *  for the app's design system — shared shapes, colours, and layout primitives.
 * ============================================================================
 */

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncy.notechaty.data.model.AccentColor
import com.ncy.notechaty.data.model.ThemeMode

// ---------------------------------------------------------------------------
//  Neutrals — the zinc scale from app.css :root / [data-theme='dark'].
// ---------------------------------------------------------------------------
private object DesktopNeutral {
    // Light  (:root)
    val LightBg = Color(0xFFF4F4F5)          // --bg
    val LightSurface = Color(0xFFFFFFFF)     // --surface
    val LightSurface2 = Color(0xFFF7F7F8)    // --surface-2
    val LightSurfaceHover = Color(0xFFF0F0F2)// --surface-hover
    val LightText = Color(0xFF18181B)        // --text
    val LightTextMuted = Color(0xFF71717A)   // --text-muted
    val LightBorder = Color(0xFFE4E4E7)      // --border
    val LightBorderStrong = Color(0xFFD4D4D8)// --border-strong

    // Dark  ([data-theme='dark'])
    val DarkBg = Color(0xFF161619)
    val DarkSurface = Color(0xFF202024)
    val DarkSurface2 = Color(0xFF27272C)
    val DarkSurfaceHover = Color(0xFF2E2E34)
    val DarkText = Color(0xFFF4F4F5)
    val DarkTextMuted = Color(0xFFA8A8B2)
    val DarkBorder = Color(0xFF313139)
    val DarkBorderStrong = Color(0xFF43434D)
}

/** An accent hue: the light+dark base values from app.css's [data-accent] presets. */
private data class Accent(
    val light: Color,
    val dark: Color,
    /** Text and icons on the accent. White for the presets; the wallpaper supplies its own. */
    val onLight: Color = AccentContrast,
    val onDark: Color = AccentContrast,
)

private val AccentContrast = Color(0xFFFFFFFF) // --accent-contrast (both themes)

// Values copied from the [data-accent='…'] presets in app.css.
private fun accentFor(accent: AccentColor): Accent = when (accent) {
    AccentColor.PURPLE ->
        Accent(Color(0xFF7C3AED), Color(0xFF8B5CF6))            // default identity
    // Resolved from the wallpaper in NcyDesktopTheme; violet only as a fallback.
    AccentColor.WALLPAPER -> Accent(Color(0xFF7C3AED), Color(0xFF8B5CF6))
    AccentColor.BLUE   -> Accent(Color(0xFF2563EB), Color(0xFF3B82F6))
    AccentColor.GREEN  -> Accent(Color(0xFF059669), Color(0xFF10B981))
    AccentColor.PINK   -> Accent(Color(0xFFDB2777), Color(0xFFEC4899))
    AccentColor.ORANGE -> Accent(Color(0xFFEA580C), Color(0xFFF97316))
    AccentColor.TEAL   -> Accent(Color(0xFF0D9488), Color(0xFF14B8A6))
    // Desktop has no red preset; use its danger hues so the option still works.
    AccentColor.RED    -> Accent(Color(0xFFDC2626), Color(0xFFF87171))
}

// --primary-container: color-mix(in srgb, accent 16%, surface)
private fun mix(accent: Color, surface: Color, accentPct: Float): Color = Color(
    red = accent.red * accentPct + surface.red * (1 - accentPct),
    green = accent.green * accentPct + surface.green * (1 - accentPct),
    blue = accent.blue * accentPct + surface.blue * (1 - accentPct),
)

private fun desktopLightScheme(accent: Accent): ColorScheme {
    val a = accent.light
    return lightColorScheme(
        primary = a,
        onPrimary = accent.onLight,
        primaryContainer = mix(a, DesktopNeutral.LightSurface, 0.16f),
        onPrimaryContainer = a,
        secondary = a,
        onSecondary = accent.onLight,
        secondaryContainer = mix(a, DesktopNeutral.LightSurface, 0.16f),
        onSecondaryContainer = a,
        tertiary = a,
        onTertiary = accent.onLight,
        background = DesktopNeutral.LightBg,
        onBackground = DesktopNeutral.LightText,
        surface = DesktopNeutral.LightSurface,
        onSurface = DesktopNeutral.LightText,
        surfaceVariant = DesktopNeutral.LightSurface2,
        onSurfaceVariant = DesktopNeutral.LightTextMuted,
        surfaceContainerLowest = DesktopNeutral.LightSurface,
        surfaceContainerLow = DesktopNeutral.LightSurface2,
        surfaceContainer = DesktopNeutral.LightSurface2,
        surfaceContainerHigh = DesktopNeutral.LightSurfaceHover,
        surfaceContainerHighest = DesktopNeutral.LightSurfaceHover,
        outline = DesktopNeutral.LightBorderStrong,
        outlineVariant = DesktopNeutral.LightBorder,
        error = Color(0xFFDC2626),              // --danger
        onError = AccentContrast,
    )
}

private fun desktopDarkScheme(accent: Accent): ColorScheme {
    val a = accent.dark
    return darkColorScheme(
        primary = a,
        onPrimary = accent.onDark,
        primaryContainer = mix(a, DesktopNeutral.DarkSurface, 0.16f),
        onPrimaryContainer = a,
        secondary = a,
        onSecondary = accent.onDark,
        secondaryContainer = mix(a, DesktopNeutral.DarkSurface, 0.16f),
        onSecondaryContainer = a,
        tertiary = a,
        onTertiary = accent.onDark,
        background = DesktopNeutral.DarkBg,
        onBackground = DesktopNeutral.DarkText,
        surface = DesktopNeutral.DarkSurface,
        onSurface = DesktopNeutral.DarkText,
        surfaceVariant = DesktopNeutral.DarkSurface2,
        onSurfaceVariant = DesktopNeutral.DarkTextMuted,
        surfaceContainerLowest = DesktopNeutral.DarkBg,
        surfaceContainerLow = DesktopNeutral.DarkSurface,
        surfaceContainer = DesktopNeutral.DarkSurface2,
        surfaceContainerHigh = DesktopNeutral.DarkSurfaceHover,
        surfaceContainerHighest = DesktopNeutral.DarkSurfaceHover,
        outline = DesktopNeutral.DarkBorderStrong,
        outlineVariant = DesktopNeutral.DarkBorder,
        error = Color(0xFFF87171),              // --danger (dark)
        onError = AccentContrast,
    )
}

/** WCAG 2 contrast ratio between two colours (1 … 21). */
fun contrastRatio(a: Color, b: Color): Float {
    val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
    return (hi + 0.05f) / (lo + 0.05f)
}

/**
 * [fill] darkened only as far as white text on it needs to reach 4.5:1, the WCAG
 * minimum for body text. Dark-theme accents are lightened for dark surfaces, so a
 * bubble in them carried white text at about 4.2:1 (violet) or far less (green).
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
//  Shape  —  --radius-sm 7 / --radius 10 / --radius-lg 14 / --radius-xl 20.
//  Desktop uses small, crisp corners rather than Material's default rounding.
// ---------------------------------------------------------------------------
private val DesktopShapes = Shapes(
    extraSmall = RoundedCornerShape(7.dp),
    small = RoundedCornerShape(7.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

// ---------------------------------------------------------------------------
//  Type  —  desktop uses IBM Plex Sans. It isn't bundled here (that needs font
//  files under res/font), so we fall back to the platform sans and match the
//  weight/line-height feel. To go the whole way: drop IBM Plex Sans .ttf files
//  into res/font and set fontFamily below to that FontFamily.
// ---------------------------------------------------------------------------
private val DesktopFont = FontFamily.SansSerif

private val DesktopTypography = Typography().run {
    copy(
        bodyLarge = bodyLarge.copy(fontFamily = DesktopFont, lineHeight = 24.sp),
        bodyMedium = bodyMedium.copy(fontFamily = DesktopFont),
        bodySmall = bodySmall.copy(fontFamily = DesktopFont),
        titleLarge = titleLarge.copy(fontFamily = DesktopFont, fontWeight = FontWeight.SemiBold, letterSpacing = TextUnit(-0.01f, TextUnitType.Em)),
        titleMedium = titleMedium.copy(fontFamily = DesktopFont, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = DesktopFont, fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontFamily = DesktopFont),
        labelMedium = labelMedium.copy(fontFamily = DesktopFont),
        labelSmall = labelSmall.copy(fontFamily = DesktopFont),
        headlineLarge = headlineLarge.copy(fontFamily = DesktopFont, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontFamily = DesktopFont, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontFamily = DesktopFont, fontWeight = FontWeight.SemiBold),
    )
}

/**
 * The accent [accentColor] resolves to — for [AccentColor.WALLPAPER], the
 * wallpaper's primary (Material You; minSdk 31 always has it). Only the accent
 * follows the wallpaper: neutrals, shapes and type stay Ncy's, so the app still
 * reads as the same product as the desktop.
 */
@Composable
private fun resolveAccent(accentColor: AccentColor): Accent {
    if (accentColor != AccentColor.WALLPAPER) return accentFor(accentColor)
    val context = LocalContext.current
    return remember(context) {
        val light = dynamicLightColorScheme(context)
        val dark = dynamicDarkColorScheme(context)
        Accent(
            light = light.primary,
            dark = dark.primary,
            onLight = light.onPrimary,
            onDark = dark.onPrimary,
        )
    }
}

/** What a swatch for [accentColor] should show: the colour it really produces. */
@Composable
fun accentSwatch(accentColor: AccentColor, dark: Boolean): Color =
    resolveAccent(accentColor).let { if (dark) it.dark else it.light }

/** The app's theme. Violet is the default identity shared with the desktop. */
@Composable
fun NcyDesktopTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.PURPLE,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val accent = resolveAccent(accentColor)
    val colorScheme = if (darkTheme) desktopDarkScheme(accent) else desktopLightScheme(accent)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DesktopTypography,
        shapes = DesktopShapes,
        content = content
    )
}

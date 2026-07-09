package com.ncy.notechaty.ui.desktoptheme

/*
 * ============================================================================
 *  Ncy "Desktop" theme  —  self-contained, safe to delete.
 * ============================================================================
 *  This whole package is an OPTIONAL re-skin that matches the Ncy desktop app's
 *  "Acy-family" visual identity (see ncy-desktop/src/app.css): zinc neutrals,
 *  violet accent, small radii, no elevation, hairline dividers.
 *
 *  HOW TO ENABLE
 *    In MainActivity.kt, swap the import + call site:
 *        import com.ncy.notechaty.ui.theme.NoteChatyTheme          // old
 *        import com.ncy.notechaty.ui.desktoptheme.NcyDesktopTheme  // new
 *    ...and change   NoteChatyTheme( ... )   to   NcyDesktopTheme( ... ).
 *    The signature is identical, so nothing else needs to change.
 *
 *  HOW TO REMOVE
 *    Revert that one import + call in MainActivity, then delete this folder.
 *    Nothing else in the app references this package.
 *
 *  Colour tokens below are copied verbatim from ncy-desktop/src/app.css so the
 *  two clients read as the same product.
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
import androidx.compose.ui.graphics.Color
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
private data class Accent(val light: Color, val dark: Color)

private val AccentContrast = Color(0xFFFFFFFF) // --accent-contrast (both themes)

// Values copied from the [data-accent='…'] presets in app.css.
private fun accentFor(accent: AccentColor): Accent = when (accent) {
    AccentColor.PURPLE, AccentColor.DYNAMIC ->
        Accent(Color(0xFF7C3AED), Color(0xFF8B5CF6))            // default identity
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
        onPrimary = AccentContrast,
        primaryContainer = mix(a, DesktopNeutral.LightSurface, 0.16f),
        onPrimaryContainer = a,
        secondary = a,
        onSecondary = AccentContrast,
        secondaryContainer = mix(a, DesktopNeutral.LightSurface, 0.16f),
        onSecondaryContainer = a,
        tertiary = a,
        onTertiary = AccentContrast,
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
        onPrimary = AccentContrast,
        primaryContainer = mix(a, DesktopNeutral.DarkSurface, 0.16f),
        onPrimaryContainer = a,
        secondary = a,
        onSecondary = AccentContrast,
        secondaryContainer = mix(a, DesktopNeutral.DarkSurface, 0.16f),
        onSecondaryContainer = a,
        tertiary = a,
        onTertiary = AccentContrast,
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
 * Desktop-styled theme. Drop-in replacement for [com.ncy.notechaty.ui.theme.NoteChatyTheme]
 * with an identical signature. Note: [AccentColor.DYNAMIC] deliberately maps to the
 * desktop's default violet (Material You would break the shared identity).
 */
@Composable
fun NcyDesktopTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.DYNAMIC,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val accent = accentFor(accentColor)
    val colorScheme = if (darkTheme) desktopDarkScheme(accent) else desktopLightScheme(accent)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DesktopTypography,
        shapes = DesktopShapes,
        content = content
    )
}

package com.docket.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DocketDarkColorScheme = darkColorScheme(
    primary = DocketPrimaryDark,
    onPrimary = DocketOnPrimaryDark,
    primaryContainer = DocketPrimaryContainerDark,
    onPrimaryContainer = DocketOnPrimaryContainerDark,
    secondary = DocketSecondaryDark,
    onSecondary = DocketOnSecondaryDark,
    secondaryContainer = DocketSecondaryContainerDark,
    onSecondaryContainer = DocketOnSecondaryContainerDark,
    // Tertiary mirrors secondary — see Color.kt.
    tertiary = DocketSecondaryDark,
    onTertiary = DocketOnSecondaryDark,
    tertiaryContainer = DocketSecondaryContainerDark,
    onTertiaryContainer = DocketOnSecondaryContainerDark,
    error = DocketErrorDark,
    onError = DocketOnErrorDark,
    errorContainer = DocketErrorContainerDark,
    onErrorContainer = DocketOnErrorContainerDark,
    background = DocketBackgroundDark,
    onBackground = DocketOnBackgroundDark,
    surface = DocketSurfaceDark,
    onSurface = DocketOnSurfaceDark,
    surfaceVariant = DocketSurfaceVariantDark,
    onSurfaceVariant = DocketOnSurfaceVariantDark,
    outline = DocketOutlineDark,
    outlineVariant = DocketOutlineVariantDark,
    surfaceContainerLowest = DocketSurfaceContainerLowestDark,
    surfaceContainerLow = DocketSurfaceContainerLowDark,
    surfaceContainer = DocketSurfaceContainerDark,
    surfaceContainerHigh = DocketSurfaceContainerHighDark,
    surfaceContainerHighest = DocketSurfaceContainerHighestDark,
    inverseSurface = DocketInverseSurfaceDark,
    inverseOnSurface = DocketInverseOnSurfaceDark,
    inversePrimary = DocketPrimaryLight,
    scrim = DocketScrim
)

private val DocketLightColorScheme = lightColorScheme(
    primary = DocketPrimaryLight,
    onPrimary = DocketOnPrimaryLight,
    primaryContainer = DocketPrimaryContainerLight,
    onPrimaryContainer = DocketOnPrimaryContainerLight,
    secondary = DocketSecondaryLight,
    onSecondary = DocketOnSecondaryLight,
    secondaryContainer = DocketSecondaryContainerLight,
    onSecondaryContainer = DocketOnSecondaryContainerLight,
    tertiary = DocketSecondaryLight,
    onTertiary = DocketOnSecondaryLight,
    tertiaryContainer = DocketSecondaryContainerLight,
    onTertiaryContainer = DocketOnSecondaryContainerLight,
    error = DocketErrorLight,
    onError = DocketOnErrorLight,
    errorContainer = DocketErrorContainerLight,
    onErrorContainer = DocketOnErrorContainerLight,
    background = DocketBackgroundLight,
    onBackground = DocketOnBackgroundLight,
    surface = DocketSurfaceLight,
    onSurface = DocketOnSurfaceLight,
    surfaceVariant = DocketSurfaceVariantLight,
    onSurfaceVariant = DocketOnSurfaceVariantLight,
    outline = DocketOutlineLight,
    outlineVariant = DocketOutlineVariantLight,
    surfaceContainerLowest = DocketSurfaceContainerLowestLight,
    surfaceContainerLow = DocketSurfaceContainerLowLight,
    surfaceContainer = DocketSurfaceContainerLight,
    surfaceContainerHigh = DocketSurfaceContainerHighLight,
    surfaceContainerHighest = DocketSurfaceContainerHighestLight,
    inverseSurface = DocketInverseSurfaceLight,
    inverseOnSurface = DocketInverseOnSurfaceLight,
    inversePrimary = DocketPrimaryDark,
    scrim = DocketScrim
)

/**
 * App-wide Material 3 theme: color scheme + type scale + shapes + the extended (status) color
 * roles. Light-first — [darkTheme] defaults to the system setting, not forced light.
 *
 * Dynamic color (Android 12+) is opt-in and off by default: the brand is one deliberate
 * indigo/violet, not per-device wallpaper theming — see the accent rationale in Color.kt.
 */
@Composable
fun DocketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DocketDarkColorScheme
        else -> DocketLightColorScheme
    }
    val extendedColors = if (darkTheme) DocketDarkExtendedColors else DocketLightExtendedColors

    CompositionLocalProvider(LocalDocketExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DocketTypography,
            shapes = DocketShapes,
            content = content
        )
    }
}

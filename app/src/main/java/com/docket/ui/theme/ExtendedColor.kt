package com.docket.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Color roles Material 3's [androidx.compose.material3.ColorScheme] doesn't have — right now
 * just "safe" and "warning" for the warranty urgency indicator. "Urgent"/"expired" deliberately
 * reuse `MaterialTheme.colorScheme.error` instead of adding a third role here; see WarrantyCard.
 *
 * These are NOT the brand accent — they're semantic status colors, same category as M3's own
 * `error`, and are exempt from the "one accent, primary actions only" rule for that reason.
 */
data class DocketExtendedColors(
    val safe: Color,
    val onSafe: Color,
    val safeContainer: Color,
    val onSafeContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

val LocalDocketExtendedColors = staticCompositionLocalOf {
    DocketExtendedColors(
        safe = DocketSafeLight,
        onSafe = DocketOnSafeLight,
        safeContainer = DocketSafeContainerLight,
        onSafeContainer = DocketOnSafeContainerLight,
        warning = DocketWarningLight,
        onWarning = DocketOnWarningLight,
        warningContainer = DocketWarningContainerLight,
        onWarningContainer = DocketOnWarningContainerLight
    )
}

val DocketLightExtendedColors = DocketExtendedColors(
    safe = DocketSafeLight,
    onSafe = DocketOnSafeLight,
    safeContainer = DocketSafeContainerLight,
    onSafeContainer = DocketOnSafeContainerLight,
    warning = DocketWarningLight,
    onWarning = DocketOnWarningLight,
    warningContainer = DocketWarningContainerLight,
    onWarningContainer = DocketOnWarningContainerLight
)

val DocketDarkExtendedColors = DocketExtendedColors(
    safe = DocketSafeDark,
    onSafe = DocketOnSafeDark,
    safeContainer = DocketSafeContainerDark,
    onSafeContainer = DocketOnSafeContainerDark,
    warning = DocketWarningDark,
    onWarning = DocketOnWarningDark,
    warningContainer = DocketWarningContainerDark,
    onWarningContainer = DocketOnWarningContainerDark
)

/** Usage: `docketExtendedColors().warning`, mirroring `MaterialTheme.colorScheme`. */
@Composable
fun docketExtendedColors(): DocketExtendedColors = LocalDocketExtendedColors.current

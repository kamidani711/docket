package com.docket.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Color roles Material 3's [androidx.compose.material3.ColorScheme] doesn't have:
 * - "safe"/"warning" for the warranty urgency indicator ("urgent"/"expired" deliberately reuse
 *   `MaterialTheme.colorScheme.error` instead of a third role here — see WarrantyCard).
 * - "coral"/"violet"/"taupe" — the design handoff's decorative tool-tone colors, used only as
 *   icon-on-tint pairs on Home's tool grid (see Color.kt's class doc for why they're icon-only
 *   rather than AA-verified text colors). Amber and mint don't get their own pair here since
 *   they're visually identical to warning/safe — Home's tool grid reads those two roles
 *   directly rather than duplicating the token.
 *
 * None of these are the brand accent — they're semantic/decorative roles, same category as
 * M3's own `error`, and are exempt from the "one accent, primary actions only" rule.
 */
data class DocketExtendedColors(
    val safe: Color,
    val onSafe: Color,
    val safeContainer: Color,
    val onSafeContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val coral: Color,
    val onCoral: Color,
    val coralContainer: Color,
    val onCoralContainer: Color,
    val violet: Color,
    val onViolet: Color,
    val violetContainer: Color,
    val onVioletContainer: Color,
    val taupe: Color,
    val onTaupe: Color,
    val taupeContainer: Color,
    val onTaupeContainer: Color,
    /** Home tool-grid's own amber/mint dark container backgrounds — see Color.kt class doc. */
    val amberToolContainer: Color,
    val mintToolContainer: Color,
    /** Raw decorative amber/mint fills (Onboarding's accent dot, Premium's star badge) — icon/fill
     *  only, not verified for text contrast. See Color.kt class doc. */
    val amber: Color,
    val mint: Color,
    /** Tertiary label tone, distinct from `muted`/onSurfaceVariant. See Color.kt class doc. */
    val ink2: Color
)

val LocalDocketExtendedColors = staticCompositionLocalOf { DocketLightExtendedColors }

val DocketLightExtendedColors = DocketExtendedColors(
    safe = DocketSafeLight,
    onSafe = DocketOnSafeLight,
    safeContainer = DocketSafeContainerLight,
    onSafeContainer = DocketOnSafeContainerLight,
    warning = DocketWarningLight,
    onWarning = DocketOnWarningLight,
    warningContainer = DocketWarningContainerLight,
    onWarningContainer = DocketOnWarningContainerLight,
    coral = DocketCoralLight,
    onCoral = DocketOnCoralLight,
    coralContainer = DocketCoralContainerLight,
    onCoralContainer = DocketOnCoralContainerLight,
    violet = DocketVioletLight,
    onViolet = DocketOnVioletLight,
    violetContainer = DocketVioletContainerLight,
    onVioletContainer = DocketOnVioletContainerLight,
    taupe = DocketTaupeLight,
    onTaupe = DocketOnTaupeLight,
    taupeContainer = DocketTaupeContainerLight,
    onTaupeContainer = DocketOnTaupeContainerLight,
    // Light mode's tool tiles use warningContainer/safeContainer directly (already pale tints).
    amberToolContainer = DocketWarningContainerLight,
    mintToolContainer = DocketSafeContainerLight,
    amber = DocketAmberLight,
    mint = DocketMintLight,
    ink2 = DocketInk2Light
)

val DocketDarkExtendedColors = DocketExtendedColors(
    safe = DocketSafeDark,
    onSafe = DocketOnSafeDark,
    safeContainer = DocketSafeContainerDark,
    onSafeContainer = DocketOnSafeContainerDark,
    warning = DocketWarningDark,
    onWarning = DocketOnWarningDark,
    warningContainer = DocketWarningContainerDark,
    onWarningContainer = DocketOnWarningContainerDark,
    coral = DocketCoralDark,
    onCoral = DocketOnCoralDark,
    coralContainer = DocketCoralContainerDark,
    onCoralContainer = DocketOnCoralContainerDark,
    violet = DocketVioletDark,
    onViolet = DocketOnVioletDark,
    violetContainer = DocketVioletContainerDark,
    onVioletContainer = DocketOnVioletContainerDark,
    taupe = DocketTaupeDark,
    onTaupe = DocketOnTaupeDark,
    taupeContainer = DocketTaupeContainerDark,
    onTaupeContainer = DocketOnTaupeContainerDark,
    amberToolContainer = DocketAmberToolContainerDark,
    mintToolContainer = DocketMintToolContainerDark,
    amber = DocketAmberDark,
    mint = DocketMintDark,
    ink2 = DocketInk2Dark
)

/** Usage: `docketExtendedColors().warning`, mirroring `MaterialTheme.colorScheme`. */
@Composable
fun docketExtendedColors(): DocketExtendedColors = LocalDocketExtendedColors.current

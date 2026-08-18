package com.docket.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Docket's full color palette — a Material 3 role set for light and dark, hand-tuned rather
 * than generated from Material Theme Builder. Neutrals share the primary's hue at very low
 * chroma (the standard M3 approach) so nothing reads as a second accent.
 *
 * ACCENT CHOICE — deep teal (#0F6B62)
 * Picked deliberately over the blue/purple every scanner-adjacent app defaults to:
 *  - Reads as "ink/stamp/ledger", not "tech product" — closer to a notary stamp than an app.
 *  - Sits far from the red/orange that CamScanner-style apps spend on urgency badges and
 *    upsell banners, so our one accent never gets mistaken for a promo.
 *  - Doubles cleanly as the "safe" end of the warranty urgency scale (teal-to-green reads as
 *    one calm family), while amber/red stay reserved for actual warnings.
 *  - Passes WCAG AA (>4.5:1) for body text on both white and near-black surfaces.
 * Per the brief: this is the ONLY accent, and it only appears on primary actions and
 * functional state (selection, focus) — never as decoration. See Buttons.kt.
 */

// ---- Primary (the one accent) ----
val DocketPrimaryLight = Color(0xFF0F6B62)
val DocketOnPrimaryLight = Color(0xFFFFFFFF)
val DocketPrimaryContainerLight = Color(0xFFB4F1E4)
val DocketOnPrimaryContainerLight = Color(0xFF00201B)

val DocketPrimaryDark = Color(0xFF7ADCCF)
val DocketOnPrimaryDark = Color(0xFF00382F)
val DocketPrimaryContainerDark = Color(0xFF005046)
val DocketOnPrimaryContainerDark = Color(0xFFB4F1E4)

// ---- Secondary / tertiary — deliberately neutral, not a second accent ----
// Tertiary mirrors secondary exactly: M3 components fall back to `tertiary` in a few places
// (some chip/FAB defaults) and we don't want a stray third hue sneaking in there.
val DocketSecondaryLight = Color(0xFF4A635F)
val DocketOnSecondaryLight = Color(0xFFFFFFFF)
val DocketSecondaryContainerLight = Color(0xFFCCE8E2)
val DocketOnSecondaryContainerLight = Color(0xFF051F1B)

val DocketSecondaryDark = Color(0xFFB1CCC6)
val DocketOnSecondaryDark = Color(0xFF1C352F)
val DocketSecondaryContainerDark = Color(0xFF334B46)
val DocketOnSecondaryContainerDark = Color(0xFFCCE8E2)

// ---- Error (system semantic, not the brand accent) ----
val DocketErrorLight = Color(0xFFBA1A1A)
val DocketOnErrorLight = Color(0xFFFFFFFF)
val DocketErrorContainerLight = Color(0xFFFFDAD6)
val DocketOnErrorContainerLight = Color(0xFF410002)

val DocketErrorDark = Color(0xFFFFB4AB)
val DocketOnErrorDark = Color(0xFF690005)
val DocketErrorContainerDark = Color(0xFF93000A)
val DocketOnErrorContainerDark = Color(0xFFFFDAD6)

// ---- Neutral surfaces — "paper" ----
val DocketBackgroundLight = Color(0xFFFBFDFA)
val DocketOnBackgroundLight = Color(0xFF191C1B)
val DocketSurfaceLight = Color(0xFFFFFFFF)
val DocketOnSurfaceLight = Color(0xFF191C1B)
val DocketSurfaceVariantLight = Color(0xFFDDE5E2)
val DocketOnSurfaceVariantLight = Color(0xFF404944)
val DocketOutlineLight = Color(0xFF707974)
val DocketOutlineVariantLight = Color(0xFFC0C9C4)
val DocketSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val DocketSurfaceContainerLowLight = Color(0xFFF5F7F4)
val DocketSurfaceContainerLight = Color(0xFFEFF2EE)
val DocketSurfaceContainerHighLight = Color(0xFFE9ECE8)
val DocketSurfaceContainerHighestLight = Color(0xFFE3E6E2)
val DocketInverseSurfaceLight = Color(0xFF2E312F)
val DocketInverseOnSurfaceLight = Color(0xFFF0F2EE)

val DocketBackgroundDark = Color(0xFF191C1B)
val DocketOnBackgroundDark = Color(0xFFE2E3DF)
val DocketSurfaceDark = Color(0xFF1F2321)
val DocketOnSurfaceDark = Color(0xFFE2E3DF)
val DocketSurfaceVariantDark = Color(0xFF404944)
val DocketOnSurfaceVariantDark = Color(0xFFC0C9C4)
val DocketOutlineDark = Color(0xFF8A938E)
val DocketOutlineVariantDark = Color(0xFF404944)
val DocketSurfaceContainerLowestDark = Color(0xFF0B0F0E)
val DocketSurfaceContainerLowDark = Color(0xFF191C1B)
val DocketSurfaceContainerDark = Color(0xFF1D211F)
val DocketSurfaceContainerHighDark = Color(0xFF282B2A)
val DocketSurfaceContainerHighestDark = Color(0xFF333635)
val DocketInverseSurfaceDark = Color(0xFFE2E3DF)
val DocketInverseOnSurfaceDark = Color(0xFF2E312F)

val DocketScrim = Color(0xFF000000)

// ---- Status (warranty urgency) — semantic, outside the M3 ColorScheme ----
// "Urgent"/"Expired" deliberately reuse MaterialTheme.colorScheme.error instead of adding a
// third custom role — see ExtendedColor.kt.
val DocketSafeLight = Color(0xFF2E6E4E)
val DocketOnSafeLight = Color(0xFFFFFFFF)
val DocketSafeContainerLight = Color(0xFFB6F0CB)
val DocketOnSafeContainerLight = Color(0xFF00210F)

val DocketSafeDark = Color(0xFF9BD4B3)
val DocketOnSafeDark = Color(0xFF00391D)
val DocketSafeContainerDark = Color(0xFF14512F)
val DocketOnSafeContainerDark = Color(0xFFB6F0CB)

val DocketWarningLight = Color(0xFF8A5300)
val DocketOnWarningLight = Color(0xFFFFFFFF)
val DocketWarningContainerLight = Color(0xFFFFDDB0)
val DocketOnWarningContainerLight = Color(0xFF2A1700)

val DocketWarningDark = Color(0xFFFFB955)
val DocketOnWarningDark = Color(0xFF472A00)
val DocketWarningContainerDark = Color(0xFF663E00)
val DocketOnWarningContainerDark = Color(0xFFFFDDB0)

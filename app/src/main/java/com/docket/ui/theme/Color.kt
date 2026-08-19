package com.docket.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Docket's full color palette — a Material 3 role set for light and dark, following the
 * `design_handoff_docket_ui` handoff's literal token table (Urbanist / `#5468F0` / flat tiles).
 *
 * ACCENT — `#5468F0` (handoff's literal value, used as-is; it's the reference brand color, not
 * a mockup approximation like the previous indigo/violet round had to correct for).
 * Per the handoff: pressed/hover states use the platform's default ripple + Docket's existing
 * press-scale animation (see Buttons.kt) — no manually-swapped "pressed" color token.
 *
 * A note on the handoff's five decorative tool-tone colors (coral/amber/mint/violet/taupe, see
 * ExtendedColor.kt): each tone's "icon on pale tint" pairing (e.g. `#FCAF4E` amber on `#FFF3E1`
 * amberTint) is intentionally low-contrast in the source design — it's a decorative icon
 * standing next to a fully-legible ink-colored label, the same latitude real icon systems get
 * everywhere. That pairing is used as-is for the Home tool grid (decorative, label carries
 * meaning). Where a tone is ever used as real standalone text — the warranty urgency chip, the
 * mint "Warranty to <date>" chip — this file uses a hand-darkened tone instead so the text
 * itself clears WCAG AA (4.5:1), same rigor every past pass of this file has applied. Destructive
 * text (Discard, Logout) reuses `error`, not the decorative `coral` tone, for the same reason —
 * see WarrantyCard's existing "urgent reuses error" precedent.
 */

// ---- Primary (the one accent) ----
val DocketPrimaryLight = Color(0xFF5468F0)
val DocketOnPrimaryLight = Color(0xFFFFFFFF)
val DocketPrimaryContainerLight = Color(0xFFEDEFFE)
val DocketOnPrimaryContainerLight = Color(0xFF212121)

val DocketPrimaryDark = Color(0xFFAEB6FF)
val DocketOnPrimaryDark = Color(0xFF1B2370)
val DocketPrimaryContainerDark = Color(0xFF3642B0)
val DocketOnPrimaryContainerDark = Color(0xFFE4E7FF)

// ---- Secondary / tertiary — neutral gray-violet fallback, not part of the handoff's named
// palette (no screen styles with an M3 "secondary" role directly) but kept coherent with the
// accent hue since a few stock M3 component defaults fall back to it. ----
val DocketSecondaryLight = Color(0xFF5B5470)
val DocketOnSecondaryLight = Color(0xFFFFFFFF)
val DocketSecondaryContainerLight = Color(0xFFE5DFF5)
val DocketOnSecondaryContainerLight = Color(0xFF1B1330)

val DocketSecondaryDark = Color(0xFFC7C1DD)
val DocketOnSecondaryDark = Color(0xFF322C46)
val DocketSecondaryContainerDark = Color(0xFF433C59)
val DocketOnSecondaryContainerDark = Color(0xFFE5DFF5)

// ---- Error (system semantic, not the brand accent; also stands in for the handoff's
// destructive-text uses of "coral" — see class doc) ----
val DocketErrorLight = Color(0xFFBA1A1A)
val DocketOnErrorLight = Color(0xFFFFFFFF)
val DocketErrorContainerLight = Color(0xFFFFDAD6)
val DocketOnErrorContainerLight = Color(0xFF410002)

val DocketErrorDark = Color(0xFFFFB4AB)
val DocketOnErrorDark = Color(0xFF690005)
val DocketErrorContainerDark = Color(0xFF93000A)
val DocketOnErrorContainerDark = Color(0xFFFFDAD6)

// ---- Neutral surfaces — ink/muted/line/tile from the handoff, mapped onto M3 roles. `tile`
// (#FAFAFA) maps to surfaceContainerLowest since that's the role Docket's shared row/card
// components (DocketListRow, FolderCard, ...) already read for their background. ----
val DocketBackgroundLight = Color(0xFFFFFFFF)
val DocketOnBackgroundLight = Color(0xFF212121) // ink
val DocketSurfaceLight = Color(0xFFFFFFFF)
val DocketOnSurfaceLight = Color(0xFF212121)
val DocketSurfaceVariantLight = Color(0xFFEEEEEE) // line
val DocketOnSurfaceVariantLight = Color(0xFF9E9E9E) // muted
val DocketOutlineLight = Color(0xFFBDBDBD)
val DocketOutlineVariantLight = Color(0xFFEEEEEE) // line
val DocketSurfaceContainerLowestLight = Color(0xFFFAFAFA) // tile
val DocketSurfaceContainerLowLight = Color(0xFFF5F5F5)
val DocketSurfaceContainerLight = Color(0xFFF0F0F0)
val DocketSurfaceContainerHighLight = Color(0xFFEEEEEE) // line
val DocketSurfaceContainerHighestLight = Color(0xFFE0E0E0)
val DocketInverseSurfaceLight = Color(0xFF212121)
val DocketInverseOnSurfaceLight = Color(0xFFFAFAFA)

val DocketBackgroundDark = Color(0xFF181818)
val DocketOnBackgroundDark = Color(0xFFFAFAFA)
val DocketSurfaceDark = Color(0xFF181818)
val DocketOnSurfaceDark = Color(0xFFFAFAFA)
val DocketSurfaceVariantDark = Color(0xFF2C2C2C) // line
val DocketOnSurfaceVariantDark = Color(0xFF9E9E9E) // muted (same value as light — the handoff
// keeps "muted" constant across themes, only bg/tile/line/text actually swap)
val DocketOutlineDark = Color(0xFF6B6B6B)
val DocketOutlineVariantDark = Color(0xFF2C2C2C) // line
val DocketSurfaceContainerLowestDark = Color(0xFF232323) // tile
val DocketSurfaceContainerLowDark = Color(0xFF262626)
val DocketSurfaceContainerDark = Color(0xFF2A2A2A)
val DocketSurfaceContainerHighDark = Color(0xFF2C2C2C) // line
val DocketSurfaceContainerHighestDark = Color(0xFF363636)
val DocketInverseSurfaceDark = Color(0xFFFAFAFA)
val DocketInverseOnSurfaceDark = Color(0xFF212121)

val DocketScrim = Color(0xFF181818)

// ---- Status (warranty urgency) — semantic, outside the M3 ColorScheme. Containers use the
// handoff's mintTint/amberTint exactly; the on-container tone is hand-darkened from the
// handoff's literal mint/amber so real chip text (days-remaining, "Warranty to <date>") clears
// WCAG AA — see class doc. "Urgent"/"Expired" still reuse MaterialTheme.colorScheme.error
// instead of a third custom role — see ExtendedColor.kt / WarrantyCard. ----
val DocketSafeLight = Color(0xFF0C7A5C)
val DocketOnSafeLight = Color(0xFFFFFFFF)
val DocketSafeContainerLight = Color(0xFFE3F8F1) // mintTint
val DocketOnSafeContainerLight = Color(0xFF0C7A5C)

val DocketSafeDark = Color(0xFF9BD4B3)
val DocketOnSafeDark = Color(0xFF00391D)
val DocketSafeContainerDark = Color(0xFF14512F)
val DocketOnSafeContainerDark = Color(0xFFB6F0CB)

val DocketWarningLight = Color(0xFF8A5300)
val DocketOnWarningLight = Color(0xFFFFFFFF)
val DocketWarningContainerLight = Color(0xFFFFF3E1) // amberTint
val DocketOnWarningContainerLight = Color(0xFF8A5300)

val DocketWarningDark = Color(0xFFFFB955)
val DocketOnWarningDark = Color(0xFF472A00)
val DocketWarningContainerDark = Color(0xFF663E00)
val DocketOnWarningContainerDark = Color(0xFFFFDDB0)

// ---- Decorative tool tones (coral/violet/taupe) — icon-only, always paired with an ink label;
// see class doc for why these skip the AA-darkening treatment safe/warning get. Dark variants
// are the handoff's own literal dark tool-tile backgrounds. ----
val DocketCoralLight = Color(0xFFFF6B67)
val DocketOnCoralLight = Color(0xFFFFFFFF)
val DocketCoralContainerLight = Color(0xFFFFECEB)
val DocketOnCoralContainerLight = Color(0xFFFF6B67)

val DocketCoralDark = Color(0xFFFF6B67)
val DocketOnCoralDark = Color(0xFF3A2323)
val DocketCoralContainerDark = Color(0xFF3A2323)
val DocketOnCoralContainerDark = Color(0xFFFF6B67)

val DocketVioletLight = Color(0xFF6C6BF0)
val DocketOnVioletLight = Color(0xFFFFFFFF)
val DocketVioletContainerLight = Color(0xFFECEAFD)
val DocketOnVioletContainerLight = Color(0xFF6C6BF0)

val DocketVioletDark = Color(0xFF6C6BF0)
val DocketOnVioletDark = Color(0xFF26264A)
val DocketVioletContainerDark = Color(0xFF26264A)
val DocketOnVioletContainerDark = Color(0xFF6C6BF0)

val DocketTaupeLight = Color(0xFFA1887F)
val DocketOnTaupeLight = Color(0xFFFFFFFF)
val DocketTaupeContainerLight = Color(0xFFF5EFEC)
val DocketOnTaupeContainerLight = Color(0xFFA1887F)

val DocketTaupeDark = Color(0xFFA1887F)
val DocketOnTaupeDark = Color(0xFF332B28)
val DocketTaupeContainerDark = Color(0xFF332B28)
val DocketOnTaupeContainerDark = Color(0xFFA1887F)

// ---- Home tool-grid's own amber/mint dark-container values — the handoff's literal dark
// tool-tile backgrounds. Kept separate from DocketSafeContainerDark/DocketWarningContainerDark
// above (which stay tuned for WarrantyCard's real chip text) rather than overloading one token
// for two different contrast needs — see class doc. ----
val DocketAmberToolContainerDark = Color(0xFF3A2F1C)
val DocketMintToolContainerDark = Color(0xFF173A30)

// ---- Raw decorative literals the handoff calls out by name but that have no other token at
// their exact saturated value — `warning`/`safe` above are hand-darkened for AA text contrast
// (see class doc), so a screen that needs the literal amber/mint fill (Onboarding's accent dot,
// Premium's star badge) reads these instead. Same "icon/decoration only, not text" caveat as
// coral/violet/taupe applies. Dark variant reuses the light value, same as coral/violet/taupe —
// these are decorative fills that don't need a separate dark-mode tone. ----
val DocketAmberLight = Color(0xFFFCAF4E)
val DocketAmberDark = Color(0xFFFCAF4E)
val DocketMintLight = Color(0xFF2ED3A5)
val DocketMintDark = Color(0xFF2ED3A5)

// ---- `ink2` — the handoff's tertiary-label tone, distinct from `muted`/onSurfaceVariant
// (#9E9E9E). Used sparingly (social-login glyph color, sheet secondary text). ----
val DocketInk2Light = Color(0xFF616161)
val DocketInk2Dark = Color(0xFF616161)

package com.docket.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Full Material 3 type scale, split across two families per the design system brief
 * ("one distinctive display typeface for headings and screen titles... system font for body
 * and UI labels"):
 *
 * - [DocketDisplayFontFamily] (serif) carries display/headline sizes and titleLarge — the
 *   sizes that actually render as a screen title or section heading. This is the single
 *   highest-leverage typographic change per the brief: a platform-default face on every
 *   heading is the strongest visual tell of an unfinished app. Serif also isn't a random
 *   pick — it echoes the "ledger/notary stamp" brand read the teal accent was chosen for
 *   (see Color.kt), not a generic display-face default.
 * - titleMedium/Small and everything below stay on the system font — these carry document
 *   titles, receipt lines, and dense reading UI, where multilingual glyph coverage
 *   (Latin/Devanagari/Arabic/Urdu) and small-size rendering quality matter more than
 *   personality. `FontFamily.Serif` is a generic family, not a bundled asset — Android's own
 *   font-fallback chain still substitutes full glyph coverage per-script for any text the
 *   serif face itself can't render, so RTL/Devanagari headings stay correct even though only
 *   Latin-script ones get the distinctive treatment. Zero added APK weight either way.
 *
 * Display/headline/title weights are bumped from M3's stock Normal to SemiBold/Medium with
 * slightly tightened tracking — the extra contrast against body text does most of the work of
 * making hierarchy legible at a glance.
 *
 * Body and label sizes stay bumped roughly one step up the scale (+2sp, line-height adjusted
 * to match) since these carry almost all of the app's actual reading text — receipt lines,
 * warranty details, list rows — and a meaningful slice of our users are reading in bad light
 * or are simply older.
 */
val DocketDisplayFontFamily = FontFamily.Serif

val DocketTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DocketDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 57.sp,
        lineHeight = 62.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = DocketDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 45.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = DocketDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DocketDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DocketDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DocketDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    ),
    // titleLarge is what TopAppBar renders as the screen title by default — the other half of
    // "headings and screen titles" gets the display face, same as headline/display above.
    titleLarge = TextStyle(
        fontFamily = DocketDisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    // --- System font from here down: row/list content (document titles, receipt merchants,
    // folder names) and all reading/UI text. Serif on every row would be the "overdesigned"
    // failure mode the brief warns against, not the restrained one. ---
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // --- Body — bumped one step up (M3 default: 16/14/12) for readability ---
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp
    ),
    // --- Labels — bumped to match (M3 default: 14/12/11); these carry button/chip/badge text ---
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    )
)

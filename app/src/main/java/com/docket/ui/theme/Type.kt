package com.docket.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.docket.R

/**
 * Urbanist, per the `design_handoff_docket_ui` handoff — bundled as a single variable-weight
 * font file (`res/font/urbanist_variable.ttf`, from Google Fonts, OFL-licensed — see
 * `licenses/urbanist_ofl_license.txt`) rather than a runtime Downloadable-Fonts fetch, so
 * rendering doesn't depend on Google Play Services being present and Docket's fully-offline
 * story stays intact. Each weight below is the same physical file loaded with a
 * [FontVariation.Settings] weight axis value — supported natively from API 26 up; on API
 * 24/25 (a vanishingly small and shrinking slice of real devices) the system falls back to the
 * file's default (Regular) instance rather than failing, so nothing crashes or goes unstyled,
 * headings just render a touch lighter there.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun urbanistFont(weight: FontWeight) = Font(
    resId = R.font.urbanist_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val DocketFontFamily = FontFamily(
    urbanistFont(FontWeight.Normal),
    urbanistFont(FontWeight.Medium),
    urbanistFont(FontWeight.SemiBold),
    urbanistFont(FontWeight.Bold),
    urbanistFont(FontWeight.ExtraBold)
)

/**
 * Full Material 3 type scale, mapped onto the handoff's typography table (one family, Urbanist,
 * throughout — display/headline weights go all the way to ExtraBold(800) per the handoff rather
 * than M3's stock Normal, since that weight jump is most of what the reference screens' headers
 * lean on for hierarchy).
 */
val DocketTypography = Typography(
    // Onboarding/auth headline (30/800) doesn't have a dedicated M3 slot at this exact size;
    // displaySmall is the closest role actually used as a full-bleed screen headline.
    displayLarge = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp, // Premium price display (44/800)
        lineHeight = 50.sp,
        letterSpacing = (-0.02).em(44)
    ),
    displayMedium = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.02).em(36)
    ),
    displaySmall = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp, // Auth headline ("Welcome back", "Create account", "Check your mail")
        lineHeight = 36.sp,
        letterSpacing = (-0.01).em(30)
    ),
    headlineLarge = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp, // Onboarding slide headline (28/800, line-height 1.25)
        lineHeight = 35.sp,
        letterSpacing = (-0.01).em(28)
    ),
    headlineMedium = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 23.sp, // Screen title ("Docket", "Files", "Account", "Premium")
        lineHeight = 28.sp,
        letterSpacing = (-0.01).em(23)
    ),
    headlineSmall = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 21.sp, // Sheet title ("Save document", "Share & export")
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp, // Section header ("Recent Files")
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    // --- Everything from here down is row/list content and UI text. ---
    titleMedium = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp, // Screen sub-title / detail app bar ("Review (2 pages)")
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp, // Settings row label, account name, primary button
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp, // List item title (15/700, see labelLarge) shares this size with body
        lineHeight = 24.sp, // Body/paragraph line-height 1.6
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, // Field text
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp, // Row meta / caption
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp, // List item title / primary button label
        lineHeight = 20.sp, // line-height 1.35
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp, // Tool label
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DocketFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp, // Tab label (700 when active — see DocketBottomNav)
        lineHeight = 14.sp,
        letterSpacing = 0.sp
    )
)

/** `-0.01em`/`-0.02em`-style tracking from the handoff, expressed in sp at a given font size
 *  (Compose letter-spacing is absolute, not font-relative, so this converts once per style). */
private fun Double.em(fontSizeSp: Int): androidx.compose.ui.unit.TextUnit = (this * fontSizeSp).sp

/**
 * Field label (13/700, muted) — the handoff's label style for underline-field forms (Sign
 * in/up, Save sheet's Folder/Format labels). Doesn't fit anywhere in [DocketTypography]'s M3
 * slots without colliding with an existing role (`labelSmall` is already spoken for as the
 * 11/500 tab label — see DocketBottomNav), so it lives as a standalone constant, same pattern
 * as [DocketPillShape] in Shape.kt.
 */
val DocketFieldLabelStyle = TextStyle(
    fontFamily = DocketFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.sp
)

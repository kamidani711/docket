package com.docket.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Sizing tokens that aren't spacing: touch targets and the recurring component dimensions
 * used across the design system.
 */
object DocketDimens {
    /** Minimum touch target for any tappable control (rows, icon buttons, chips). */
    val minTouchTarget: Dp = 48.dp

    /** Touch target for primary actions (PrimaryButton, main CTAs). */
    val primaryTouchTarget: Dp = 56.dp

    /** Standard leading icon/avatar size in list rows (FolderCard, ReceiptRow, WarrantyCard). */
    val rowIconSize: Dp = 44.dp

    /** Larger icon/avatar size for hero contexts (empty states, sheet headers). */
    val heroIconSize: Dp = 64.dp

    /** Default width for a DocumentThumbnail; height follows from its fixed page aspect ratio. */
    val thumbnailWidth: Dp = 96.dp

    /** Approximate page aspect ratio (width / height, ~3:4) used by DocumentThumbnail. */
    const val pageAspectRatio: Float = 0.75f
}

package com.docket.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's spacing scale: 4 / 8 / 12 / 16 / 24 / 32 / 48 / 64 / 96, all on the 8-point grid
 * (4 as the sole half-step for tight icon/text gaps). Use these instead of ad hoc `.dp` values
 * everywhere padding, gaps, or offsets are needed — that consistency is most of what makes a
 * layout actually look considered instead of just sparse. The two large steps (64/96) are for
 * section-level breathing room (hero headers, empty states) — everyday content sticks to 4–32.
 */
object DocketSpacing {
    val space4: Dp = 4.dp
    val space8: Dp = 8.dp
    val space12: Dp = 12.dp
    val space16: Dp = 16.dp
    val space20: Dp = 20.dp
    val space24: Dp = 24.dp
    val space32: Dp = 32.dp
    val space48: Dp = 48.dp
    val space64: Dp = 64.dp
    val space96: Dp = 96.dp
}

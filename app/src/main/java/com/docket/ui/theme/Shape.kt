package com.docket.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii. Per the design brief's "pick one radius value and never deviate": [medium]
 * (16dp) is *the* card radius — every document-shaped surface (list rows, page thumbnails,
 * library grid cards, the search field) uses it, full stop, rather than each picking its own.
 * The other steps are for genuinely different shape categories, not alternates for cards:
 * [extraSmall] for small status chips/badges, [small] for compact icon containers, [large] and
 * [extraLarge] for sheet-style surfaces where a bigger sweep reads correctly. Values stay on
 * the spacing rhythm (8/12/16/20/28) so rounding tracks the same grid as layout.
 */
val DocketShapes = Shapes(
    extraSmall = RoundedCornerShape(DocketSpacing.space8),
    small = RoundedCornerShape(DocketSpacing.space12),
    medium = RoundedCornerShape(DocketSpacing.space16),
    large = RoundedCornerShape(DocketSpacing.space20),
    extraLarge = RoundedCornerShape(28.dp)
)

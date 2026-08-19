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
 *
 * [DocketPillShape] is a second, deliberate exception alongside the chip/icon-container ones
 * above — buttons and standalone chips (mode selectors, folder chips) go fully stadium-shaped
 * rather than sharing the card radius, since a 16dp corner on a 56dp-tall button reads as a
 * rounded rectangle, not the confident pill shape the rest of the button styling (soft shadow,
 * press-scale) is going for.
 */
val DocketShapes = Shapes(
    extraSmall = RoundedCornerShape(DocketSpacing.space8),
    small = RoundedCornerShape(DocketSpacing.space12),
    medium = RoundedCornerShape(DocketSpacing.space16),
    large = RoundedCornerShape(DocketSpacing.space20),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Fully rounded "stadium" shape for buttons and standalone chips — see the class doc above. */
val DocketPillShape = RoundedCornerShape(50)

/** "Big card" radius (26dp) — sits between [DocketShapes.large] (20) and `.extraLarge` (28) on
 *  the spacing rhythm, but is genuinely its own category (only the Premium coral card uses it)
 *  rather than a general scale step, so it's a standalone constant like [DocketPillShape]. */
val DocketBigCardShape = RoundedCornerShape(26.dp)

/** Document-thumbnail radius (6dp) — the small 46×62 file-row thumbnail specifically (Files/Home
 *  list rows). Distinct from the card radius family above; other thumbnail-shaped surfaces
 *  (Review's larger page-preview tiles) intentionally keep the 16dp card radius instead. */
val DocketThumbnailShape = RoundedCornerShape(6.dp)

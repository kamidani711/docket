package com.docket.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * A soft, brand-tinted shadow — the alternative to a hard 1dp outline for "this is a raised
 * surface" cards (list rows, thumbnails, stat tiles). Tinting the shadow with the primary hue
 * instead of pure black keeps it feeling soft on both light and dark surfaces, per the
 * "match shadow color to the background" rule: a cool-teal shadow reads as depth, a flat gray
 * one reads as grime.
 */
@Composable
fun Modifier.docketCardShadow(
    elevation: Dp = DocketSpacing.space4,
    shape: Shape = RoundedCornerShape(DocketSpacing.space16)
): Modifier {
    val tint = MaterialTheme.colorScheme.primary
    return this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = tint.copy(alpha = 0.10f),
        spotColor = tint.copy(alpha = 0.18f)
    )
}

/** Slightly stronger version for elements meant to read as clearly "floating" (FABs, sheets). */
@Composable
fun Modifier.docketFloatingShadow(
    shape: Shape = RoundedCornerShape(DocketSpacing.space20)
): Modifier = docketCardShadow(elevation = DocketSpacing.space12, shape = shape)

/** Neutral (non-tinted) soft shadow for surfaces sitting on top of colored/dark hero areas. */
fun Modifier.docketNeutralShadow(
    elevation: Dp = DocketSpacing.space4,
    shape: Shape = RoundedCornerShape(DocketSpacing.space16)
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = Color.Black.copy(alpha = 0.08f),
    spotColor = Color.Black.copy(alpha = 0.16f)
)

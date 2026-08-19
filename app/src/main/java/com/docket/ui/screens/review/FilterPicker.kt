package com.docket.ui.screens.review

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docket.R
import com.docket.domain.model.ScanFilter
import com.docket.ui.theme.DocketOnSurfaceVariantLight
import com.docket.ui.theme.DocketPrimaryLight
import com.docket.ui.theme.DocketSurfaceContainerLowestDark

@Composable
fun FilterPicker(
    swatches: Map<ScanFilter, Bitmap>,
    selectedFilter: ScanFilter,
    onFilterSelected: (ScanFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(
            text = "FILTER",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = DocketOnSurfaceVariantLight,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScanFilter.entries.forEach { filter ->
                val isSelected = filter == selectedFilter
                val cardShape = RoundedCornerShape(14.dp)
                val cardBorderColor = if (isSelected) DocketPrimaryLight else Color.Transparent
                val cardBgColor = if (isSelected) DocketPrimaryLight.copy(alpha = 0.12f) else DocketSurfaceContainerLowestDark
                val textColor = if (isSelected) DocketPrimaryLight else Color.White

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(cardShape)
                        .background(cardBgColor)
                        .border(if (isSelected) 1.5.dp else 1.dp, cardBorderColor, cardShape)
                        .clickable { onFilterSelected(filter) }
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val swatchShape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(swatchShape)
                            .border(1.dp, FilterSwatchBorderColor, swatchShape)
                            .background(
                                when (filter) {
                                    ScanFilter.ORIGINAL -> Brush.linearGradient(OriginalSwatchGradient)
                                    ScanFilter.BLACK_AND_WHITE -> Brush.linearGradient(BlackAndWhiteSwatchGradient)
                                    ScanFilter.GREYSCALE -> Brush.linearGradient(GreyscaleSwatchGradient)
                                    ScanFilter.ENHANCE -> Brush.linearGradient(EnhanceSwatchGradient)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val swatch = swatches[filter]
                        if (swatch != null) {
                            Image(
                                bitmap = swatch.asImageBitmap(),
                                contentDescription = filter.label(),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Text(
                        text = filter.label(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// One-off decorative swatch colors — these preview each filter's *look*, not a semantic brand
// tone (the Enhance swatch's second stop happens to match the `mint` token's hex, but that's
// coincidence, not meaning — don't alias it to docketExtendedColors().mint). Not theme-reactive:
// the filter row lives on Review's always-dark chrome, so these values don't change with the
// app theme either.
private val FilterSwatchBorderColor = Color(0xFF333333)
private val OriginalSwatchGradient = listOf(Color(0xFFFAFAFA), Color(0xFFEEEEEE))
private val BlackAndWhiteSwatchGradient = listOf(Color(0xFF212121), Color(0xFF000000))
private val GreyscaleSwatchGradient = listOf(Color(0xFF757575), Color(0xFF424242))
private val EnhanceSwatchGradient = listOf(Color(0xFF5468F0), Color(0xFF2ED3A5))

@Composable
private fun ScanFilter.label(): String = when (this) {
    ScanFilter.ORIGINAL -> "Original"
    ScanFilter.BLACK_AND_WHITE -> "B & W"
    ScanFilter.GREYSCALE -> "Greyscale"
    ScanFilter.ENHANCE -> "Enhance"
}


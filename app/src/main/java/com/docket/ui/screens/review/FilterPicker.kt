package com.docket.ui.screens.review

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.docket.R
import com.docket.domain.model.ScanFilter
import com.docket.ui.theme.DocketSpacing

@Composable
fun FilterPicker(
    swatches: Map<ScanFilter, Bitmap>,
    selectedFilter: ScanFilter,
    onFilterSelected: (ScanFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space12),
        contentPadding = PaddingValues(horizontal = DocketSpacing.space20)
    ) {
        items(ScanFilter.entries, key = { it.name }) { filter ->
            val isSelected = filter == selectedFilter
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { onFilterSelected(filter) },
                    contentAlignment = Alignment.Center
                ) {
                    val swatch = swatches[filter]
                    if (swatch != null) {
                        Image(
                            bitmap = swatch.asImageBitmap(),
                            contentDescription = filter.label(),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.small)
                        )
                    } else {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
                Text(filter.label(), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ScanFilter.label(): String = when (this) {
    ScanFilter.ORIGINAL -> stringResource(R.string.filter_original)
    ScanFilter.BLACK_AND_WHITE -> stringResource(R.string.filter_black_and_white)
    ScanFilter.GREYSCALE -> stringResource(R.string.filter_greyscale)
    ScanFilter.ENHANCE -> stringResource(R.string.filter_enhance)
}

package com.docket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.docket.ui.icons.DocketIcons
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.DocketThumbnailShape

@Composable
fun DocketFileRow(
    title: String,
    meta: String,
    thumbnailPath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(DocketSpacing.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val thumbShape = DocketThumbnailShape
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 62.dp)
                    .clip(thumbShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, thumbShape)
            ) {
                if (thumbnailPath != null) {
                    AsyncImage(
                        model = thumbnailPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Theme-reactive (unlike Scan/Review's fixed-dark chrome) — this row follows
                    // the app theme, so its placeholder stripes must swap in dark mode too.
                    val lineColor = MaterialTheme.colorScheme.outlineVariant
                    val gapColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val stripeHeight = 6.dp.toPx()
                        val lineHeight = 2.dp.toPx()
                        var y = 0f
                        while (y < size.height) {
                            drawRect(
                                color = lineColor,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, y),
                                size = androidx.compose.ui.geometry.Size(size.width, lineHeight)
                            )
                            drawRect(
                                color = gapColor,
                                topLeft = androidx.compose.ui.geometry.Offset(0f, y + lineHeight),
                                size = androidx.compose.ui.geometry.Size(size.width, stripeHeight - lineHeight)
                            )
                            y += stripeHeight
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onShare != null) {
                    IconButton(onClick = onShare, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = DocketIcons.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (onMore != null) {
                    IconButton(onClick = onMore, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = DocketIcons.Dots,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

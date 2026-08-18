package com.docket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketCardShadow

/**
 * A single scanned page (or the cover of a multi-page document) in a grid. [thumbnailModel] is
 * anything Coil can load (file path, Uri, ...); leave it null to show the placeholder icon
 * before real thumbnails exist.
 */
@Composable
fun DocumentThumbnail(
    pageCount: Int,
    modifier: Modifier = Modifier,
    thumbnailModel: Any? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .width(DocketDimens.thumbnailWidth)
            .aspectRatio(DocketDimens.pageAspectRatio)
            .docketCardShadow(elevation = DocketSpacing.space4, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (thumbnailModel != null) {
            AsyncImage(
                model = thumbnailModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }

        if (pageCount > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(DocketSpacing.space4),
                color = Color.Black.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    text = pageCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = DocketSpacing.space8, vertical = 2.dp)
                )
            }
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            )
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(DocketSpacing.space4)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

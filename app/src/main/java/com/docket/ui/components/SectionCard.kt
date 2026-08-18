package com.docket.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketCardShadow
import java.util.Locale

/**
 * A titled group of related rows on one shadow-lifted card — the "grouped settings" convention
 * (iOS Settings, Material's own grouped-list pattern) used everywhere this app shows a flat
 * list of informational/nav rows (Settings, Backup, Privacy, Usage). [header] sits above the
 * card rather than inside it, in the app's one accent color, so a long scroll still reads as a
 * sequence of distinct sections instead of one undifferentiated wall of text.
 */
@Composable
fun DocketSectionCard(
    header: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
        if (header != null) {
            androidx.compose.material3.Text(
                text = header.uppercase(Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = DocketSpacing.space4)
            )
        }
        val shape = MaterialTheme.shapes.medium
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .docketCardShadow(elevation = DocketSpacing.space4, shape = shape),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier.padding(horizontal = DocketSpacing.space16, vertical = DocketSpacing.space8),
                verticalArrangement = Arrangement.spacedBy(DocketSpacing.space12),
                content = content
            )
        }
    }
}

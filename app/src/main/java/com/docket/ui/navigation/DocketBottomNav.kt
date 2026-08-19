package com.docket.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docket.ui.icons.DocketIcons
import com.docket.ui.theme.DocketSpacing

private data class TabSpec(val destination: Destination, val label: String, val icon: (Boolean) -> ImageVector)

private val TABS = listOf(
    TabSpec(Destination.Home, "Home") { filled -> DocketIcons.home(filled) },
    TabSpec(Destination.Files, "Files") { filled -> DocketIcons.folder(filled) },
    TabSpec(Destination.Unlock, "Premium") { filled -> DocketIcons.star(filled) },
    TabSpec(Destination.Account, "Account") { filled -> DocketIcons.user(filled) }
)

/**
 * The four persistent bottom-nav tabs — borderless, per the handoff, not a hard-edged M3
 * `NavigationBar`. Shown only on [Destination.TOP_LEVEL] routes; see `DocketApp`.
 */
@Composable
fun DocketBottomNav(currentDestination: Destination?, onNavigate: (Destination) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = DocketSpacing.space8)) {
            TABS.forEach { tab ->
                val selected = currentDestination == tab.destination
                val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp)
                        .clickable(onClick = { onNavigate(tab.destination) }),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = tab.icon(selected),
                        contentDescription = tab.label,
                        tint = color,
                        modifier = Modifier.padding(bottom = DocketSpacing.space4)
                    )
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium),
                        color = color
                    )
                }
            }
        }
    }
}

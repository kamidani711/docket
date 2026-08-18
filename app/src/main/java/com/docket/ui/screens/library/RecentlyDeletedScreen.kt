package com.docket.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.docket.domain.model.Document
import com.docket.ui.components.DocketListRow
import com.docket.ui.components.EmptyState
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.util.formatDisplayDate
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyDeletedScreen(
    onBack: () -> Unit,
    viewModel: RecentlyDeletedViewModel = hiltViewModel()
) {
    val documents by viewModel.documents.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recently Deleted") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (documents.isEmpty()) {
            EmptyState(
                title = "Nothing here",
                message = "Deleted documents stay here for 30 days before they're removed for good.",
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(DocketSpacing.space20),
                verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
            ) {
                item {
                    Text(
                        "Documents here are automatically deleted for good 30 days after being removed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = DocketSpacing.space8)
                    )
                }
                items(documents, key = { it.id }) { document ->
                    RecentlyDeletedRow(
                        document = document,
                        onRestore = { viewModel.restore(document.id) },
                        onDeleteForever = { viewModel.deleteForever(document.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentlyDeletedRow(document: Document, onRestore: () -> Unit, onDeleteForever: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    DocketListRow(
        leading = {},
        trailing = {
            Row {
                IconButton(onClick = onRestore) {
                    Icon(Icons.Filled.Restore, contentDescription = "Restore")
                }
                IconButton(
                    onClick = {
                        // Haptic on destructive confirmation — this tap is the confirmation,
                        // there's no separate dialog for an already-deleted document.
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDeleteForever()
                    }
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete forever", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    ) {
        Text(document.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(daysRemainingLabel(document.deletedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun daysRemainingLabel(deletedAt: Long?): String {
    if (deletedAt == null) return ""
    val elapsedDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - deletedAt)
    val remaining = (30 - elapsedDays).coerceAtLeast(0)
    val deletedLabel = formatDisplayDate(deletedAt)
    return "Deleted $deletedLabel · $remaining days left"
}

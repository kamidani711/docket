package com.docket.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import com.docket.domain.model.AnalyticsSummary
import com.docket.ui.components.DocketSectionCard
import com.docket.ui.components.DocketTextButton
import com.docket.ui.theme.DocketSpacing
import java.util.Locale

/**
 * Everything this screen shows comes from a Room table on this device — see
 * [com.docket.domain.repository.AnalyticsRepository]'s doc. Showing the raw counts here, with a
 * one-tap way to clear them, is the whole point: nothing hidden, nothing sent anywhere, nothing
 * the user can't immediately verify or wipe for themselves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit, viewModel: AnalyticsViewModel = hiltViewModel()) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    var showClearConfirm by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Usage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DocketSpacing.space20, vertical = DocketSpacing.space8),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
        ) {
            Text(
                "Counts of what you've done in Docket, kept entirely on this device. Nothing " +
                    "here is ever sent anywhere.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DocketSectionCard(header = "Activity") {
                StatRow("Scans started", summary.scansStarted.toString())
                StatRow("Documents saved", summary.documentsSaved.toString())
                StatRow("Exports completed", summary.exportsCompleted.toString())
                StatRow("Searches performed", summary.searchesPerformed.toString())
            }

            DocketSectionCard(header = "Reliability") {
                StatRow("Crash-free sessions", crashFreeLabel(summary))
            }

            if (summary.featureUsage.isNotEmpty()) {
                DocketSectionCard(header = "Feature usage") {
                    summary.featureUsage.entries.sortedByDescending { it.value }.forEach { (key, count) ->
                        StatRow(featureLabel(key), count.toString())
                    }
                }
            }

            DocketTextButton(
                text = "Clear usage data",
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear usage data?") },
            text = { Text("This removes every count above. It can't be undone.") },
            confirmButton = {
                DocketTextButton(
                    text = "Clear",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.clearAll()
                        showClearConfirm = false
                    }
                )
            },
            dismissButton = { DocketTextButton(text = "Cancel", onClick = { showClearConfirm = false }) }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun crashFreeLabel(summary: AnalyticsSummary): String {
    if (summary.totalSessions == 0) return "No sessions yet"
    val percent = (summary.crashFreeSessionRate * 100).let { String.format(Locale.getDefault(), "%.0f", it) }
    return "$percent% (${summary.cleanSessions} of ${summary.totalSessions})"
}

/** Internal event-detail keys -> what the user sees. Falls back to the raw key for anything not
 *  in this list yet, rather than silently dropping it, so a future feature tag added in code
 *  without a matching UI label still shows up as *something*. */
private fun featureLabel(key: String): String = when (key) {
    "receipt_tagged" -> "Receipts tagged"
    "warranty_set" -> "Warranties set"
    "backup_created" -> "Backups created"
    "csv_exported" -> "CSV exports"
    "app_lock_enabled" -> "App lock turned on"
    else -> key
}

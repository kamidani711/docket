package com.docket.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docket.R
import com.docket.ui.components.BrandHeader
import com.docket.ui.components.DocketFileRow
import com.docket.ui.components.EmptyState
import com.docket.ui.components.QuickAction
import com.docket.ui.components.QuickActionGrid
import com.docket.ui.icons.DocketIcons
import com.docket.ui.navigation.Destination
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketCardShadow
import com.docket.ui.theme.docketExtendedColors
import com.docket.ui.util.formatDisplayDateTime
import com.docket.ui.util.shareFiles

@Composable
fun HomeScreen(
    onNavigate: (Destination) -> Unit,
    onOpenDocument: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentDocuments by viewModel.recentDocuments.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShareFiles -> shareFiles(context, event.files, event.mimeType)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space12)) {
                HomeFab(icon = DocketIcons.Image, contentDescription = stringResource(R.string.scan_gallery_button)) {
                    onNavigate(Destination.ScanFlow(launch = Destination.ScanFlow.LAUNCH_GALLERY))
                }
                HomeFab(icon = DocketIcons.Camera, contentDescription = stringResource(R.string.home_scan_fab_desc)) {
                    onNavigate(Destination.ScanFlow())
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = DocketSpacing.space96)
        ) {
            item {
                BrandHeader(title = stringResource(R.string.home_title)) {
                    IconButton(onClick = { onNavigate(Destination.Files) }) {
                        Icon(
                            imageVector = DocketIcons.Search,
                            contentDescription = stringResource(R.string.common_search),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                QuickActionGrid(
                    actions = homeTools(onNavigate),
                    modifier = Modifier.padding(horizontal = DocketSpacing.space16, vertical = DocketSpacing.space4)
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(start = DocketSpacing.space24, end = DocketSpacing.space24, top = DocketSpacing.space20, bottom = 18.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = DocketSpacing.space24, end = DocketSpacing.space16, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.home_recent_files_header), style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { onNavigate(Destination.Files) }) {
                        Icon(
                            imageVector = DocketIcons.ArrowRight,
                            contentDescription = stringResource(R.string.home_see_all_desc),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (recentDocuments.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_message),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DocketSpacing.space24)
                    )
                }
            } else {
                items(recentDocuments, key = { it.id }) { document ->
                    DocketFileRow(
                        title = document.title,
                        meta = formatDisplayDateTime(document.createdAt),
                        thumbnailPath = document.thumbnailPath,
                        onClick = { onOpenDocument(document.id) },
                        onShare = { viewModel.shareDocument(document.id) },
                        onMore = { onOpenDocument(document.id) },
                        modifier = Modifier.padding(horizontal = DocketSpacing.space16, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeFab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    // Glow per the handoff's FAB spec (rgba(84,104,240,.45)) — stronger than docketCardShadow's
    // default card glow, so pass explicit alphas and zero out the stock tonal elevation shadow
    // underneath rather than stacking two shadows.
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
        modifier = Modifier.docketCardShadow(
            elevation = DocketSpacing.space12,
            shape = CircleShape,
            ambientAlpha = 0.24f,
            spotAlpha = 0.45f
        )
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
private fun homeTools(onNavigate: (Destination) -> Unit): List<QuickAction> {
    val extended = docketExtendedColors()
    return listOf(
        QuickAction(stringResource(R.string.home_tool_scan_doc), DocketIcons.Frame, extended.warningContainer, extended.onWarningContainer) { onNavigate(Destination.ScanFlow()) },
        QuickAction(stringResource(R.string.home_tool_receipt), DocketIcons.Receipt, extended.taupeContainer, extended.onTaupeContainer) { onNavigate(Destination.ScanFlow()) },
        QuickAction(stringResource(R.string.home_tool_id_card), DocketIcons.IdCard, extended.coralContainer, extended.onCoralContainer) { onNavigate(Destination.ScanFlow()) },
        QuickAction(stringResource(R.string.home_tool_import_pdf), DocketIcons.Pdf, extended.violetContainer, extended.onVioletContainer) {
            onNavigate(Destination.ScanFlow(launch = Destination.ScanFlow.LAUNCH_PDF))
        },
        QuickAction(stringResource(R.string.home_tool_warranties), DocketIcons.ShieldCheck, extended.coralContainer, extended.onCoralContainer) { onNavigate(Destination.Warranties) },
        QuickAction(stringResource(R.string.home_tool_protect), DocketIcons.Shield, extended.safeContainer, extended.onSafeContainer) { onNavigate(Destination.Files) },
        QuickAction(stringResource(R.string.home_tool_export_csv), DocketIcons.Csv, extended.warningContainer, extended.onWarningContainer) { onNavigate(Destination.Receipts) },
        QuickAction(stringResource(R.string.home_tool_all_tools), DocketIcons.Grid, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary) { onNavigate(Destination.Files) }
    )
}

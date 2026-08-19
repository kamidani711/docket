package com.docket.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.ExportFormat
import com.docket.ui.components.DocketSectionCard
import com.docket.ui.icons.DocketIcons
import com.docket.ui.theme.DocketSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPreferencesScreen(onBack: () -> Unit, viewModel: ScanPreferencesViewModel = hiltViewModel()) {
    val defaultExportFormat by viewModel.defaultExportFormat.collectAsStateWithLifecycle()
    val defaultLibrarySort by viewModel.defaultLibrarySort.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan preferences") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(DocketIcons.Back, contentDescription = "Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(DocketSpacing.space20),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space24)
        ) {
            DocketSectionCard(header = "Default export format") {
                Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                    FilterChip(
                        selected = defaultExportFormat == ExportFormat.PDF,
                        onClick = { viewModel.setDefaultExportFormat(ExportFormat.PDF) },
                        label = { Text("PDF") }
                    )
                    FilterChip(
                        selected = defaultExportFormat == ExportFormat.IMAGE_SET,
                        onClick = { viewModel.setDefaultExportFormat(ExportFormat.IMAGE_SET) },
                        label = { Text("Separate images") }
                    )
                }
            }
            DocketSectionCard(header = "Default library sort") {
                Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                    FilterChip(
                        selected = defaultLibrarySort == DocumentSort.DATE,
                        onClick = { viewModel.setDefaultLibrarySort(DocumentSort.DATE) },
                        label = { Text("Date") }
                    )
                    FilterChip(
                        selected = defaultLibrarySort == DocumentSort.NAME,
                        onClick = { viewModel.setDefaultLibrarySort(DocumentSort.NAME) },
                        label = { Text("Name") }
                    )
                    FilterChip(
                        selected = defaultLibrarySort == DocumentSort.SIZE,
                        onClick = { viewModel.setDefaultLibrarySort(DocumentSort.SIZE) },
                        label = { Text("Size") }
                    )
                }
            }
        }
    }
}

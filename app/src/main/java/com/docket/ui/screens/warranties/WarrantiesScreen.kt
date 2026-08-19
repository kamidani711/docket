package com.docket.ui.screens.warranties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.docket.domain.model.Warranty
import com.docket.ui.components.EmptyState
import com.docket.ui.components.WarrantyCard
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.util.formatDisplayDate
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantiesScreen(
    onBack: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    viewModel: WarrantyListViewModel = hiltViewModel()
) {
    val warranties by viewModel.warranties.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Warranties") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (warranties.isEmpty()) {
            EmptyState(
                title = "No warranties tracked",
                message = "Tag a scan as a receipt, then set a warranty on it to see it here.",
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(DocketSpacing.space20),
                verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
            ) {
                items(warranties, key = { it.id }) { warranty ->
                    WarrantyRow(warranty = warranty, onClick = { onOpenDocument(warranty.documentId) })
                }
            }
        }
    }
}

@Composable
private fun WarrantyRow(warranty: Warranty, onClick: () -> Unit) {
    WarrantyCard(
        itemName = warranty.itemName,
        expiryDateLabel = "Expires ${formatDate(warranty.expiryDate)}",
        daysRemaining = daysUntil(warranty.expiryDate),
        onClick = onClick
    )
}

private fun daysUntil(expiryMillis: Long): Int =
    TimeUnit.MILLISECONDS.toDays(expiryMillis - System.currentTimeMillis()).toInt()

private fun formatDate(epochMillis: Long): String = formatDisplayDate(epochMillis)

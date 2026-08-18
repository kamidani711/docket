package com.docket.ui.screens.designsystem

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.docket.ui.components.BottomSheetMenu
import com.docket.ui.components.DocketMenuAction
import com.docket.ui.components.DocketSearchBar
import com.docket.ui.components.DocketTextButton
import com.docket.ui.components.DocumentThumbnail
import com.docket.ui.components.EmptyState
import com.docket.ui.components.FolderCard
import com.docket.ui.components.PrimaryButton
import com.docket.ui.components.ReceiptRow
import com.docket.ui.components.SecondaryButton
import com.docket.ui.components.WarrantyCard
import com.docket.ui.theme.DocketPreviews
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.DocketTheme

/**
 * Live component gallery — every design-system component in one place, with runtime dark
 * mode / RTL toggles so you can actually flip them on a device instead of only trusting
 * Android Studio's static previews (those exist too, see the `@DocketPreviews` functions
 * below, driven by the same [DesignSystemGalleryContent]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSystemScreen(onBack: () -> Unit) {
    var forceDark by remember { mutableStateOf(false) }
    var forceRtl by remember { mutableStateOf(false) }

    DocketTheme(darkTheme = forceDark) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (forceRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Design System") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Surface(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ToggleRow(
                            label = "Dark theme",
                            checked = forceDark,
                            onCheckedChange = { forceDark = it }
                        )
                        ToggleRow(
                            label = "Right-to-left",
                            checked = forceRtl,
                            onCheckedChange = { forceRtl = it }
                        )
                        HorizontalDivider()
                        DesignSystemGalleryContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DocketSpacing.space16, vertical = DocketSpacing.space8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * The actual gallery, with no Scaffold/toggle chrome of its own — reused by
 * [DesignSystemScreen] and by the `@DocketPreviews` functions at the bottom of this file.
 */
@Composable
fun DesignSystemGalleryContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(DocketSpacing.space16),
        verticalArrangement = Arrangement.spacedBy(DocketSpacing.space32)
    ) {
        ButtonsSection()
        DocumentThumbnailSection()
        FolderCardSection()
        ReceiptRowSection()
        WarrantyCardSection()
        EmptyStateSection()
        SearchBarSection()
        BottomSheetMenuSection()
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space12)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun ButtonsSection() = GallerySection("Buttons") {
    Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
        PrimaryButton(text = "Save document", onClick = {}, modifier = Modifier.fillMaxWidth())
        PrimaryButton(text = "Saving…", onClick = {}, loading = true, modifier = Modifier.fillMaxWidth())
        PrimaryButton(text = "Save document", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        SecondaryButton(text = "Cancel", onClick = {}, modifier = Modifier.fillMaxWidth())
        SecondaryButton(text = "Cancel", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        DocketTextButton(text = "Skip for now", onClick = {})
    }
}

@Composable
private fun DocumentThumbnailSection() = GallerySection("Document Thumbnail") {
    Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space12)) {
        DocumentThumbnail(pageCount = 1, onClick = {})
        DocumentThumbnail(pageCount = 4, onClick = {})
        DocumentThumbnail(pageCount = 2, selected = true, onClick = {})
    }
}

@Composable
private fun FolderCardSection() = GallerySection("Folder Card") {
    Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
        FolderCard(name = "Taxes 2025", documentCount = 12, onClick = {})
        FolderCard(name = "Warranties", documentCount = 1, onClick = {})
    }
}

@Composable
private fun ReceiptRowSection() = GallerySection("Receipt Row") {
    Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
        ReceiptRow(merchant = "Fresh Mart Grocers", amount = "$42.18", date = "Aug 12, 2026", onClick = {})
        ReceiptRow(merchant = "City Electronics", amount = "$219.00", date = "Jul 30, 2026", onClick = {})
    }
}

@Composable
private fun WarrantyCardSection() = GallerySection("Warranty Card") {
    Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
        WarrantyCard(itemName = "Refrigerator", expiryDateLabel = "Expires Oct 2026", daysRemaining = 60, onClick = {})
        WarrantyCard(itemName = "Laptop", expiryDateLabel = "Expires Sep 2026", daysRemaining = 20, onClick = {})
        WarrantyCard(itemName = "Blender", expiryDateLabel = "Expires this week", daysRemaining = 5, onClick = {})
        WarrantyCard(itemName = "Headphones", expiryDateLabel = "Expired Aug 2026", daysRemaining = -3, onClick = {})
    }
}

@Composable
private fun EmptyStateSection() = GallerySection("Empty State") {
    EmptyState(
        title = "No documents yet",
        message = "Scan a receipt, warranty card, or any paper document to get started.",
        icon = Icons.Filled.Inbox,
        actionLabel = "Scan a document",
        onAction = {}
    )
}

@Composable
private fun SearchBarSection() = GallerySection("Search Bar") {
    var query by remember { mutableStateOf("") }
    DocketSearchBar(query = query, onQueryChange = { query = it }, placeholder = "Search documents")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetMenuSection() = GallerySection("Bottom Sheet Menu") {
    var showSheet by remember { mutableStateOf(false) }
    SecondaryButton(text = "Open document actions", onClick = { showSheet = true })
    if (showSheet) {
        BottomSheetMenu(
            onDismissRequest = { showSheet = false },
            actions = listOf(
                DocketMenuAction("Share", Icons.Filled.Share) { showSheet = false },
                DocketMenuAction("Rename", Icons.Filled.Edit) { showSheet = false },
                DocketMenuAction("Delete", Icons.Filled.Delete, destructive = true) { showSheet = false }
            )
        )
    }
}

// ---- Android Studio design-time previews: light/dark × LTR/RTL, same content as the screen ----

@DocketPreviews
@Composable
private fun DesignSystemGalleryPreview() {
    DocketTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                DesignSystemGalleryContent()
            }
        }
    }
}

package com.docket.ui.screens.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.docket.R
import com.docket.domain.model.Document
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.DocumentTypeFilter
import com.docket.ui.components.DocketListRow
import com.docket.ui.components.EmptyState
import com.docket.ui.components.PrimaryButton
import com.docket.ui.navigation.Destination
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketCardShadow
import com.docket.ui.util.formatDisplayDate
import com.docket.ui.util.shareFiles
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    onOpenDocument: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onNavigate: (Destination) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val folderDocumentCounts by viewModel.folderDocumentCounts.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val folderStack by viewModel.folderStack.collectAsStateWithLifecycle()
    val canCreateSubfolder by viewModel.canCreateSubfolder.collectAsStateWithLifecycle()
    val sortBy by viewModel.sortBy.collectAsStateWithLifecycle()
    val typeFilter by viewModel.typeFilter.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val selection by viewModel.selection.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showNewFolderPrompt by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    val movedToRecentlyDeletedOne = stringResource(R.string.library_moved_to_recently_deleted_one)
    val movedToRecentlyDeletedMany = stringResource(R.string.library_moved_to_recently_deleted_many)
    val undoLabel = stringResource(R.string.common_undo)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.UndoDelete -> {
                    val result = snackbarHostState.showSnackbar(
                        message = if (event.documentIds.size == 1) {
                            movedToRecentlyDeletedOne
                        } else {
                            movedToRecentlyDeletedMany.format(event.documentIds.size)
                        },
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.documentIds)
                }
                is LibraryEvent.ShareFiles -> shareFiles(context, event.files, event.mimeType)
                LibraryEvent.PremiumRequired -> onNavigate(Destination.Unlock)
            }
        }
    }

    val isSelectionMode = selection.isNotEmpty()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSelectionMode) {
                SelectionTopBar(
                    count = selection.size,
                    onClose = viewModel::clearSelection,
                    onDelete = { viewModel.deleteDocuments(selection.toList()) },
                    onShare = viewModel::shareSelected,
                    onMove = { viewModel.moveSelectedTo(folderStack.lastOrNull()) }
                )
            } else {
                LibraryTopBar(
                    title = stringResource(R.string.app_name),
                    canGoUp = folderStack.isNotEmpty(),
                    onUp = { viewModel.navigateUp() },
                    onOpenSearch = onOpenSearch,
                    sortBy = sortBy,
                    onSortChange = viewModel::setSortBy,
                    typeFilter = typeFilter,
                    onTypeFilterChange = viewModel::setTypeFilter,
                    viewMode = viewMode,
                    onViewModeChange = viewModel::setViewMode,
                    onNewFolder = { showNewFolderPrompt = true },
                    canCreateSubfolder = canCreateSubfolder,
                    onOpenRecentlyDeleted = onOpenRecentlyDeleted,
                    onOpenSettingsClick = { onNavigate(Destination.Settings) }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { onNavigate(Destination.Scan) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = DocketSpacing.space8,
                        pressedElevation = DocketSpacing.space4
                    )
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = stringResource(R.string.library_scan_a_document))
                }
            }
        }
    ) { innerPadding ->
        val isEmpty = folders.isEmpty() && documents.isEmpty()
        if (isEmpty) {
            EmptyState(
                title = stringResource(R.string.library_empty_title),
                message = stringResource(R.string.library_empty_message),
                illustration = { LibraryEmptyIllustration() },
                actionLabel = stringResource(R.string.library_scan_a_document),
                onAction = { onNavigate(Destination.Scan) },
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            )
        } else {
            Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                // Folders live in their own horizontal row now, separate from the document
                // grid/list below — a folder isn't a document and reading them as one
                // undifferentiated set was exactly the "unfinished" flatness being fixed here.
                if (folders.isNotEmpty()) {
                    FolderChipRow(
                        folders = folders,
                        documentCounts = folderDocumentCounts,
                        onOpenFolder = viewModel::openFolder
                    )
                }

                if (documents.isEmpty()) {
                    EmptyState(
                        title = stringResource(R.string.library_empty_title),
                        message = stringResource(R.string.library_empty_message),
                        illustration = { LibraryEmptyIllustration() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (viewMode == LibraryViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(DocketSpacing.space20),
                        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space12),
                        verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
                    ) {
                        itemsIndexed(documents, key = { _, document -> document.id }) { index, document ->
                            StaggeredEntry(index = index) {
                                LibraryDocumentCard(
                                    document = document,
                                    isSelected = document.id in selection,
                                    isSelectionMode = isSelectionMode,
                                    onOpen = { onOpenDocument(document.id) },
                                    onToggleSelect = { viewModel.toggleSelection(document.id) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(DocketSpacing.space20),
                        verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
                    ) {
                        items(documents, key = { it.id }) { document ->
                            DocumentListItem(
                                document = document,
                                isSelected = document.id in selection,
                                isSelectionMode = isSelectionMode,
                                onOpen = { onOpenDocument(document.id) },
                                onToggleSelect = { viewModel.toggleSelection(document.id) },
                                onSwipeDelete = { viewModel.deleteDocuments(listOf(document.id)) },
                                onShare = { viewModel.shareDocument(document.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderPrompt) {
        NewFolderPrompt(
            name = newFolderName,
            onNameChange = { newFolderName = it },
            onConfirm = {
                if (newFolderName.isNotBlank()) viewModel.createFolder(newFolderName.trim())
                newFolderName = ""
                showNewFolderPrompt = false
            },
            onDismiss = {
                newFolderName = ""
                showNewFolderPrompt = false
            }
        )
    }
}

/** First 8 items only, 30ms offset each — a list/grid that all snaps in at once reads as a
 *  template; a brief cascade reads as considered. Items beyond the cutoff (and anything that
 *  re-enters composition after being scrolled far away) appear immediately, no delay. */
@Composable
private fun StaggeredEntry(index: Int, content: @Composable () -> Unit) {
    if (index >= 8) {
        content()
        return
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 30L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 6 }
    ) {
        content()
    }
}

@Composable
private fun FolderChipRow(
    folders: List<com.docket.domain.model.Folder>,
    documentCounts: Map<Long, Int>,
    onOpenFolder: (Long) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DocketSpacing.space20, vertical = DocketSpacing.space8),
        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
    ) {
        items(folders, key = { it.id }) { folder ->
            Surface(
                onClick = { onOpenFolder(folder.id) },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                modifier = Modifier.docketCardShadow(elevation = DocketSpacing.space4, shape = RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = DocketSpacing.space12, vertical = DocketSpacing.space8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val count = documentCounts[folder.id] ?: 0
                    if (count > 0) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(
    title: String,
    canGoUp: Boolean,
    onUp: () -> Unit,
    onOpenSearch: () -> Unit,
    sortBy: DocumentSort,
    onSortChange: (DocumentSort) -> Unit,
    typeFilter: DocumentTypeFilter,
    onTypeFilterChange: (DocumentTypeFilter) -> Unit,
    viewMode: LibraryViewMode,
    onViewModeChange: (LibraryViewMode) -> Unit,
    onNewFolder: () -> Unit,
    canCreateSubfolder: Boolean,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenSettingsClick: () -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSortSubmenu by remember { mutableStateOf(false) }
    var showFilterSubmenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            if (canGoUp) {
                IconButton(onClick = onUp) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.library_up_desc))
                }
            }
        },
        actions = {
            IconButton(onClick = onOpenSearch) {
                Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.common_search))
            }
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.library_more_desc))
                }
                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (viewMode == LibraryViewMode.LIST) {
                                    stringResource(R.string.library_switch_to_grid)
                                } else {
                                    stringResource(R.string.library_switch_to_list)
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (viewMode == LibraryViewMode.LIST) Icons.Filled.GridView else Icons.AutoMirrored.Filled.ViewList,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            onViewModeChange(if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST)
                            showOverflowMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_sort_by, sortBy.label())) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
                        onClick = { showSortSubmenu = true }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_filter_by, typeFilter.label())) },
                        onClick = { showFilterSubmenu = true }
                    )
                    if (canCreateSubfolder) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.library_new_folder)) },
                            onClick = { onNewFolder(); showOverflowMenu = false }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_recently_deleted)) },
                        onClick = { onOpenRecentlyDeleted(); showOverflowMenu = false }
                    )
                    // Settings itself isn't part of the localized core flow yet (see the
                    // shipping-package weak-points list), so this stays plain English too,
                    // consistent with SettingsScreen's own hardcoded title rather than
                    // introducing a new inconsistency by localizing just the entry point to it.
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = { onOpenSettingsClick(); showOverflowMenu = false }
                    )
                }
                DropdownMenu(expanded = showSortSubmenu, onDismissRequest = { showSortSubmenu = false }) {
                    DocumentSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.library_sort_by, sort.label())) },
                            onClick = { onSortChange(sort); showSortSubmenu = false; showOverflowMenu = false }
                        )
                    }
                }
                DropdownMenu(expanded = showFilterSubmenu, onDismissRequest = { showFilterSubmenu = false }) {
                    DocumentTypeFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label()) },
                            onClick = { onTypeFilterChange(filter); showFilterSubmenu = false; showOverflowMenu = false }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun DocumentSort.label(): String = when (this) {
    DocumentSort.DATE -> stringResource(R.string.sort_date)
    DocumentSort.NAME -> stringResource(R.string.sort_name)
    DocumentSort.SIZE -> stringResource(R.string.sort_size)
}

@Composable
private fun DocumentTypeFilter.label(): String = when (this) {
    DocumentTypeFilter.ALL -> stringResource(R.string.filter_all)
    DocumentTypeFilter.PLAIN -> stringResource(R.string.filter_plain)
    DocumentTypeFilter.RECEIPT -> stringResource(R.string.filter_receipt)
    DocumentTypeFilter.WARRANTY -> stringResource(R.string.filter_warranty)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(count: Int, onClose: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit, onMove: () -> Unit) {
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.library_selected_count, count, count)) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.library_cancel_selection_desc))
            }
        },
        actions = {
            IconButton(onClick = onShare) { Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.common_share)) }
            IconButton(onClick = onMove) { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = stringResource(R.string.library_move_desc)) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete)) }
        }
    )
}

/**
 * The 2-column grid card — documents styled to read as documents: a white "sheet of paper" with
 * soft elevation and a page-like aspect ratio, not a flat icon-and-filename row. Long-press to
 * multi-select, same gesture as before.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryDocumentCard(
    document: Document,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onOpen: () -> Unit,
    onToggleSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() else onOpen() },
                onLongClick = onToggleSelect
            ),
        verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(DocketDimens.pageAspectRatio)
                .docketCardShadow(elevation = DocketSpacing.space4, shape = MaterialTheme.shapes.medium),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (document.thumbnailPath != null) {
                    AsyncImage(
                        model = document.thumbnailPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Type indicator: an icon, never a color chip — color alone doesn't survive
                // colorblindness or bright-sunlight outdoor use (see the design system doc).
                if (document.hasReceipt || document.hasWarranty) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(DocketSpacing.space8),
                        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space4)
                    ) {
                        if (document.hasReceipt) {
                            TypeBadge(icon = Icons.Filled.Receipt, contentDescription = stringResource(R.string.library_type_receipt_desc))
                        }
                        if (document.hasWarranty) {
                            TypeBadge(icon = Icons.Filled.Shield, contentDescription = stringResource(R.string.library_type_warranty_desc))
                        }
                    }
                }

                if (document.pageCount > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(DocketSpacing.space8),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = document.pageCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = DocketSpacing.space8, vertical = 2.dp)
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f))
                    )
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.library_selected_desc),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(DocketSpacing.space8)
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = document.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pluralStringResource(R.plurals.library_document_pages, document.pageCount, document.pageCount) +
                    " · " + formatDate(document.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TypeBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(50),
        shadowElevation = 1.dp
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(4.dp)
                .size(14.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DocumentListItem(
    document: Document,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onOpen: () -> Unit,
    onToggleSelect: () -> Unit,
    onSwipeDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = DocketSpacing.space16),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    ) {
        DocketListRow(
            modifier = Modifier.combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() else onOpen() },
                onLongClick = onToggleSelect
            ),
            leading = {
                if (isSelectionMode) {
                    Icon(
                        if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (isSelected) {
                            stringResource(R.string.library_selected_desc)
                        } else {
                            stringResource(R.string.library_not_selected_desc)
                        },
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(DocketDimens.rowIconSize)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        if (document.thumbnailPath != null) {
                            AsyncImage(
                                model = document.thumbnailPath,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Filled.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            trailing = {
                // Quick single-document share, no need to enter multi-select for the common
                // "just share this one thing" case -- multi-select's toolbar Share action
                // still covers sharing several at once.
                if (!isSelectionMode) {
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.library_document_actions_desc)
                            )
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_share)) },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                }
                            )
                        }
                    }
                }
            }
        ) {
            Text(document.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = pluralStringResource(R.plurals.library_document_pages, document.pageCount, document.pageCount) +
                    " · " + formatDate(document.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * A bottom sheet, not a dialog — per the component rule, dialogs are reserved for destructive
 * confirmation, and creating a folder is a plain contextual action like any other sheet in the
 * app (SaveSheet, ExportSheet, ...).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewFolderPrompt(name: String, onNameChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = DocketSpacing.space20)
                .padding(bottom = DocketSpacing.space32),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
        ) {
            Text(stringResource(R.string.library_new_folder), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(
                text = stringResource(R.string.library_create_folder),
                onClick = onConfirm,
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** A small hand-drawn stack of pages instead of a generic inbox glyph — this is one of the two
 *  empty states an actual user is likely to see (a brand-new install, or a fully-cleared
 *  folder), so it earns a real illustration rather than a stock icon. Built from plain Canvas
 *  primitives — no new dependency for something this simple. */
@Composable
private fun LibraryEmptyIllustration() {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = Modifier.size(96.dp)) {
        val pageSize = androidx.compose.ui.geometry.Size(size.width * 0.62f, size.height * 0.8f)
        val corner = CornerRadius(size.width * 0.06f)

        // Two back pages, offset and rotated slightly, then the front page on top.
        rotate(degrees = -8f, pivot = center) {
            drawRoundRect(
                color = outline.copy(alpha = 0.5f),
                topLeft = androidx.compose.ui.geometry.Offset((size.width - pageSize.width) / 2, (size.height - pageSize.height) / 2),
                size = pageSize,
                cornerRadius = corner,
                style = Fill
            )
        }
        rotate(degrees = 6f, pivot = center) {
            drawRoundRect(
                color = outline.copy(alpha = 0.7f),
                topLeft = androidx.compose.ui.geometry.Offset((size.width - pageSize.width) / 2, (size.height - pageSize.height) / 2),
                size = pageSize,
                cornerRadius = corner,
                style = Fill
            )
        }
        val frontTopLeft = androidx.compose.ui.geometry.Offset((size.width - pageSize.width) / 2, (size.height - pageSize.height) / 2)
        drawRoundRect(
            color = Color.White,
            topLeft = frontTopLeft,
            size = pageSize,
            cornerRadius = corner,
            style = Fill
        )
        drawRoundRect(
            color = outline,
            topLeft = frontTopLeft,
            size = pageSize,
            cornerRadius = corner,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
        // A couple of faint text lines on the front page.
        val lineX = frontTopLeft.x + pageSize.width * 0.18f
        val lineWidth = pageSize.width * 0.64f
        listOf(0.32f, 0.44f, 0.56f).forEach { fraction ->
            drawLine(
                color = outline,
                start = androidx.compose.ui.geometry.Offset(lineX, frontTopLeft.y + pageSize.height * fraction),
                end = androidx.compose.ui.geometry.Offset(lineX + lineWidth, frontTopLeft.y + pageSize.height * fraction),
                strokeWidth = 2.dp.toPx()
            )
        }
        // A small accent mark (the one place the accent color is allowed to show up here —
        // it's standing in for "detected/scanned", a functional signal, not decoration).
        drawCircle(
            color = primary,
            radius = pageSize.width * 0.09f,
            center = androidx.compose.ui.geometry.Offset(frontTopLeft.x + pageSize.width * 0.82f, frontTopLeft.y + pageSize.height * 0.78f)
        )
    }
}

private fun formatDate(epochMillis: Long): String = formatDisplayDate(epochMillis)

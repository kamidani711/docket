package com.docket.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.docket.R
import com.docket.domain.model.SearchResult
import com.docket.ui.components.DocketListRow
import com.docket.ui.components.DocketSearchBar
import com.docket.ui.components.EmptyState
import com.docket.ui.theme.DocketSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onResultSelected: (documentId: Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.common_search)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            DocketSearchBar(
                query = query,
                onQueryChange = viewModel::onQueryChanged,
                placeholder = stringResource(R.string.search_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DocketSpacing.space20, vertical = DocketSpacing.space8)
            )

            when {
                query.isBlank() -> EmptyState(
                    title = stringResource(R.string.search_empty_title),
                    message = stringResource(R.string.search_empty_message),
                    icon = Icons.Filled.Search,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                isSearching -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                results.isEmpty() -> EmptyState(
                    title = stringResource(R.string.search_no_results_title),
                    message = stringResource(R.string.search_no_results_message, query),
                    icon = Icons.Filled.Search,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(DocketSpacing.space20),
                    verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
                ) {
                    items(results, key = { "${it.documentId}_${it.pageIndex}" }) { result ->
                        SearchResultRow(result = result, onClick = { onResultSelected(result.documentId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: SearchResult, onClick: () -> Unit) {
    DocketListRow(
        onClick = onClick,
        leading = {
            Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) {
        Text(
            text = result.documentTitle,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.search_page_label, result.pageIndex + 1),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = highlightedSnippet(result),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Splits the FTS `snippet()` output on its control-character match markers and re-assembles
 *  it as an [AnnotatedString] with the matched portions bolded/tinted. */
@Composable
private fun highlightedSnippet(result: SearchResult): AnnotatedString {
    val highlightStyle = SpanStyle(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    return remember(result) {
        buildAnnotatedString {
            var remaining = result.snippetText
            while (true) {
                val startIndex = remaining.indexOf(result.snippetMatchStart)
                if (startIndex == -1) {
                    append(remaining)
                    break
                }
                append(remaining.substring(0, startIndex))
                remaining = remaining.substring(startIndex + result.snippetMatchStart.length)

                val endIndex = remaining.indexOf(result.snippetMatchEnd)
                if (endIndex == -1) {
                    append(remaining)
                    break
                }
                withStyle(highlightStyle) { append(remaining.substring(0, endIndex)) }
                remaining = remaining.substring(endIndex + result.snippetMatchEnd.length)
            }
        }
    }
}

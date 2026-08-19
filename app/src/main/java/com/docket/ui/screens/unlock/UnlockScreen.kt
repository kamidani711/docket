package com.docket.ui.screens.unlock

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.docket.R
import com.docket.ui.components.DocketSectionCard
import com.docket.ui.components.DocketTextButton
import com.docket.ui.components.PrimaryButton
import com.docket.ui.theme.DocketSpacing

/**
 * The only place Premium is ever pitched. Reached exclusively by tapping a Premium-gated
 * feature (see the various `onOpenPremium`/`onUnlockPremiumClick` call sites) or from Settings —
 * never shown unprompted, never blocks anything else, dismisses with one tap of the back arrow
 * like every other screen. No countdown, no "limited time", no repeated nagging: just what
 * Premium adds and a price, once, when the user actually asked to see it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(onBack: () -> Unit, viewModel: UnlockViewModel = hiltViewModel()) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity

    val benefits = listOf(
        stringResource(R.string.unlock_benefit_batch_export),
        stringResource(R.string.unlock_benefit_text_layer),
        stringResource(R.string.unlock_benefit_languages),
        stringResource(R.string.unlock_benefit_folders)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.unlock_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
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
                .padding(DocketSpacing.space20),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space24)
        ) {
            // A quiet badge above the pitch — same neutral-circle-plus-accent-icon language as
            // EmptyState and the "built in" language row, not a one-off flourish. The accent
            // shows up on the icon only, same restraint rule as everywhere else in the app.
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            if (isPremium) {
                Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                    Text(stringResource(R.string.unlock_owned_title), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.unlock_owned_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                    Text(stringResource(R.string.unlock_title_cta), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.unlock_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DocketSectionCard {
                benefits.forEach { benefit ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(benefit, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (!isPremium) {
                PrimaryButton(
                    text = when {
                        isBusy -> stringResource(R.string.unlock_button_working)
                        price != null -> stringResource(R.string.unlock_button_with_price, price ?: "")
                        else -> stringResource(R.string.unlock_title_cta)
                    },
                    onClick = { activity?.let(viewModel::purchase) },
                    enabled = !isBusy && activity != null,
                    loading = isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DocketTextButton(
                text = stringResource(R.string.unlock_restore),
                onClick = viewModel::restorePurchases,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            )

            message?.let { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

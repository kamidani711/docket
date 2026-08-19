package com.docket.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.docket.BuildConfig
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.LanguagePackState
import com.docket.domain.model.LanguagePackStatus
import com.docket.domain.model.StorageBreakdown
import com.docket.ui.components.DocketSectionCard
import com.docket.ui.components.DocketTextButton
import com.docket.ui.components.SecondaryButton
import com.docket.ui.lock.BiometricAuthenticator
import com.docket.ui.theme.DocketSpacing
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenDesignSystem: () -> Unit,
    onOpenPipelineDebug: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val languagePacks by viewModel.languagePacks.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val installingLanguage by viewModel.installingLanguage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val defaultExportFormat by viewModel.defaultExportFormat.collectAsStateWithLifecycle()
    val defaultLibrarySort by viewModel.defaultLibrarySort.collectAsStateWithLifecycle()
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val storageBreakdown by viewModel.storageBreakdown.collectAsStateWithLifecycle()
    val isClearingCache by viewModel.isClearingCache.collectAsStateWithLifecycle()

    // BiometricPrompt needs a live FragmentActivity (see BiometricAuthenticator) — MainActivity
    // is one, so this cast is safe everywhere this screen is actually reachable from.
    val activity = LocalContext.current as? FragmentActivity
    val biometricAuthenticator = remember(activity) { activity?.let(::BiometricAuthenticator) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space24)
        ) {
            DocketSectionCard(header = "Premium") {
                SettingsNavRow(
                    title = if (isPremium) "Premium unlocked" else "Unlock Premium",
                    subtitle = if (isPremium) {
                        "Thanks for supporting Docket — everything's unlocked for good."
                    } else {
                        "Batch export, searchable PDF text, extra OCR languages, unlimited folders."
                    },
                    onClick = onOpenPremium
                )
            }

            DocketSectionCard(header = "OCR languages") {
                Text(
                    "Latin is built in and always available. Additional scripts download on " +
                        "demand (about 25MB each) once Premium is unlocked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = DocketSpacing.space4)
                )

                languagePacks.forEachIndexed { index, packState ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    LanguagePackRow(
                        state = packState,
                        isPremium = isPremium,
                        isInstalling = installingLanguage == packState.language,
                        onInstall = {
                            if (isPremium) viewModel.installLanguage(packState.language) else onOpenPremium()
                        },
                        onRemove = { viewModel.removeLanguage(packState.language) }
                    )
                }

                errorMessage?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }

            DocketSectionCard(header = "Defaults") {
                Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                    Text("Export format", style = MaterialTheme.typography.titleMedium)
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
                Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                    Text("Library sort", style = MaterialTheme.typography.titleMedium)
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

            DocketSectionCard(header = "App lock") {
                AppLockSection(
                    enabled = appLockEnabled,
                    authenticator = biometricAuthenticator,
                    onSetEnabled = viewModel::setAppLockEnabled
                )
            }

            DocketSectionCard(header = "Storage") {
                StorageSection(
                    breakdown = storageBreakdown,
                    isClearing = isClearingCache,
                    onClearCache = viewModel::clearCache
                )
            }

            DocketSectionCard(header = "Data") {
                SettingsNavRow(
                    title = "Backup & restore",
                    subtitle = "Encrypted export to a file you choose. Nothing leaves your device unless you send it.",
                    onClick = onOpenBackup
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsNavRow(
                    title = "Privacy",
                    subtitle = "What Docket does — and doesn't — do with your data.",
                    onClick = onOpenPrivacy
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsNavRow(
                    title = "Usage",
                    subtitle = "Local counts of what you've used Docket for — never sent anywhere.",
                    onClick = onOpenAnalytics
                )
            }

            // Debug-only QA tool (light/dark × LTR/RTL component gallery) — one direct route
            // reference, not a dynamically-built list of destinations. That distinction matters:
            // it's exactly the pattern that avoided the circular-init crash the dev-nav list
            // used to hit (see LibraryScreen.kt's history) — a single object reference has
            // nothing to be circular with. BuildConfig.DEBUG is compile-time constant-folded by
            // R8 in a release build, so this row is stripped from what actually ships.
            if (BuildConfig.DEBUG) {
                DocketSectionCard(header = "Debug") {
                    SettingsNavRow(
                        title = "Design system (debug)",
                        subtitle = "Component gallery — light/dark, LTR/RTL.",
                        onClick = onOpenDesignSystem
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsNavRow(
                        title = "Pipeline debug",
                        subtitle = "Inspect each enhancement stage on a real picked image, with timing.",
                        onClick = onOpenPipelineDebug
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(DocketSpacing.space8))
        }
    }
}

@Composable
private fun AppLockSection(
    enabled: Boolean,
    authenticator: BiometricAuthenticator?,
    onSetEnabled: (Boolean) -> Unit
) {
    var confirmError by remember { mutableStateOf<String?>(null) }
    val canUseBiometrics = authenticator?.isAvailable() == true

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Require unlock to open Docket", style = MaterialTheme.typography.bodyLarge)
            Text(
                if (canUseBiometrics) {
                    "Uses your fingerprint, face, or device PIN — nothing new to set up."
                } else {
                    "Set up a screen lock (PIN, pattern, fingerprint, or face) in your device " +
                        "settings first."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = enabled,
            enabled = canUseBiometrics || enabled,
            onCheckedChange = { checked ->
                if (!checked) {
                    // Reaching this screen with the lock on already required a successful
                    // unlock (see AppLockGate) — no need to re-confirm just to turn it off.
                    onSetEnabled(false)
                } else {
                    // Confirm the device's biometrics/PIN actually work before committing to
                    // locking the user out with them — an untested method turned on here would
                    // be exactly the kind of thing that locks someone out of their own documents.
                    confirmError = null
                    authenticator?.authenticate(
                        onSuccess = { onSetEnabled(true) },
                        onError = { message -> confirmError = message }
                    )
                }
            }
        )
    }
    confirmError?.let { message ->
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun StorageSection(breakdown: StorageBreakdown?, isClearing: Boolean, onClearCache: () -> Unit) {
    if (breakdown == null) {
        Text(
            "Calculating…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        StorageRow("Documents", breakdown.documentsBytes)
        StorageRow("Database", breakdown.databaseBytes)
        StorageRow("Cache", breakdown.cacheBytes)
        HorizontalDivider()
        StorageRow("Total", breakdown.totalBytes, emphasize = true)
    }
    SecondaryButton(
        text = if (isClearing) "Clearing…" else "Clear cache",
        onClick = onClearCache,
        enabled = !isClearing && (breakdown?.cacheBytes ?: 0L) > 0L,
        loading = isClearing
    )
}

@Composable
private fun StorageRow(label: String, bytes: Long, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
        )
        Text(
            formatBytes(bytes),
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (emphasize) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}

@Composable
private fun SettingsNavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = DocketSpacing.space8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LanguagePackRow(
    state: LanguagePackState,
    isPremium: Boolean,
    isInstalling: Boolean,
    onInstall: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DocketSpacing.space8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(state.language.displayName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = when {
                    state.language.alwaysAvailable -> "Built in"
                    state.status == LanguagePackStatus.INSTALLED -> "Installed"
                    else -> "~${state.language.approxDownloadMb} MB"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            state.language.alwaysAvailable -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Built in",
                tint = MaterialTheme.colorScheme.primary
            )
            isInstalling -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            state.status == LanguagePackStatus.INSTALLED -> DocketTextButton(text = "Remove", onClick = onRemove)
            else -> SecondaryButton(
                text = "Install",
                onClick = onInstall,
                leadingIcon = if (!isPremium) Icons.Filled.Lock else null
            )
        }
    }
}

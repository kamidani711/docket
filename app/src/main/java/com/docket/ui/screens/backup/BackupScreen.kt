package com.docket.ui.screens.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import com.docket.ui.components.DocketSectionCard
import com.docket.ui.components.PrimaryButton
import com.docket.ui.theme.DocketSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    var exportPassphrase by remember { mutableStateOf("") }
    var exportPassphraseConfirm by remember { mutableStateOf("") }
    var importPassphrase by remember { mutableStateOf("") }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { viewModel.createBackup(it, exportPassphrase) } }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.restoreBackup(it, importPassphrase) } }

    val isBusy = state is BackupUiState.InProgress

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
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
            Text(
                "By default, nothing leaves this device. Backup is entirely optional: you " +
                    "choose the file, you choose where it goes — your own Drive, an SD card, " +
                    "anywhere. We never see it, never upload it, and can't recover it for you " +
                    "if you lose the passphrase.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DocketSectionCard(header = "Back up") {
                OutlinedTextField(
                    value = exportPassphrase,
                    onValueChange = { exportPassphrase = it },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = exportPassphraseConfirm,
                    onValueChange = { exportPassphraseConfirm = it },
                    label = { Text("Confirm passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
                val exportReady = exportPassphrase.length >= 6 && exportPassphrase == exportPassphraseConfirm
                PrimaryButton(
                    text = "Choose where to save",
                    onClick = { createDocumentLauncher.launch("docket-backup.dbk") },
                    enabled = exportReady && !isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
                if (exportPassphrase.isNotEmpty() && exportPassphrase != exportPassphraseConfirm) {
                    Text("Passphrases don't match yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            DocketSectionCard(header = "Restore") {
                Text(
                    "Restoring replaces everything currently in Docket on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = importPassphrase,
                    onValueChange = { importPassphrase = it },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton(
                    text = "Choose backup file",
                    onClick = { openDocumentLauncher.launch(arrayOf("*/*")) },
                    enabled = importPassphrase.isNotEmpty() && !isBusy,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (val current = state) {
                is BackupUiState.InProgress -> {
                    CircularProgressIndicator()
                    Text(current.message, style = MaterialTheme.typography.bodyMedium)
                }
                is BackupUiState.BackupComplete -> Text(
                    "Backup saved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                is BackupUiState.RestoreStaged -> Text(
                    "Import ready — close Docket completely and reopen it to finish.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                is BackupUiState.Failed -> Text(
                    current.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                BackupUiState.Idle -> Unit
            }
        }
    }
}

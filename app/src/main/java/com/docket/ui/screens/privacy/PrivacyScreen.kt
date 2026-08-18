package com.docket.ui.screens.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.docket.ui.components.DocketSectionCard
import com.docket.ui.theme.DocketSpacing

/**
 * Plain-language privacy explanation — not a legal privacy policy (that's a separate, static
 * document shipped for the Play Store listing; see the Play Store package write-up), but the
 * in-app answer to "what does this app actually do with my stuff". Every claim here has to stay
 * true to the actual code: no network calls carrying document content or usage data, ever,
 * except the one isolated, clearly-labeled exception for Play Billing purchase verification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy") },
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
            PrivacySection(
                title = "Your documents never leave this device",
                body = "Scanning, text recognition, receipt parsing, and search all run " +
                    "entirely on your phone. Docket has no server, no cloud sync, and no " +
                    "account to sign into — there's nowhere for your documents to go even " +
                    "if you wanted to send them there."
            )
            PrivacySection(
                title = "No tracking, no ads, no analytics sent anywhere",
                body = "Docket doesn't use any third-party analytics or advertising SDK. The " +
                    "app does keep a small local log of what you use it for — scans, saves, " +
                    "searches — so it can show you your own usage in Settings. That log " +
                    "stays in a database on your phone, is never transmitted, and you can " +
                    "clear it at any time."
            )
            PrivacySection(
                title = "Backup is entirely optional, and entirely yours",
                body = "If you choose to back up your library, you pick the file and the " +
                    "destination — your own Drive, an SD card, anywhere your phone can write " +
                    "a file. The backup is encrypted with a passphrase only you know; Docket " +
                    "never sees or stores it. We can't read your backups, and we can't " +
                    "recover them for you if the passphrase is lost."
            )
            PrivacySection(
                title = "The one thing that does involve Google",
                body = "Unlocking Premium goes through Google Play Billing to verify your " +
                    "purchase. Docket itself has no internet permission and makes no network " +
                    "calls of its own, ever — the purchase check happens inside the Play " +
                    "Store app already running on your phone, not inside Docket. It carries " +
                    "nothing but the purchase check itself, and only runs when you choose to " +
                    "unlock or restore a purchase."
            )
            PrivacySection(
                title = "Permissions, and why each one exists",
                body = "Camera: to scan documents. Notifications: to remind you before a " +
                    "warranty expires. Nothing else is requested, and nothing here requires " +
                    "internet access to work — Premium is the sole exception, described above."
            )
        }
    }
}

@Composable
private fun PrivacySection(title: String, body: String) {
    DocketSectionCard {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

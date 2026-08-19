package com.docket.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docket.ui.components.DocketSectionCard
import com.docket.ui.icons.DocketIcons
import com.docket.ui.lock.BiometricAuthenticator
import com.docket.ui.theme.DocketSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(onBack: () -> Unit, viewModel: SecurityViewModel = hiltViewModel()) {
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? FragmentActivity
    val biometricAuthenticator = remember(activity) { activity?.let(::BiometricAuthenticator) }
    var confirmError by remember { mutableStateOf<String?>(null) }
    val canUseBiometrics = biometricAuthenticator?.isAvailable() == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(DocketIcons.Back, contentDescription = "Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(DocketSpacing.space20),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
        ) {
            DocketSectionCard(header = "App lock") {
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
                                "Set up a screen lock (PIN, pattern, fingerprint, or face) in your device settings first."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = appLockEnabled,
                        enabled = canUseBiometrics || appLockEnabled,
                        onCheckedChange = { checked ->
                            if (!checked) {
                                // Reaching this screen with the lock on already required a
                                // successful unlock (see AppLockGate) — no need to re-confirm
                                // just to turn it off.
                                viewModel.setAppLockEnabled(false)
                            } else {
                                // Confirm the device's biometrics/PIN actually work before
                                // committing to locking the user out with them.
                                confirmError = null
                                biometricAuthenticator?.authenticate(
                                    onSuccess = { viewModel.setAppLockEnabled(true) },
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
        }
    }
}

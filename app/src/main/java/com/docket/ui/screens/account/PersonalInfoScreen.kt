package com.docket.ui.screens.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docket.ui.components.DocketTextButton
import com.docket.ui.components.DocketUnderlineField
import com.docket.ui.components.PrimaryButton
import com.docket.ui.components.SecondaryButton
import com.docket.ui.icons.DocketIcons
import com.docket.ui.screens.profile.ProfileViewModel
import com.docket.ui.theme.DocketSpacing

/** Edit the local profile's name/email, or delete it entirely — see [ProfileViewModel]. If no
 *  profile exists yet, this doubles as a lightweight Sign-up entry point (same underlying
 *  operation). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var name by remember(profile) { mutableStateOf(profile?.displayName.orEmpty()) }
    var email by remember(profile) { mutableStateOf(profile?.email.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var confirmingDelete by remember { mutableStateOf(false) }
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (profile != null) onBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(DocketIcons.Back, contentDescription = "Back") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(DocketSpacing.space20),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space20)
        ) {
            DocketUnderlineField(value = name, onValueChange = { name = it }, label = "Full name")
            DocketUnderlineField(value = email, onValueChange = { email = it }, label = "Email", keyboardType = KeyboardType.Email)
            if (profile == null) {
                DocketUnderlineField(value = password, onValueChange = { password = it }, label = "Password", isPassword = true)
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            PrimaryButton(
                text = "Save",
                onClick = {
                    if (profile == null) viewModel.signUp(name, email, password) else viewModel.updatePersonalInfo(name, email)
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (profile != null) {
                if (confirmingDelete) {
                    Text(
                        "Delete this profile? Your documents aren't affected — only the local " +
                            "name/email/password are removed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    SecondaryButton(text = "Confirm delete", onClick = viewModel::deleteProfile, modifier = Modifier.fillMaxWidth())
                } else {
                    DocketTextButton(text = "Delete profile", onClick = { confirmingDelete = true })
                }
            }
        }
    }
}

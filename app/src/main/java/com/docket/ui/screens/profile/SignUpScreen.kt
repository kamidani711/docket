package com.docket.ui.screens.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docket.R
import com.docket.ui.components.DocketCheckbox
import com.docket.ui.components.DocketUnderlineField
import com.docket.ui.components.PrimaryButton
import com.docket.ui.icons.DocketIcons
import com.docket.ui.theme.DocketSpacing

@Composable
fun SignUpScreen(
    onBack: () -> Unit,
    onSignedUp: () -> Unit,
    onOpenSignIn: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { onSignedUp() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DocketSpacing.space24)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(top = DocketSpacing.space16)) {
                Icon(imageVector = DocketIcons.Back, contentDescription = stringResource(R.string.common_back))
            }
            Text(
                stringResource(R.string.signup_headline),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
            )
            Text(
                stringResource(R.string.signup_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 28.dp)
            )
            DocketUnderlineField(value = name, onValueChange = { name = it }, label = stringResource(R.string.signup_name_label))
            Spacer(modifier = Modifier.padding(top = 4.dp))
            DocketUnderlineField(value = email, onValueChange = { email = it }, label = stringResource(R.string.signup_email_label), keyboardType = KeyboardType.Email)
            Spacer(modifier = Modifier.padding(top = 4.dp))
            DocketUnderlineField(value = password, onValueChange = { password = it }, label = stringResource(R.string.signup_password_label), isPassword = true)
            DocketCheckbox(
                checked = agreed,
                onCheckedChange = { agreed = it },
                modifier = Modifier.padding(top = DocketSpacing.space16)
            ) {
                Text(
                    stringResource(R.string.signup_consent),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(R.string.signup_button),
                onClick = { viewModel.signUp(name, email, password) },
                loading = isBusy,
                enabled = agreed && !isBusy,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DocketSpacing.space16, bottom = DocketSpacing.space32),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.signup_have_account_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.signup_sign_in_link),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpenSignIn)
                )
            }
        }
    }
}

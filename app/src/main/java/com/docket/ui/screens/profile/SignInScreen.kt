package com.docket.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
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
import com.docket.ui.theme.docketExtendedColors

@Composable
fun SignInScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    onOpenSignUp: () -> Unit,
    onOpenReset: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { onSignedIn() }
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
                stringResource(R.string.signin_headline),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
            )
            Text(
                stringResource(R.string.signin_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 26.dp)
            )
            DocketUnderlineField(value = email, onValueChange = { email = it }, label = stringResource(R.string.signin_email_label), keyboardType = KeyboardType.Email)
            Spacer(modifier = Modifier.padding(top = 6.dp))
            DocketUnderlineField(value = password, onValueChange = { password = it }, label = stringResource(R.string.signin_password_label), isPassword = true)
            DocketCheckbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                modifier = Modifier.padding(top = 22.dp)
            ) {
                Text(stringResource(R.string.signin_remember_me), style = MaterialTheme.typography.titleSmall)
            }
            Text(
                stringResource(R.string.signin_forgot_password),
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .padding(top = 26.dp)
                    .clickable(onClick = onOpenReset)
                    .align(Alignment.CenterHorizontally)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .padding(bottom = 1.dp)
                )
                Text(
                    text = stringResource(R.string.signin_or_continue_with),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .padding(bottom = 1.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SocialLoginButton(label = "G", modifier = Modifier.weight(1f))
                SocialLoginButton(label = "", modifier = Modifier.weight(1f))
                SocialLoginButton(label = "f", modifier = Modifier.weight(1f))
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(R.string.signin_button),
                onClick = { viewModel.signIn(email, password) },
                loading = isBusy,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = DocketSpacing.space16, bottom = DocketSpacing.space32),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.signin_no_account_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.signin_sign_up_link),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpenSignUp)
                )
            }
        }
    }
}

@Composable
private fun SocialLoginButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = docketExtendedColors().ink2
            )
        }
    }
}

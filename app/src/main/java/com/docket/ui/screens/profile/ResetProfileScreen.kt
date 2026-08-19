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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docket.R
import com.docket.ui.components.PrimaryButton
import com.docket.ui.icons.DocketIcons
import com.docket.ui.theme.DocketSpacing
import kotlinx.coroutines.delay

/**
 * Screen 4: Password reset / "Check your mail" OTP screen matching the prototype.
 * 4 OTP digit boxes, resend countdown timer, "Confirm" primary button, and an interactive numeric keypad.
 */
@Composable
fun ResetProfileScreen(
    onBack: () -> Unit,
    onReset: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val currentProfile by viewModel.profile.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val email = currentProfile?.email?.ifBlank { null } ?: stringResource(R.string.reset_default_email)
    var otpDigits by remember { mutableStateOf(listOf("4", "6", "7")) }
    var secondsLeft by remember { mutableIntStateOf(55) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { onReset() }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DocketSpacing.space24)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(top = DocketSpacing.space16)
            ) {
                Icon(imageVector = DocketIcons.Back, contentDescription = stringResource(R.string.common_back))
            }

            Text(
                text = stringResource(R.string.reset_headline),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp)
            )

            Text(
                text = stringResource(R.string.reset_body, email),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 30.dp)
            )

            // 4 OTP Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(4) { index ->
                    val digit = otpDigits.getOrNull(index) ?: ""
                    val isFilled = digit.isNotEmpty()
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(66.dp)
                            .clip(shape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.surfaceContainerLowest
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, shape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = digit,
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.reset_resend_prompt),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, bottom = 4.dp)
            )

            val resendPrefix = stringResource(R.string.reset_resend_prefix)
            val resendSeconds = stringResource(R.string.reset_resend_seconds, secondsLeft)
            Text(
                text = buildAnnotatedString {
                    append(resendPrefix)
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(resendSeconds)
                    }
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = secondsLeft == 0) {
                        secondsLeft = 55
                    }
            )

            error?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            PrimaryButton(
                text = stringResource(R.string.reset_confirm_button),
                onClick = {
                    // Docket's "reset" is honestly local (see ProfileRepository's class doc) — the
                    // 4-digit code the user just entered doubles as the new local passphrase since
                    // there's no separate password field on this screen, matching the prototype.
                    viewModel.resetProfile(
                        displayName = currentProfile?.displayName.orEmpty(),
                        email = currentProfile?.email.orEmpty(),
                        passphrase = otpDigits.joinToString("")
                    )
                },
                enabled = otpDigits.size == 4 && currentProfile != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Interactive Numeric Keypad at bottom
            val keypadShape = RoundedCornerShape(18.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 26.dp),
                shape = keypadShape,
                color = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "⌫")
                    )
                    keyRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = key.isNotEmpty()) {
                                            when (key) {
                                                "⌫" -> if (otpDigits.isNotEmpty()) otpDigits = otpDigits.dropLast(1)
                                                "" -> Unit
                                                else -> if (otpDigits.size < 4) otpDigits = otpDigits + key
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 21.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

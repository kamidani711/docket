package com.docket.ui.screens.unlock

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.docket.ui.components.BrandHeader
import com.docket.ui.components.DocketLogoMark
import com.docket.ui.icons.DocketIcons
import com.docket.ui.theme.DocketBigCardShape
import com.docket.ui.theme.DocketOnSurfaceLight
import com.docket.ui.theme.DocketPillShape
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketCardShadow
import com.docket.ui.theme.docketExtendedColors

@Composable
fun UnlockScreen(
    onBack: () -> Unit = {},
    viewModel: UnlockViewModel = hiltViewModel()
) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity

    val displayPrice = price ?: "$14.99"
    val benefits = listOf(
        stringResource(R.string.unlock_benefit_batch_export),
        stringResource(R.string.unlock_benefit_text_layer),
        stringResource(R.string.unlock_benefit_languages),
        stringResource(R.string.unlock_benefit_folders)
    )
    val priceSuffix = stringResource(R.string.unlock_price_suffix)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            BrandHeader(
                title = stringResource(R.string.unlock_title),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Card stack with amber sliver and coral card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Amber sliver peeking behind the coral card
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp, top = 34.dp, bottom = 70.dp)
                        .size(width = 26.dp, height = 340.dp)
                        .background(docketExtendedColors().amber, RoundedCornerShape(20.dp))
                )

                // Main Coral Card
                val coralShape = DocketBigCardShape
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Handoff spec: premium card glow is rgba(255,107,103,.32).
                        .docketCardShadow(
                            elevation = 16.dp,
                            shape = coralShape,
                            tint = docketExtendedColors().coral,
                            ambientAlpha = 0.16f,
                            spotAlpha = 0.32f
                        )
                        .clip(coralShape)
                        .background(docketExtendedColors().coral)
                        .padding(horizontal = 24.dp, vertical = 30.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                append(displayPrice)
                                append(" ")
                                withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)) {
                                    append(priceSuffix)
                                }
                            },
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = if (isPremium) stringResource(R.string.unlock_owned_message) else stringResource(R.string.unlock_message),
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.95f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp)
                        )

                        Box(
                            modifier = Modifier
                                .padding(vertical = 22.dp, horizontal = 4.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.35f))
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            benefits.forEach { benefit ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .background(Color.White, RoundedCornerShape(7.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = DocketIcons.Check,
                                            contentDescription = null,
                                            tint = docketExtendedColors().coral,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = benefit,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 21.sp
                                        ),
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (!isPremium) {
                                    activity?.let(viewModel::purchase)
                                }
                            },
                            enabled = !isBusy,
                            shape = DocketPillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = DocketOnSurfaceLight
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 26.dp)
                                .height(56.dp)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = DocketOnSurfaceLight
                                )
                            } else {
                                Text(
                                    text = if (isPremium) stringResource(R.string.unlock_active_label) else stringResource(R.string.unlock_title_cta),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DocketOnSurfaceLight
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isBusy) { viewModel.restorePurchases() }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.unlock_restore),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            message?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
        }
    }
}


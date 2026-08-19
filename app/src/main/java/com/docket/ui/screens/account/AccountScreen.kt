package com.docket.ui.screens.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docket.BuildConfig
import com.docket.R
import com.docket.ui.components.DocketSectionCard
import com.docket.ui.components.DocketTextButton
import com.docket.ui.components.PrimaryButton
import com.docket.ui.icons.DocketIcons
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketPillShape
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketCardShadow
import com.docket.ui.theme.docketExtendedColors
import kotlin.math.roundToInt

/**
 * The Account tab — replaces the old flat `SettingsScreen` with a profile tile, a Premium
 * banner, and grouped nav rows into the smaller focused screens Settings used to cram onto one
 * page (Personal Info, Scan preferences, Security, Language, Backup & restore, Recently
 * deleted), plus Privacy and Usage (real existing screens the handoff's row list doesn't
 * mention but which aren't being orphaned) and a real app-wide Dark Mode toggle.
 */
@Composable
fun AccountScreen(
    onOpenPersonalInfo: () -> Unit,
    onOpenScanPreferences: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenSignIn: () -> Unit,
    onOpenSignUp: () -> Unit,
    onOpenDesignSystem: () -> Unit = {},
    onOpenPipelineDebug: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val darkModeOverride by viewModel.darkModeOverride.collectAsStateWithLifecycle()
    val storageBreakdown by viewModel.storageBreakdown.collectAsStateWithLifecycle()
    val deviceFreeBytes by viewModel.deviceFreeBytes.collectAsStateWithLifecycle()
    val isClearingCache by viewModel.isClearingCache.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            com.docket.ui.components.BrandHeader(
                title = stringResource(R.string.account_title),
                modifier = Modifier.padding(bottom = DocketSpacing.space16)
            ) {
                androidx.compose.material3.IconButton(onClick = {}) {
                    Icon(
                        imageVector = DocketIcons.DotsCircle,
                        contentDescription = stringResource(R.string.account_more_desc),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = DocketSpacing.space20),
                verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
            ) {
                ProfileTile(
                    displayName = profile?.displayName,
                    email = profile?.email,
                    isSignedIn = isSignedIn && profile != null,
                    usedBytes = storageBreakdown?.totalBytes ?: 0L,
                    freeBytes = deviceFreeBytes,
                    onSignIn = onOpenSignIn,
                    onSignUp = onOpenSignUp
                )

                if (!isPremium) {
                    PremiumBanner(onClick = onOpenPremium)
                }

                DocketSectionCard {
                    AccountNavRow(DocketIcons.user(), stringResource(R.string.account_row_personal_info), onOpenPersonalInfo)
                    RowDivider()
                    AccountNavRow(DocketIcons.Gear, stringResource(R.string.account_row_scan_preferences), onOpenScanPreferences)
                    RowDivider()
                    AccountNavRow(DocketIcons.ShieldCheck, stringResource(R.string.account_row_security), onOpenSecurity)
                    RowDivider()
                    AccountNavRow(DocketIcons.Doc, stringResource(R.string.account_row_language), onOpenLanguage)
                    RowDivider()
                    AccountNavRow(DocketIcons.Info, stringResource(R.string.account_row_backup), onOpenBackup)
                    RowDivider()
                    AccountNavRow(DocketIcons.folder(), stringResource(R.string.library_recently_deleted), onOpenRecentlyDeleted)
                    RowDivider()
                    AccountNavRow(DocketIcons.Shield, stringResource(R.string.account_row_privacy), onOpenPrivacy)
                    RowDivider()
                    AccountNavRow(DocketIcons.Eye, stringResource(R.string.account_row_usage), onOpenAnalytics)
                }

                // Debug-only QA entry points, stripped from release builds by R8's constant
                // folding of BuildConfig.DEBUG — same pattern the old SettingsScreen used.
                if (BuildConfig.DEBUG) {
                    DocketSectionCard(header = "Debug") {
                        AccountNavRow(DocketIcons.Grid, "Design system (debug)", onOpenDesignSystem)
                        RowDivider()
                        AccountNavRow(DocketIcons.Doc, "Pipeline debug", onOpenPipelineDebug)
                    }
                }

                DocketSectionCard(header = stringResource(R.string.account_storage_header)) {
                    val breakdown = storageBreakdown
                    if (breakdown == null) {
                        Text(stringResource(R.string.account_calculating), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(
                            stringResource(R.string.account_storage_used_cache, formatBytes(breakdown.totalBytes), formatBytes(breakdown.cacheBytes)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (breakdown.cacheBytes > 0) {
                            DocketTextButton(
                                text = if (isClearingCache) stringResource(R.string.account_clearing_cache) else stringResource(R.string.account_clear_cache),
                                onClick = viewModel::clearCache,
                                enabled = !isClearingCache,
                                loading = isClearingCache
                            )
                        }
                    }
                }

                DocketSectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
                    ) {
                        Icon(imageVector = DocketIcons.Eye, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Text(
                            stringResource(R.string.account_dark_mode),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = darkModeOverride ?: androidx.compose.foundation.isSystemInDarkTheme(),
                            onCheckedChange = viewModel::setDarkModeOverride
                        )
                    }
                    if (isSignedIn) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = viewModel::signOut),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
                        ) {
                            Icon(imageVector = DocketIcons.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(stringResource(R.string.account_log_out), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                Box(modifier = Modifier.height(DocketSpacing.space24))
            }
        }
    }
}

@Composable
private fun ProfileTile(
    displayName: String?,
    email: String?,
    isSignedIn: Boolean,
    usedBytes: Long,
    freeBytes: Long,
    onSignIn: () -> Unit,
    onSignUp: () -> Unit
) {
    val shape = MaterialTheme.shapes.large
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .docketCardShadow(elevation = DocketSpacing.space4, shape = shape),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = shape
    ) {
        if (isSignedIn && displayName != null) {
            Row(
                modifier = Modifier.padding(DocketSpacing.space16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4)) {
                    Text(displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    email?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // Real device-free-space usage, not a fabricated storage quota — see
                    // AccountViewModel.
                    val total = (usedBytes + freeBytes).coerceAtLeast(1L)
                    val fraction = (usedBytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    Text(
                        stringResource(R.string.account_used_on_device, formatBytes(usedBytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh, DocketPillShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(6.dp)
                                .background(MaterialTheme.colorScheme.primary, DocketPillShape)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(DocketSpacing.space16),
                verticalArrangement = Arrangement.spacedBy(DocketSpacing.space12)
            ) {
                Text(stringResource(R.string.account_not_signed_in_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.account_not_signed_in_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                    com.docket.ui.components.SecondaryButton(text = stringResource(R.string.signin_button), onClick = onSignIn, modifier = Modifier.weight(1f))
                    PrimaryButton(text = stringResource(R.string.account_create_account_button), onClick = onSignUp, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PremiumBanner(onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.large
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .docketCardShadow(elevation = DocketSpacing.space8, shape = shape),
        color = MaterialTheme.colorScheme.primary,
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(DocketSpacing.space16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
        ) {
            Box(
                modifier = Modifier
                    .size(DocketDimens.rowIconSize)
                    .background(docketExtendedColors().warning, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = DocketIcons.star(filled = true), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4)) {
                Text(stringResource(R.string.account_premium_banner_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary)
                Text(
                    stringResource(R.string.account_premium_banner_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
            }
            Surface(color = MaterialTheme.colorScheme.onPrimary, shape = DocketPillShape) {
                Text(
                    stringResource(R.string.account_unlock_button),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = DocketSpacing.space16, vertical = DocketSpacing.space8)
                )
            }
        }
    }
}

@Composable
private fun AccountNavRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = DocketSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Icon(imageVector = DocketIcons.Chevron, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> "${(bytes / (1024.0 * 1024.0 * 1024.0) * 10).roundToInt() / 10.0} GB"
    bytes >= 1024 * 1024 -> "${(bytes / (1024.0 * 1024.0) * 10).roundToInt() / 10.0} MB"
    bytes >= 1024 -> "${(bytes / 1024.0 * 10).roundToInt() / 10.0} KB"
    else -> "$bytes B"
}

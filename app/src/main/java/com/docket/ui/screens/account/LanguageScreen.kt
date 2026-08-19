package com.docket.ui.screens.account

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docket.domain.model.LanguagePackStatus
import com.docket.ui.components.DocketSectionCard
import com.docket.ui.components.DocketTextButton
import com.docket.ui.components.SecondaryButton
import com.docket.ui.icons.DocketIcons
import com.docket.ui.theme.DocketSpacing

/** Tag to BCP-47 language tag for [AppCompatDelegate.setApplicationLocales] — matches the
 *  8 locales Docket's `res/values-*` folders already ship (see strings.xml's own header note). */
private val SUPPORTED_APP_LANGUAGES = listOf(
    null to "System default",
    "en" to "English",
    "ar" to "العربية (Arabic)",
    "ur" to "اردو (Urdu)",
    "hi" to "हिन्दी (Hindi)",
    "in" to "Bahasa Indonesia",
    "pt-BR" to "Português (Brasil)",
    "es" to "Español",
    "fr" to "Français"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(onBack: () -> Unit, viewModel: LanguageViewModel = hiltViewModel()) {
    val activity = LocalContext.current as? FragmentActivity
    var currentTag by remember {
        mutableStateOf(AppCompatDelegate.getApplicationLocales().toLanguageTags().takeIf { it.isNotBlank() })
    }
    val languagePacks by viewModel.languagePacks.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val installingLanguage by viewModel.installingLanguage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Language") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(DocketIcons.Back, contentDescription = "Back") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(DocketSpacing.space20),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space24)
        ) {
            DocketSectionCard(header = "App language") {
                SUPPORTED_APP_LANGUAGES.forEachIndexed { index, (tag, label) ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentTag = tag
                                AppCompatDelegate.setApplicationLocales(
                                    if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
                                )
                                activity?.recreate()
                            }
                            .padding(vertical = DocketSpacing.space8),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (currentTag == tag) {
                            Icon(DocketIcons.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            DocketSectionCard(header = "OCR recognition languages") {
                Text(
                    "Latin is built in and always available. Additional scripts download on " +
                        "demand (about 25MB each) once Premium is unlocked.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                languagePacks.forEachIndexed { index, packState ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = DocketSpacing.space8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(packState.language.displayName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = when {
                                    packState.language.alwaysAvailable -> "Built in"
                                    packState.status == LanguagePackStatus.INSTALLED -> "Installed"
                                    else -> "~${packState.language.approxDownloadMb} MB"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        when {
                            packState.language.alwaysAvailable -> Icon(
                                imageVector = DocketIcons.Check,
                                contentDescription = "Built in",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            installingLanguage == packState.language -> CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
                            packState.status == LanguagePackStatus.INSTALLED ->
                                DocketTextButton(text = "Remove", onClick = { viewModel.removeLanguage(packState.language) })
                            else -> SecondaryButton(
                                text = "Install",
                                onClick = { viewModel.installLanguage(packState.language) }
                            )
                        }
                    }
                }
                errorMessage?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                if (!isPremium) {
                    Text(
                        "Additional scripts need Premium.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

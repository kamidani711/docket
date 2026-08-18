package com.docket.ui.theme

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Drop on any `@Composable` to get it rendered in Android Studio's preview panel across all
 * four axes the brief calls out: light/dark × LTR/RTL (Arabic locale exercises RTL — Urdu is
 * also RTL but Arabic is enough to catch mirroring bugs at design time).
 */
@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Light RTL", locale = "ar", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark RTL", locale = "ar", uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class DocketPreviews

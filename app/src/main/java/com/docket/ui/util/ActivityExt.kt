package com.docket.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** `LocalContext.current` in Compose is often a `ContextWrapper` — unwrap to find the Activity
 *  GmsDocumentScanner's `getStartScanIntent(Activity)` needs. */
tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("No Activity found from context $this")
}

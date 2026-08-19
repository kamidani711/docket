package com.docket.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.docket.ui.components.PrimaryButton
import com.docket.ui.theme.DocketSpacing

/**
 * Wraps the whole app. When app lock is off, [content] shows immediately. When it's on, content
 * is hidden behind [LockScreen] until [onAuthenticate] reports success — re-locking every time
 * the app is backed away from (ON_STOP), not just on cold start, which is what people expect
 * from "app lock" on any platform.
 */
@Composable
fun AppLockGate(
    onAuthenticate: (onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    viewModel: AppLockViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    var isUnlocked by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appLockEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && appLockEnabled == true) {
                isUnlocked = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun attemptUnlock() {
        errorMessage = null
        onAuthenticate({ isUnlocked = true }, { message -> errorMessage = message })
    }

    // Prompt automatically the first time a locked session appears, instead of making the
    // user tap "Unlock" just to see the prompt they were already expecting.
    LaunchedEffect(appLockEnabled == true && !isUnlocked) {
        if (appLockEnabled == true && !isUnlocked) attemptUnlock()
    }

    when {
        appLockEnabled == null -> Unit // first DataStore read hasn't landed yet; show nothing rather than risk a flash of content
        appLockEnabled == false || isUnlocked -> content()
        else -> LockScreen(errorMessage = errorMessage, onUnlockClick = ::attemptUnlock)
    }
}

@Composable
private fun LockScreen(errorMessage: String?, onUnlockClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(DocketSpacing.space24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier.padding(top = DocketSpacing.space16, bottom = DocketSpacing.space24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4)
            ) {
                Text("Docket is locked", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Unlock with your fingerprint, face, or device PIN.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            errorMessage?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = DocketSpacing.space16)
                )
            }
            PrimaryButton(text = "Unlock", onClick = onUnlockClick)
        }
    }
}

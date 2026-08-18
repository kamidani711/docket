package com.docket

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.docket.data.analytics.CrashSessionTracker
import com.docket.ui.lock.AppLockGate
import com.docket.ui.lock.BiometricAuthenticator
import com.docket.ui.navigation.DocketNavHost
import com.docket.ui.theme.DocketTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-Activity host. All screens are Compose destinations inside [DocketNavHost].
 *
 * `android:launchMode="singleTop"` (manifest) + [onNewIntent] is what lets a warranty reminder
 * notification tap route into an already-running instance instead of spawning a duplicate one —
 * see [WarrantyReminderWorker][com.docket.data.work.WarrantyReminderWorker] for the other end.
 *
 * Extends [FragmentActivity] (not plain `ComponentActivity`) purely because [BiometricPrompt
 * ][androidx.biometric.BiometricPrompt] needs one — see [BiometricAuthenticator]. Compose's
 * `setContent` works the same either way, since `FragmentActivity` is itself a `ComponentActivity`.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var pendingDocumentId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val biometricAuthenticator = BiometricAuthenticator(this)
        handleIntent(intent)
        setContent {
            DocketTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppLockGate(
                        onAuthenticate = { onSuccess, onError ->
                            biometricAuthenticator.authenticate(onSuccess, onError)
                        }
                    ) {
                        DocketNavHost(
                            pendingDocumentId = pendingDocumentId,
                            onPendingDocumentIdConsumed = { pendingDocumentId = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    // Re-arms the crash marker every time the app returns to the foreground — not just once in
    // DocketApplication.onCreate() — so a crash after a background/foreground cycle (without a
    // full process restart) is still caught the next time the app actually launches fresh.
    override fun onStart() {
        super.onStart()
        CrashSessionTracker.markSessionActive(this)
    }

    // Marks this session as having ended cleanly — see CrashSessionTracker's class doc for why
    // this is the "clean background transition" signal rather than onDestroy.
    override fun onStop() {
        super.onStop()
        CrashSessionTracker.markSessionEnded(this)
    }

    private fun handleIntent(intent: Intent) {
        val documentId = intent.getLongExtra(EXTRA_DOCUMENT_ID, -1L)
        if (documentId > 0) pendingDocumentId = documentId
    }

    companion object {
        /** Notification deep-link target — see WarrantyReminderWorker. */
        const val EXTRA_DOCUMENT_ID = "documentId"
    }
}

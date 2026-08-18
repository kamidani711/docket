package com.docket.ui.lock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

/**
 * Thin wrapper around [BiometricPrompt] for the optional app-lock setting. Deliberately not
 * behind a domain interface like the rest of the app's Android integrations — BiometricPrompt
 * is inherently tied to a live Activity (it hosts its own DialogFragment internally), so
 * there's no meaningful data-layer implementation to swap it for. It's a UI-layer concern, same
 * as navigation. Accepts fingerprint/face (BIOMETRIC_WEAK) or the device's own PIN/pattern/
 * password (DEVICE_CREDENTIAL) — whichever the user already has set up, so app lock doesn't
 * require enrolling anything new.
 */
class BiometricAuthenticator(private val activity: FragmentActivity) {

    /** False if the device has no usable biometric or device-credential lock set up at all —
     *  the app-lock toggle should stay off (and say why) in that case. */
    fun isAvailable(): Boolean =
        BiometricManager.from(activity).canAuthenticate(ALLOWED_AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // A user-initiated cancel (back button, negative button) isn't a failure
                    // worth surfacing as an error message.
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        onError(errString.toString())
                    }
                }

                // onAuthenticationFailed (one unrecognized fingerprint, say) is deliberately
                // not handled — the system prompt stays open and lets the user retry on its own.
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Docket")
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()
        prompt.authenticate(info)
    }
}

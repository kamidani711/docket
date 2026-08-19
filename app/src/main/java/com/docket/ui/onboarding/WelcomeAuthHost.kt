package com.docket.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.docket.ui.screens.profile.ResetProfileScreen
import com.docket.ui.screens.profile.SignInScreen
import com.docket.ui.screens.profile.SignUpScreen

private enum class WelcomeAuthPage { SignIn, SignUp, Reset }

/**
 * Post-onboarding auth, matching the handoff: Skip/Next lands on Sign in. Back (or a successful
 * sign-in/up) continues into the app — profiles stay optional, so backing out never blocks Home.
 */
@Composable
fun WelcomeAuthHost(onFinished: () -> Unit) {
    var page by remember { mutableStateOf(WelcomeAuthPage.SignIn) }

    when (page) {
        WelcomeAuthPage.SignIn -> SignInScreen(
            onBack = onFinished,
            onSignedIn = onFinished,
            onOpenSignUp = { page = WelcomeAuthPage.SignUp },
            onOpenReset = { page = WelcomeAuthPage.Reset }
        )
        WelcomeAuthPage.SignUp -> SignUpScreen(
            onBack = { page = WelcomeAuthPage.SignIn },
            onSignedUp = onFinished,
            onOpenSignIn = { page = WelcomeAuthPage.SignIn }
        )
        WelcomeAuthPage.Reset -> ResetProfileScreen(
            onBack = { page = WelcomeAuthPage.SignIn },
            onReset = { page = WelcomeAuthPage.SignIn }
        )
    }
}

package com.docket.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Wraps the whole app, same shape as [com.docket.ui.lock.AppLockGate]: shows nothing while the
 * first DataStore read is in flight, [OnboardingScreen] once (first launch only), then
 * [content] forever after. Sits outside [com.docket.ui.lock.AppLockGate] in `MainActivity` — a
 * brand-new install has app lock off anyway, so onboarding is always what a first launch sees.
 */
@Composable
fun OnboardingGate(
    viewModel: OnboardingViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
    val showWelcomeAuth by viewModel.showWelcomeAuth.collectAsStateWithLifecycle()

    when {
        hasSeenOnboarding == null -> Unit // first DataStore read hasn't landed yet; show nothing rather than risk a flash
        hasSeenOnboarding == false -> OnboardingScreen(onFinished = viewModel::markSeen)
        showWelcomeAuth -> WelcomeAuthHost(onFinished = viewModel::finishWelcomeAuth)
        else -> content()
    }
}

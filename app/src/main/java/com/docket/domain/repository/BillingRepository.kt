package com.docket.domain.repository

import android.app.Activity
import com.docket.domain.common.Result
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google Play Billing for the single one-time "lifetime unlock" product — no subscriptions
 * anywhere in this app. [launchPurchaseFlow] needs a live [Activity] (Play's own purchase UI is
 * shown over it), same reason [com.docket.ui.lock.BiometricAuthenticator] needs one; everything
 * else here is a plain suspend call an Activity-less ViewModel can use directly.
 */
interface BillingRepository {
    /** Localized price string (e.g. "$4.99") once Play has responded, or null while still
     *  loading or if Play Billing isn't reachable on this device. */
    val premiumPrice: StateFlow<String?>

    /** Human-readable messages for purchase failures that happen asynchronously, after
     *  [launchPurchaseFlow] has already returned — Play's own purchase sheet reports those
     *  through the listener callback, not the launch call itself. */
    val purchaseErrors: SharedFlow<String>

    suspend fun launchPurchaseFlow(activity: Activity): Result<Unit>

    /** Re-queries Play for the account's existing purchases and reconciles local unlock state
     *  against them. Called once on app start and from the explicit "Restore purchases" button.
     *  Returns true if a valid purchase was found. */
    suspend fun syncPurchases(): Result<Boolean>
}

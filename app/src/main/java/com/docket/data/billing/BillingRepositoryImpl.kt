package com.docket.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.docket.domain.common.Result
import com.docket.domain.repository.BillingRepository
import com.docket.domain.repository.PremiumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * One-time, non-consumable "lifetime unlock" — [PREMIUM_PRODUCT_ID] must be created in Play
 * Console as a managed in-app product with exactly that ID before this does anything useful.
 *
 * The [BillingClient] connection itself is still the classic listener-based lifecycle (that
 * part hasn't changed in years of Billing Library versions); the individual query/purchase/
 * acknowledge calls use the `billing-ktx` suspend extensions on top of it.
 *
 * IMPORTANT — nothing that touches Play Billing can actually run in this build environment (no
 * device, no Play Store, no network access here at all). This is written against the
 * documented Billing Library 7 API surface but has never been exercised against a real Play
 * Console product or a real purchase. Treat it as a first draft to verify on a real signed
 * build with a real product configured — see the chat write-up for the full list of what that
 * verification needs to cover.
 */
@Singleton
class BillingRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val premiumRepository: PremiumRepository
) : BillingRepository, PurchasesUpdatedListener {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        // The no-arg overload is deprecated in favor of scoping pending-purchase support
        // explicitly — Docket only ever sells the one-time lifetime unlock, never a
        // subscription, so only one-time products need it enabled.
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var productDetails: ProductDetails? = null

    private val _premiumPrice = MutableStateFlow<String?>(null)
    override val premiumPrice: StateFlow<String?> = _premiumPrice.asStateFlow()

    private val _purchaseErrors = MutableSharedFlow<String>()
    override val purchaseErrors: SharedFlow<String> = _purchaseErrors.asSharedFlow()

    init {
        // Fire-and-forget: this class is a Hilt singleton created on first injection, so
        // connecting eagerly means the price is usually already loaded by the time the unlock
        // screen opens, and syncPurchases() gets its one-per-process-lifetime run at app start
        // (see DocketApplication) without the caller needing to wait on the connection itself.
        repositoryScope.launch {
            if (ensureConnected()) {
                loadProductDetails()
                syncPurchases()
            }
        }
    }

    override suspend fun launchPurchaseFlow(activity: Activity): Result<Unit> {
        if (!ensureConnected()) {
            return Result.Error(IllegalStateException("Couldn't reach Google Play. Check your connection and try again."))
        }
        val details = productDetails ?: run {
            loadProductDetails()
            productDetails
        } ?: return Result.Error(IllegalStateException("Couldn't load purchase details from Google Play."))

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        return if (result.responseCode == BillingResponseCode.OK) {
            Result.Success(Unit)
        } else {
            Result.Error(IllegalStateException(result.debugMessage.ifBlank { "Couldn't start the purchase." }))
        }
    }

    override suspend fun syncPurchases(): Result<Boolean> {
        if (!ensureConnected()) {
            return Result.Error(IllegalStateException("Couldn't reach Google Play."))
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val purchasesResult = billingClient.queryPurchasesAsync(params)
        if (purchasesResult.billingResult.responseCode != BillingResponseCode.OK) {
            return Result.Error(IllegalStateException(purchasesResult.billingResult.debugMessage))
        }
        val validPurchases = purchasesResult.purchasesList.filter { it.isValidPremiumPurchase() }
        validPurchases.forEach { acknowledgeIfNeeded(it) }
        // Play is the source of truth once billing is reachable: an empty result here means no
        // purchase on this account, so this correctly un-sets a stale/incorrect local flag too
        // (a refund, for instance) — not just the "found one, unlock" direction.
        premiumRepository.setPremiumUnlocked(validPurchases.isNotEmpty())
        return Result.Success(validPurchases.isNotEmpty())
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                purchases.orEmpty().filter { it.isValidPremiumPurchase() }.forEach { purchase ->
                    repositoryScope.launch {
                        acknowledgeIfNeeded(purchase)
                        premiumRepository.setPremiumUnlocked(true)
                    }
                }
            }
            BillingResponseCode.USER_CANCELED -> Unit // not a failure worth surfacing
            else -> repositoryScope.launch {
                _purchaseErrors.emit(billingResult.debugMessage.ifBlank { "Purchase failed." })
            }
        }
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params)
        }
    }

    private fun Purchase.isValidPremiumPurchase(): Boolean =
        products.contains(PREMIUM_PRODUCT_ID) && purchaseState == Purchase.PurchaseState.PURCHASED

    private suspend fun loadProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()
        val result = billingClient.queryProductDetails(params)
        val details = result.productDetailsList?.firstOrNull()
        productDetails = details
        _premiumPrice.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) continuation.resume(result.responseCode == BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    // No manual reconnect here — the next call to ensureConnected() (the next
                    // billing action the user takes) will see isReady == false and reconnect.
                }
            })
        }
    }

    private companion object {
        const val PREMIUM_PRODUCT_ID = "premium_lifetime_unlock"
    }
}

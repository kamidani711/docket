package com.docket.domain.repository

import com.docket.domain.model.PremiumFeature
import kotlinx.coroutines.flow.Flow

/**
 * Gating logic + the one flag it reads. [setPremiumUnlocked] is called by
 * [com.docket.domain.repository.BillingRepository]'s implementation whenever a purchase or
 * restore is verified against Play — that's the only writer. Billing reconciles this flag
 * against Play on every app start (see [BillingRepository]), so it's never stale for long even
 * across a refund: Play is the source of truth.
 */
interface PremiumRepository {
    val isPremium: Flow<Boolean>

    fun isFeatureUnlocked(feature: PremiumFeature): Flow<Boolean>

    suspend fun setPremiumUnlocked(unlocked: Boolean)
}

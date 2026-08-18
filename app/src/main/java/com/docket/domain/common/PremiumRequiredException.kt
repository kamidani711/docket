package com.docket.domain.common

import com.docket.domain.model.PremiumFeature

/** Thrown (well — returned via [Result.Error], never actually thrown) when a use case is asked
 *  for a [feature] the user hasn't unlocked. Carries which feature so the UI can show a
 *  specific upsell rather than a generic error. */
class PremiumRequiredException(val feature: PremiumFeature) :
    Exception("Requires premium: $feature")

package com.docket.domain.model

/** Aggregated local counts, exactly what Settings > Usage shows. Nothing behind this model is
 *  ever transmitted — see [com.docket.domain.repository.AnalyticsRepository]'s doc. */
data class AnalyticsSummary(
    val scansStarted: Int = 0,
    val documentsSaved: Int = 0,
    val exportsCompleted: Int = 0,
    val searchesPerformed: Int = 0,
    val cleanSessions: Int = 0,
    val crashedSessions: Int = 0,
    /** Feature name -> times used, e.g. "receipt_tagged" -> 4. Keys are internal identifiers,
     *  not user-facing strings — the UI maps each to a display label. */
    val featureUsage: Map<String, Int> = emptyMap()
) {
    val totalSessions: Int get() = cleanSessions + crashedSessions

    /** 1.0 (100%) when there's no session history yet, rather than an undefined 0/0 — an empty
     *  history isn't evidence of instability. */
    val crashFreeSessionRate: Float
        get() = if (totalSessions == 0) 1f else cleanSessions / totalSessions.toFloat()
}

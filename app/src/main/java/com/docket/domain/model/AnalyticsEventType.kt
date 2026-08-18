package com.docket.domain.model

/**
 * Every kind of event the local-only analytics store tracks. [FEATURE_USED] is the catch-all
 * for notable feature taps (see call sites for the exact [AnalyticsSummary.featureUsage] keys
 * used) — everything else here gets its own dedicated count because the brief calls it out by
 * name: scans, saves, exports, searches, and crash-free sessions.
 */
enum class AnalyticsEventType {
    SCAN_STARTED,
    DOCUMENT_SAVED,
    DOCUMENT_EXPORTED,
    SEARCH_PERFORMED,
    FEATURE_USED,
    SESSION_CLEAN,
    SESSION_CRASHED
}

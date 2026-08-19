package com.docket.ui.navigation

import kotlinx.serialization.Serializable

/**
 * All nav destinations, as type-safe routes (Navigation Compose 2.8+'s `@Serializable` route
 * API) rather than hand-built route strings — the compiler checks every `navigate(...)` call
 * against these types instead of a typo in a string template failing silently at runtime.
 * [DocumentDetail] carries its `documentId` argument as an actual constructor parameter now,
 * so there's no separate `createRoute`/route-template split to keep in sync by hand.
 *
 * [ScanFlow] is the nested Scan → Review sub-graph's own route, not a screen — see
 * `DocketNavHost`'s comment on why Scan and Review share one `ScanSessionViewModel` scoped to
 * it.
 */
sealed interface Destination {
    @Serializable data object Library : Destination
    @Serializable data object Scan : Destination
    @Serializable data object Review : Destination
    @Serializable data object ScanFlow : Destination

    @Serializable data class DocumentDetail(val documentId: Long) : Destination

    @Serializable data object Search : Destination
    @Serializable data object RecentlyDeleted : Destination
    @Serializable data object Backup : Destination
    @Serializable data object Privacy : Destination
    @Serializable data object Analytics : Destination
    @Serializable data object Receipts : Destination
    @Serializable data object Warranties : Destination
    @Serializable data object Settings : Destination
    @Serializable data object Unlock : Destination
    @Serializable data object DesignSystem : Destination
    @Serializable data object PipelineDebug : Destination

    companion object {
        // The type-safe API still surfaces DocumentDetail's argument as a plain SavedStateHandle
        // entry keyed by its property name — this constant is that key, kept around so
        // DocumentDetailViewModel doesn't need to know the route type exists at all.
        const val ARG_DOCUMENT_ID = "documentId"
    }
}

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
 *
 * [Home], [Files], [Unlock] and [Account] are the four persistent bottom-nav tabs (see
 * `DocketApp`/`DocketBottomNav`) — everything else is a full-screen push with the bar hidden.
 * [Home]/[Files]/[Account] replace the old single [Library]+[Settings] pair from before the
 * `design_handoff_docket_ui` rebuild: [Files] is [Library]'s screen restyled under a new route
 * name, and [Account] absorbs [Settings]'s nav role (split into focused sub-screens — see
 * `AccountScreen`).
 */
sealed interface Destination {
    @Serializable data object Home : Destination
    @Serializable data object Files : Destination
    @Serializable data object Scan : Destination
    @Serializable data object Review : Destination

    /**
     * Nested Scan → Review graph. [launch] picks the capture entry: camera shutter, gallery
     * import (Home's image FAB / Import PDF), matching the handoff's Home tool + FAB routes.
     */
    @Serializable data class ScanFlow(val launch: String = LAUNCH_CAMERA) : Destination {
        companion object {
            const val LAUNCH_CAMERA = "camera"
            const val LAUNCH_GALLERY = "gallery"
            const val LAUNCH_PDF = "pdf"
        }
    }

    @Serializable data class DocumentDetail(val documentId: Long) : Destination

    @Serializable data object Search : Destination
    @Serializable data object RecentlyDeleted : Destination
    @Serializable data object Backup : Destination
    @Serializable data object Privacy : Destination
    @Serializable data object Analytics : Destination
    @Serializable data object Receipts : Destination
    @Serializable data object Warranties : Destination
    @Serializable data object Unlock : Destination
    @Serializable data object Account : Destination
    @Serializable data object PersonalInfo : Destination
    @Serializable data object ScanPreferences : Destination
    @Serializable data object Security : Destination
    @Serializable data object Language : Destination
    @Serializable data object SignIn : Destination
    @Serializable data object SignUp : Destination
    @Serializable data object ResetProfile : Destination
    @Serializable data object DesignSystem : Destination
    @Serializable data object PipelineDebug : Destination

    companion object {
        // The type-safe API still surfaces DocumentDetail's argument as a plain SavedStateHandle
        // entry keyed by its property name — this constant is that key, kept around so
        // DocumentDetailViewModel doesn't need to know the route type exists at all.
        const val ARG_DOCUMENT_ID = "documentId"

        /** The four persistent bottom-nav tabs — see class doc. */
        val TOP_LEVEL: Set<Destination> = setOf(Home, Files, Unlock, Account)
    }
}

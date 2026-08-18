package com.docket.ui.navigation

/**
 * All top-level nav destinations. [DocumentDetail] carries a `documentId` argument — use
 * [DocumentDetail.createRoute] to navigate to a specific document, not `DocumentDetail.route`
 * directly (that's the route *template*, only valid for NavHost registration).
 */
sealed class Destination(val route: String, val label: String) {
    data object Library : Destination("library", "Library")
    data object Scan : Destination("scan", "Scan")
    data object Review : Destination("review", "Review")

    data object DocumentDetail : Destination("document_detail/{$ARG_DOCUMENT_ID}", "Document Detail") {
        fun createRoute(documentId: Long): String = "document_detail/$documentId"
    }

    data object Search : Destination("search", "Search")
    data object RecentlyDeleted : Destination("recently_deleted", "Recently Deleted")
    data object Backup : Destination("backup", "Backup & Restore")
    data object Privacy : Destination("privacy", "Privacy")
    data object Analytics : Destination("analytics", "Usage")
    data object Receipts : Destination("receipts", "Receipts")
    data object Warranties : Destination("warranties", "Warranties")
    data object Settings : Destination("settings", "Settings")
    data object Unlock : Destination("unlock", "Unlock")
    data object DesignSystem : Destination("design_system", "Design System")
    data object PipelineDebug : Destination("pipeline_debug", "Pipeline Debug")

    companion object {
        const val ARG_DOCUMENT_ID = "documentId"
    }
}

package com.docket.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.docket.ui.screens.analytics.AnalyticsScreen
import com.docket.ui.screens.backup.BackupScreen
import com.docket.ui.screens.debug.PipelineDebugScreen
import com.docket.ui.screens.designsystem.DesignSystemScreen
import com.docket.ui.screens.documentdetail.DocumentDetailScreen
import com.docket.ui.screens.library.LibraryScreen
import com.docket.ui.screens.library.RecentlyDeletedScreen
import com.docket.ui.screens.privacy.PrivacyScreen
import com.docket.ui.screens.receipts.ReceiptsScreen
import com.docket.ui.screens.review.ReviewScreen
import com.docket.ui.screens.scan.ScanScreen
import com.docket.ui.screens.scan.ScanSessionViewModel
import com.docket.ui.screens.search.SearchScreen
import com.docket.ui.screens.settings.SettingsScreen
import com.docket.ui.screens.unlock.UnlockScreen
import com.docket.ui.screens.warranties.WarrantiesScreen

@Composable
fun DocketNavHost(
    navController: NavHostController = rememberNavController(),
    pendingDocumentId: Long? = null,
    onPendingDocumentIdConsumed: () -> Unit = {}
) {
    // Warranty reminder notification taps land here — see MainActivity.
    LaunchedEffect(pendingDocumentId) {
        if (pendingDocumentId != null) {
            navController.navigate(Destination.DocumentDetail(pendingDocumentId))
            onPendingDocumentIdConsumed()
        }
    }

    // A quiet "shared axis" push/pop, not a default hard cut — one moment applied once at the
    // NavHost level covers every screen instead of a bespoke transition per destination. Short
    // (200/150ms, well under the 300ms ceiling) and directional: forward pushes content in from
    // the right and gently out to the left, back reverses it — the standard cue for "you went
    // somewhere" vs. "you came back."
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 10 }
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 10 }
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it / 10 }
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { it / 10 }
    }

    NavHost(
        navController = navController,
        startDestination = Destination.Library,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = popEnter,
        popExitTransition = popExit
    ) {
        composable<Destination.Library> {
            LibraryScreen(
                onOpenDocument = { documentId ->
                    navController.navigate(Destination.DocumentDetail(documentId))
                },
                onOpenSearch = { navController.navigate(Destination.Search) },
                onOpenRecentlyDeleted = { navController.navigate(Destination.RecentlyDeleted) },
                onNavigate = { destination -> navController.navigate(destination) }
            )
        }

        composable<Destination.RecentlyDeleted> {
            RecentlyDeletedScreen(onBack = navController::popBackStack)
        }

        // Scan + Review share one ScanSessionViewModel, scoped to this nested graph's own
        // back stack entry rather than to either screen individually — that's what lets you
        // navigate Scan -> Review -> (add a page, which pops back to Scan's launcher) -> Review
        // without the in-progress session resetting at any point in between. See
        // ScanSessionViewModel's class doc for why there's barely any state to lose either way.
        navigation<Destination.ScanFlow>(startDestination = Destination.Scan) {
            composable<Destination.Scan> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Destination.ScanFlow>()
                }
                val viewModel: ScanSessionViewModel = hiltViewModel(parentEntry)
                ScanScreen(
                    viewModel = viewModel,
                    onBack = navController::popBackStack,
                    onContinueToReview = { navController.navigate(Destination.Review) }
                )
            }
            composable<Destination.Review> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Destination.ScanFlow>()
                }
                val viewModel: ScanSessionViewModel = hiltViewModel(parentEntry)
                ReviewScreen(
                    viewModel = viewModel,
                    onBack = navController::popBackStack,
                    onSaveComplete = {
                        navController.navigate(Destination.Library) {
                            popUpTo<Destination.ScanFlow> { inclusive = true }
                        }
                    },
                    onOpenPremium = { navController.navigate(Destination.Unlock) }
                )
            }
        }

        composable<Destination.DocumentDetail> {
            // documentId itself is read from SavedStateHandle inside DocumentDetailViewModel
            // (Hilt's nav-arg integration wires it in automatically, keyed by
            // Destination.ARG_DOCUMENT_ID) rather than threaded through here as a screen
            // parameter.
            DocumentDetailScreen(
                onBack = navController::popBackStack,
                onOpenPremium = { navController.navigate(Destination.Unlock) }
            )
        }

        composable<Destination.Search> {
            SearchScreen(
                onBack = navController::popBackStack,
                onResultSelected = { documentId ->
                    navController.navigate(Destination.DocumentDetail(documentId))
                }
            )
        }

        composable<Destination.Receipts> {
            ReceiptsScreen(
                onBack = navController::popBackStack,
                onOpenDocument = { documentId ->
                    navController.navigate(Destination.DocumentDetail(documentId))
                }
            )
        }
        composable<Destination.Warranties> {
            WarrantiesScreen(
                onBack = navController::popBackStack,
                onOpenDocument = { documentId ->
                    navController.navigate(Destination.DocumentDetail(documentId))
                }
            )
        }
        composable<Destination.Settings> {
            SettingsScreen(
                onBack = navController::popBackStack,
                onOpenBackup = { navController.navigate(Destination.Backup) },
                onOpenPrivacy = { navController.navigate(Destination.Privacy) },
                onOpenPremium = { navController.navigate(Destination.Unlock) },
                onOpenAnalytics = { navController.navigate(Destination.Analytics) },
                onOpenDesignSystem = { navController.navigate(Destination.DesignSystem) },
                onOpenPipelineDebug = { navController.navigate(Destination.PipelineDebug) }
            )
        }
        composable<Destination.Backup> {
            BackupScreen(onBack = navController::popBackStack)
        }
        composable<Destination.Privacy> {
            PrivacyScreen(onBack = navController::popBackStack)
        }
        composable<Destination.Analytics> {
            AnalyticsScreen(onBack = navController::popBackStack)
        }
        composable<Destination.Unlock> {
            UnlockScreen(onBack = navController::popBackStack)
        }
        composable<Destination.DesignSystem> {
            DesignSystemScreen(onBack = navController::popBackStack)
        }
        composable<Destination.PipelineDebug> {
            PipelineDebugScreen(onBack = navController::popBackStack)
        }
    }
}

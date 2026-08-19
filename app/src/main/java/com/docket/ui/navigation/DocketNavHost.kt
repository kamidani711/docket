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
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.docket.ui.screens.account.AccountScreen
import com.docket.ui.screens.account.LanguageScreen
import com.docket.ui.screens.account.PersonalInfoScreen
import com.docket.ui.screens.account.ScanPreferencesScreen
import com.docket.ui.screens.account.SecurityScreen
import com.docket.ui.screens.analytics.AnalyticsScreen
import com.docket.ui.screens.backup.BackupScreen
import com.docket.ui.screens.debug.PipelineDebugScreen
import com.docket.ui.screens.designsystem.DesignSystemScreen
import com.docket.ui.screens.documentdetail.DocumentDetailScreen
import com.docket.ui.screens.home.HomeScreen
import com.docket.ui.screens.library.LibraryScreen
import com.docket.ui.screens.library.RecentlyDeletedScreen
import com.docket.ui.screens.privacy.PrivacyScreen
import com.docket.ui.screens.profile.ResetProfileScreen
import com.docket.ui.screens.profile.SignInScreen
import com.docket.ui.screens.profile.SignUpScreen
import com.docket.ui.screens.receipts.ReceiptsScreen
import com.docket.ui.screens.review.ReviewScreen
import com.docket.ui.screens.scan.ScanScreen
import com.docket.ui.screens.scan.ScanSessionViewModel
import com.docket.ui.screens.search.SearchScreen
import com.docket.ui.screens.unlock.UnlockScreen
import com.docket.ui.screens.warranties.WarrantiesScreen

@Composable
fun DocketNavHost(
    navController: NavHostController = rememberNavController(),
    pendingDocumentId: Long? = null,
    onPendingDocumentIdConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
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
        startDestination = Destination.Home,
        modifier = modifier,
        enterTransition = enter,
        exitTransition = exit,
        popEnterTransition = popEnter,
        popExitTransition = popExit
    ) {
        composable<Destination.Home> {
            HomeScreen(
                onNavigate = { destination -> navController.navigate(destination) },
                onOpenDocument = { documentId -> navController.navigate(Destination.DocumentDetail(documentId)) },
                onOpenSearch = { navController.navigate(Destination.Search) }
            )
        }

        composable<Destination.Files> {
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
                val scanFlow = parentEntry.toRoute<Destination.ScanFlow>()
                val viewModel: ScanSessionViewModel = hiltViewModel(parentEntry)
                ScanScreen(
                    viewModel = viewModel,
                    launch = scanFlow.launch,
                    onBack = navController::popBackStack,
                    onOpenFiles = {
                        navController.navigate(Destination.Files) {
                            popUpTo<Destination.ScanFlow> { inclusive = true }
                        }
                    },
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
                        navController.navigate(Destination.Files) {
                            popUpTo<Destination.ScanFlow> { inclusive = true }
                        }
                    },
                    onDiscard = {
                        navController.navigate(Destination.Home) {
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

        composable<Destination.Unlock> {
            UnlockScreen(onBack = navController::popBackStack)
        }

        composable<Destination.Account> {
            AccountScreen(
                onOpenPersonalInfo = { navController.navigate(Destination.PersonalInfo) },
                onOpenScanPreferences = { navController.navigate(Destination.ScanPreferences) },
                onOpenSecurity = { navController.navigate(Destination.Security) },
                onOpenLanguage = { navController.navigate(Destination.Language) },
                onOpenBackup = { navController.navigate(Destination.Backup) },
                onOpenRecentlyDeleted = { navController.navigate(Destination.RecentlyDeleted) },
                onOpenPrivacy = { navController.navigate(Destination.Privacy) },
                onOpenAnalytics = { navController.navigate(Destination.Analytics) },
                onOpenPremium = { navController.navigate(Destination.Unlock) },
                onOpenSignIn = { navController.navigate(Destination.SignIn) },
                onOpenSignUp = { navController.navigate(Destination.SignUp) },
                onOpenDesignSystem = { navController.navigate(Destination.DesignSystem) },
                onOpenPipelineDebug = { navController.navigate(Destination.PipelineDebug) }
            )
        }
        composable<Destination.PersonalInfo> {
            PersonalInfoScreen(onBack = navController::popBackStack)
        }
        composable<Destination.ScanPreferences> {
            ScanPreferencesScreen(onBack = navController::popBackStack)
        }
        composable<Destination.Security> {
            SecurityScreen(onBack = navController::popBackStack)
        }
        composable<Destination.Language> {
            LanguageScreen(onBack = navController::popBackStack)
        }
        composable<Destination.SignIn> {
            SignInScreen(
                onBack = navController::popBackStack,
                onSignedIn = navController::popBackStack,
                onOpenSignUp = {
                    navController.navigate(Destination.SignUp) { popUpTo<Destination.SignIn> { inclusive = true } }
                },
                onOpenReset = { navController.navigate(Destination.ResetProfile) }
            )
        }
        composable<Destination.SignUp> {
            SignUpScreen(
                onBack = navController::popBackStack,
                onSignedUp = navController::popBackStack,
                onOpenSignIn = {
                    navController.navigate(Destination.SignIn) { popUpTo<Destination.SignUp> { inclusive = true } }
                }
            )
        }
        composable<Destination.ResetProfile> {
            ResetProfileScreen(onBack = navController::popBackStack, onReset = navController::popBackStack)
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
        composable<Destination.DesignSystem> {
            DesignSystemScreen(onBack = navController::popBackStack)
        }
        composable<Destination.PipelineDebug> {
            PipelineDebugScreen(onBack = navController::popBackStack)
        }
    }
}

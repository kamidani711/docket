package com.docket.ui.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * The composition root above [DocketNavHost]: owns the single shared `NavController` and wraps
 * the host in a `Scaffold` whose `bottomBar` only renders on [Destination.TOP_LEVEL] routes
 * (Home/Files/Premium/Account) — every other screen (Scan, Review, Document detail, Search,
 * Settings sub-screens, ...) is a full-screen push with the bar hidden, same as before this
 * screen existed. Each screen's own `Scaffold` (topBar, content) is untouched; only the bottom
 * inset changes when the bar is showing, via [Modifier.consumeWindowInsets] on the NavHost so
 * neither Scaffold double-reserves space for the system nav bar.
 */
@Composable
fun DocketApp(pendingDocumentId: Long?, onPendingDocumentIdConsumed: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTopLevel = backStackEntry?.destination?.let { destination ->
        when {
            destination.hierarchy.any { it.hasRoute<Destination.Home>() } -> Destination.Home
            destination.hierarchy.any { it.hasRoute<Destination.Files>() } -> Destination.Files
            destination.hierarchy.any { it.hasRoute<Destination.Unlock>() } -> Destination.Unlock
            destination.hierarchy.any { it.hasRoute<Destination.Account>() } -> Destination.Account
            else -> null
        }
    }

    Scaffold(
        bottomBar = {
            if (currentTopLevel != null) {
                DocketBottomNav(
                    currentDestination = currentTopLevel,
                    onNavigate = { destination ->
                        navController.navigate(destination) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        DocketNavHost(
            navController = navController,
            pendingDocumentId = pendingDocumentId,
            onPendingDocumentIdConsumed = onPendingDocumentIdConsumed,
            modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .consumeWindowInsets(innerPadding)
        )
    }
}

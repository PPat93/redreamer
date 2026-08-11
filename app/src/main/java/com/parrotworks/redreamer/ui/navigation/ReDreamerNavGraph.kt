package com.parrotworks.redreamer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.parrotworks.redreamer.ui.bin.BinScreen
import com.parrotworks.redreamer.ui.detail.DreamDetailScreen
import com.parrotworks.redreamer.ui.editor.DreamEditorScreen
import com.parrotworks.redreamer.ui.home.HomeScreen
import com.parrotworks.redreamer.ui.tags.TagManagementScreen

/**
 * Navigation is only accepted while the current destination is actually resumed.
 *
 * Two taps in quick succession otherwise queue two navigations — the first hasn't finished
 * animating, so the click handler is still live. That means two detail screens stacked on one dream,
 * or worse: two editors, each with its own ViewModel autosaving, producing two dreams from a single
 * tap on the FAB. Mid-transition the outgoing entry is no longer RESUMED, so the second call is
 * dropped.
 */
private fun NavHostController.navigateOnce(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route)
    }
}

/** Same guard for going back, so a double tap can't pop two screens. */
private fun NavHostController.popOnce() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}

@Composable
fun ReDreamerNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) { entry ->
            val deletedCount by entry.savedStateHandle
                .getStateFlow(Destinations.RESULT_DREAMS_DELETED, 0)
                .collectAsStateWithLifecycle()

            HomeScreen(
                onDreamClick = { id -> navController.navigateOnce(Destinations.dreamDetail(id)) },
                onAddDreamClick = { navController.navigateOnce(Destinations.dreamEditorNew()) },
                onBinClick = { navController.navigateOnce(Destinations.BIN) },
                onManageTagsClick = { navController.navigateOnce(Destinations.TAG_MANAGEMENT) },
                deletedCount = deletedCount,
                onDeletedMessageShown = {
                    entry.savedStateHandle[Destinations.RESULT_DREAMS_DELETED] = 0
                },
            )
        }

        composable(Destinations.BIN) {
            BinScreen(onBack = { navController.popOnce() })
        }

        composable(Destinations.TAG_MANAGEMENT) {
            TagManagementScreen(onBack = { navController.popOnce() })
        }

        composable(
            route = Destinations.DREAM_DETAIL,
            arguments = listOf(navArgument(Destinations.ARG_DREAM_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val dreamId = backStackEntry.arguments?.getLong(Destinations.ARG_DREAM_ID) ?: return@composable
            // DreamDetailViewModel reads the same dreamId itself via SavedStateHandle;
            // it's captured here only to build the edit-navigation route.
            DreamDetailScreen(
                onEditClick = { navController.navigateOnce(Destinations.dreamEditorEdit(dreamId)) },
                onBack = { navController.popOnce() },
                onDeleted = {
                    // Hand the confirmation to the list; this screen is about to disappear.
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(Destinations.RESULT_DREAMS_DELETED, 1)
                    navController.popOnce()
                },
            )
        }

        composable(
            route = Destinations.DREAM_EDITOR,
            arguments = listOf(
                navArgument(Destinations.ARG_DREAM_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            // dreamId is read by DreamEditorViewModel straight from SavedStateHandle,
            // which Hilt wires to this back stack entry's nav arguments automatically.
            DreamEditorScreen(
                onSaved = { navController.popOnce() },
                onCancel = { navController.popOnce() },
                onManageTagsClick = { navController.navigateOnce(Destinations.TAG_MANAGEMENT) },
            )
        }
    }
}

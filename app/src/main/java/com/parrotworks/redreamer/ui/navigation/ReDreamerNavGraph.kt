package com.parrotworks.redreamer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun ReDreamerNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) { entry ->
            val deletedCount by entry.savedStateHandle
                .getStateFlow(Destinations.RESULT_DREAMS_DELETED, 0)
                .collectAsStateWithLifecycle()

            HomeScreen(
                onDreamClick = { id -> navController.navigate(Destinations.dreamDetail(id)) },
                onAddDreamClick = { navController.navigate(Destinations.dreamEditorNew()) },
                onBinClick = { navController.navigate(Destinations.BIN) },
                onManageTagsClick = { navController.navigate(Destinations.TAG_MANAGEMENT) },
                deletedCount = deletedCount,
                onDeletedMessageShown = {
                    entry.savedStateHandle[Destinations.RESULT_DREAMS_DELETED] = 0
                },
            )
        }

        composable(Destinations.BIN) {
            BinScreen(onBack = { navController.popBackStack() })
        }

        composable(Destinations.TAG_MANAGEMENT) {
            TagManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Destinations.DREAM_DETAIL,
            arguments = listOf(navArgument(Destinations.ARG_DREAM_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val dreamId = backStackEntry.arguments?.getLong(Destinations.ARG_DREAM_ID) ?: return@composable
            // DreamDetailViewModel reads the same dreamId itself via SavedStateHandle;
            // it's captured here only to build the edit-navigation route.
            DreamDetailScreen(
                onEditClick = { navController.navigate(Destinations.dreamEditorEdit(dreamId)) },
                onBack = { navController.popBackStack() },
                onDeleted = {
                    // Hand the confirmation to the list; this screen is about to disappear.
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(Destinations.RESULT_DREAMS_DELETED, 1)
                    navController.popBackStack()
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
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() },
                onManageTagsClick = { navController.navigate(Destinations.TAG_MANAGEMENT) },
            )
        }
    }
}

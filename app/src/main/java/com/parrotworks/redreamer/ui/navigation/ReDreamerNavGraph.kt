package com.parrotworks.redreamer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.parrotworks.redreamer.ui.detail.DreamDetailScreen
import com.parrotworks.redreamer.ui.editor.DreamEditorScreen
import com.parrotworks.redreamer.ui.list.DreamListScreen

@Composable
fun ReDreamerNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.DREAM_LIST) {
        composable(Destinations.DREAM_LIST) {
            DreamListScreen(
                onDreamClick = { id -> navController.navigate(Destinations.dreamDetail(id)) },
                onAddClick = { navController.navigate(Destinations.dreamEditorNew()) },
            )
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
                onDeleted = { navController.popBackStack() },
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
            )
        }
    }
}

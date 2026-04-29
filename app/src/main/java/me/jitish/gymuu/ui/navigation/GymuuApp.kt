package me.jitish.gymuu.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import me.jitish.gymuu.ui.GymViewModel
import me.jitish.gymuu.ui.exercise.SelectExerciseScreen
import me.jitish.gymuu.ui.routine.RoutineLaunchScreen
import me.jitish.gymuu.ui.routine.RoutineListScreen
import me.jitish.gymuu.ui.routine.WorkoutDayScreen
import me.jitish.gymuu.ui.theme.GymBlack

@Composable
fun GymuuApp(viewModel: GymViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    Surface(color = GymBlack, modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.START) {
            composable(Routes.START) {
                RoutineLaunchScreen(state = state, navController = navController)
            }
            composable(Routes.ROUTINES) {
                RoutineListScreen(state = state, viewModel = viewModel, navController = navController)
            }
            composable(
                route = Routes.WORKOUT,
                arguments = listOf(
                    navArgument("routineId") { type = NavType.StringType },
                    navArgument("dayId") { type = NavType.StringType }
                )
            ) { entry ->
                val routineId = Uri.decode(entry.arguments?.getString("routineId").orEmpty())
                val dayId = Uri.decode(entry.arguments?.getString("dayId").orEmpty())
                WorkoutDayScreen(
                    state = state,
                    viewModel = viewModel,
                    navController = navController,
                    routineId = routineId,
                    dayId = dayId
                )
            }
            composable(
                route = Routes.SELECT,
                arguments = listOf(
                    navArgument("routineId") { type = NavType.StringType },
                    navArgument("dayId") { type = NavType.StringType },
                    navArgument("swapExerciseId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                SelectExerciseScreen(
                    state = state,
                    viewModel = viewModel,
                    navController = navController,
                    routineId = Uri.decode(entry.arguments?.getString("routineId").orEmpty()),
                    dayId = Uri.decode(entry.arguments?.getString("dayId").orEmpty()),
                    swapExerciseId = entry.arguments?.getString("swapExerciseId")?.let(Uri::decode)
                )
            }
        }
    }
}


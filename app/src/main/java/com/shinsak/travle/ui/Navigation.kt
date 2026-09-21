package com.shinsak.travle.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shinsak.travle.data.TripRepository
import com.shinsak.travle.ui.components.Ease
import com.shinsak.travle.ui.components.Tab
import com.shinsak.travle.ui.screens.CompareScreen
import com.shinsak.travle.ui.screens.DetailScreen
import com.shinsak.travle.ui.screens.EditScreen
import com.shinsak.travle.ui.screens.HomeScreen
import com.shinsak.travle.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val COMPARE = "compare"
    const val SETTINGS = "settings"
    const val EDIT = "edit?tripId={tripId}"
    const val DETAIL = "detail/{tripId}"

    fun edit(tripId: String? = null) = if (tripId == null) "edit" else "edit?tripId=$tripId"
    fun detail(tripId: String) = "detail/$tripId"
}

private fun NavHostController.goTab(tab: Tab) {
    when (tab) {
        Tab.HOME -> popBackStack(Routes.HOME, inclusive = false)
        Tab.INPUT -> navigate(Routes.edit()) { launchSingleTop = true }
        Tab.COMPARE -> navigate(Routes.COMPARE) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
        Tab.SETTINGS -> navigate(Routes.SETTINGS) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }
}

@Composable
fun TravleNavHost(repo: TripRepository) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = {
            fadeIn(tween(240, easing = Ease)) + slideInHorizontally(tween(340, easing = Ease)) { it / 7 }
        },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(240, easing = Ease)) },
        popExitTransition = {
            fadeOut(tween(200)) + slideOutHorizontally(tween(320, easing = Ease)) { it / 7 }
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                repo = repo,
                onOpen = { nav.navigate(Routes.detail(it)) },
                onAdd = { nav.navigate(Routes.edit()) },
                onTab = { nav.goTab(it) },
            )
        }
        composable(Routes.COMPARE) {
            CompareScreen(
                repo = repo,
                onBack = { nav.goTab(Tab.HOME) },
                onTab = { nav.goTab(it) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(repo = repo, onTab = { nav.goTab(it) })
        }
        composable(
            route = Routes.EDIT,
            arguments = listOf(
                navArgument("tripId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            EditScreen(
                repo = repo,
                tripId = entry.arguments?.getString("tripId"),
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("tripId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("tripId") ?: return@composable
            DetailScreen(
                repo = repo,
                tripId = id,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.edit(id)) },
                onCompare = {
                    repo.addCompare(id)
                    nav.goTab(Tab.COMPARE)
                },
            )
        }
    }
}

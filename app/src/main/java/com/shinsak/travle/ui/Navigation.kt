package com.shinsak.travle.ui

import android.net.Uri
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
import com.shinsak.travle.ui.screens.BookingScreen
import com.shinsak.travle.ui.screens.ChecklistScreen
import com.shinsak.travle.ui.screens.CityScreen
import com.shinsak.travle.ui.screens.DatesScreen
import com.shinsak.travle.ui.screens.ExpenseEditScreen
import com.shinsak.travle.ui.screens.GuideScreen
import com.shinsak.travle.ui.screens.InfoScreen
import com.shinsak.travle.ui.screens.LedgerScreen
import com.shinsak.travle.ui.screens.PlaceEditScreen
import com.shinsak.travle.ui.screens.SettingsScreen
import com.shinsak.travle.ui.screens.TripHomeScreen
import com.shinsak.travle.ui.screens.TripsScreen

object Routes {
    const val TRIPS = "trips"
    const val SETTINGS = "settings"
    const val CITY = "new/city"
    const val DATES = "new/dates/{city}/{currency}"
    const val TRIP = "trip/{id}"
    const val PLACE = "trip/{id}/place?itemId={itemId}&day={day}"
    const val LEDGER = "trip/{id}/ledger"
    const val EXPENSE = "trip/{id}/expense?expId={expId}&day={day}"
    const val CHECKS = "trip/{id}/checks"
    const val BOOKING = "trip/{id}/booking"
    const val GUIDE = "trip/{id}/guide?editor={editor}"
    const val INFO = "trip/{id}/info"

    fun dates(city: String, currency: String) = "new/dates/${Uri.encode(city)}/$currency"
    fun trip(id: String) = "trip/$id"
    fun place(id: String, itemId: String? = null, day: Int = 1) = "trip/$id/place?day=$day" + (itemId?.let { "&itemId=$it" } ?: "")
    fun ledger(id: String) = "trip/$id/ledger"
    fun expense(id: String, expId: String? = null, day: Int = 0) = "trip/$id/expense?day=$day" + (expId?.let { "&expId=$it" } ?: "")
    fun checks(id: String) = "trip/$id/checks"
    fun booking(id: String) = "trip/$id/booking"
    fun guide(id: String, editor: Boolean) = "trip/$id/guide?editor=$editor"
    fun info(id: String) = "trip/$id/info"
}

private fun NavHostController.goTab(tripId: String, tab: Tab) {
    val route = when (tab) {
        Tab.PLAN -> Routes.trip(tripId)
        Tab.LEDGER -> Routes.ledger(tripId)
        Tab.CHECK -> Routes.checks(tripId)
        Tab.BOOKING -> Routes.booking(tripId)
        Tab.INFO -> Routes.info(tripId)
    }
    if (tab == Tab.PLAN) {
        popBackStack(Routes.trip(tripId), inclusive = false)
        return
    }
    navigate(route) {
        popUpTo(Routes.trip(tripId)) { inclusive = false }
        launchSingleTop = true
    }
}

@Composable
fun TravleNavHost(repo: TripRepository) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = Routes.TRIPS,
        enterTransition = { fadeIn(tween(240, easing = Ease)) + slideInHorizontally(tween(340, easing = Ease)) { it / 7 } },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(240, easing = Ease)) },
        popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(320, easing = Ease)) { it / 7 } },
    ) {
        composable(Routes.TRIPS) {
            TripsScreen(
                repo = repo,
                onOpen = { nav.navigate(Routes.trip(it)) },
                onNew = { nav.navigate(Routes.CITY) },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) { SettingsScreen(repo = repo, onBack = { nav.popBackStack() }) }
        composable(Routes.CITY) {
            CityScreen(onBack = { nav.popBackStack() }, onNext = { city, cur -> nav.navigate(Routes.dates(city, cur)) })
        }
        composable(
            Routes.DATES,
            arguments = listOf(navArgument("city") { type = NavType.StringType }, navArgument("currency") { type = NavType.StringType }),
        ) { entry ->
            DatesScreen(
                repo = repo,
                city = entry.arguments?.getString("city") ?: "",
                currency = entry.arguments?.getString("currency") ?: "KRW",
                onBack = { nav.popBackStack() },
                onCreated = { id ->
                    nav.navigate(Routes.trip(id)) { popUpTo(Routes.TRIPS) { inclusive = false } }
                },
            )
        }
        composable(Routes.TRIP, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            TripHomeScreen(
                repo = repo, tripId = id,
                onBack = { nav.popBackStack(Routes.TRIPS, inclusive = false) },
                onTab = { nav.goTab(id, it) },
                onAddPlace = { day -> nav.navigate(Routes.place(id, day = day)) },
                onEditPlace = { itemId -> nav.navigate(Routes.place(id, itemId = itemId)) },
            )
        }
        composable(
            Routes.PLACE,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("itemId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("day") { type = NavType.IntType; defaultValue = 1 },
            ),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            PlaceEditScreen(
                repo = repo, tripId = id,
                itemId = entry.arguments?.getString("itemId"),
                initialDay = entry.arguments?.getInt("day") ?: 1,
                onBack = { nav.popBackStack() },
                onBrowse = { nav.navigate(Routes.guide(id, editor = true)) },
            )
        }
        composable(Routes.LEDGER, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            LedgerScreen(
                repo = repo, tripId = id,
                onBack = { nav.goTab(id, Tab.PLAN) },
                onTab = { nav.goTab(id, it) },
                onAdd = { day -> nav.navigate(Routes.expense(id, day = day)) },
                onEdit = { expId -> nav.navigate(Routes.expense(id, expId = expId)) },
            )
        }
        composable(
            Routes.EXPENSE,
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("expId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("day") { type = NavType.IntType; defaultValue = 0 },
            ),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            ExpenseEditScreen(
                repo = repo, tripId = id,
                expId = entry.arguments?.getString("expId"),
                initialDay = entry.arguments?.getInt("day") ?: 0,
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            Routes.GUIDE,
            arguments = listOf(navArgument("id") { type = NavType.StringType }, navArgument("editor") { type = NavType.BoolType; defaultValue = true }),
        ) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            val fromEditor = entry.arguments?.getBoolean("editor") ?: true
            GuideScreen(
                repo = repo, tripId = id,
                onBack = { nav.popBackStack() },
                onPick = { p ->
                    repo.guidePick.value = p
                    if (fromEditor) nav.popBackStack() else nav.navigate(Routes.place(id, day = 1))
                },
            )
        }
        composable(Routes.INFO, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            InfoScreen(
                repo = repo, tripId = id,
                onBack = { nav.goTab(id, Tab.PLAN) },
                onTab = { nav.goTab(id, it) },
                onGuide = { nav.navigate(Routes.guide(id, editor = false)) },
            )
        }
        composable(Routes.CHECKS, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            ChecklistScreen(repo = repo, tripId = id, onBack = { nav.goTab(id, Tab.PLAN) }, onTab = { nav.goTab(id, it) })
        }
        composable(Routes.BOOKING, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id") ?: return@composable
            BookingScreen(repo = repo, tripId = id, onBack = { nav.goTab(id, Tab.PLAN) }, onTab = { nav.goTab(id, it) })
        }
    }
}

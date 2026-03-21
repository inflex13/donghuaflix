package com.donghuaflix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.donghuaflix.ui.browse.BrowseScreen
import com.donghuaflix.ui.detail.DetailScreen
import com.donghuaflix.ui.home.HomeScreen
import com.donghuaflix.ui.player.PlayerScreen
import com.donghuaflix.ui.search.SearchScreen
import com.donghuaflix.ui.watchlist.WatchlistScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Browse : Screen("browse?genre={genre}") {
        fun createRoute(genre: String? = null) = "browse?genre=${genre ?: ""}"
    }
    data object Detail : Screen("detail/{showId}?resumeEp={resumeEp}") {
        fun createRoute(showId: Int, resumeEp: Int? = null) =
            "detail/$showId?resumeEp=${resumeEp ?: ""}"
    }
    data object Player : Screen("player/{showId}/{episodeNumber}?website={website}") {
        fun createRoute(showId: Int, episodeNumber: Int, website: String? = null) =
            "player/$showId/$episodeNumber?website=${website ?: ""}"
    }
    data object Search : Screen("search")
    data object Watchlist : Screen("watchlist")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onShowClick = { showId, resumeEp -> navController.navigate(Screen.Detail.createRoute(showId, resumeEp)) },
                onBrowseClick = { genre -> navController.navigate(Screen.Browse.createRoute(genre)) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onWatchlistClick = { navController.navigate(Screen.Watchlist.route) },
            )
        }

        composable(
            route = Screen.Browse.route,
            arguments = listOf(navArgument("genre") { type = NavType.StringType; defaultValue = "" }),
        ) { backStackEntry ->
            val genre = backStackEntry.arguments?.getString("genre")?.takeIf { it.isNotBlank() }
            BrowseScreen(
                genre = genre,
                onShowClick = { showId -> navController.navigate(Screen.Detail.createRoute(showId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("showId") { type = NavType.IntType },
                navArgument("resumeEp") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { backStackEntry ->
            val showId = backStackEntry.arguments?.getInt("showId") ?: return@composable
            val resumeEp = backStackEntry.arguments?.getString("resumeEp")?.toIntOrNull()
            DetailScreen(
                showId = showId,
                resumeEpisode = resumeEp,
                onPlayEpisode = { epNum, website ->
                    navController.navigate(Screen.Player.createRoute(showId, epNum, website))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("showId") { type = NavType.IntType },
                navArgument("episodeNumber") { type = NavType.IntType },
                navArgument("website") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { backStackEntry ->
            val showId = backStackEntry.arguments?.getInt("showId") ?: return@composable
            val episodeNumber = backStackEntry.arguments?.getInt("episodeNumber") ?: return@composable
            val website = backStackEntry.arguments?.getString("website")?.takeIf { it.isNotBlank() }
            PlayerScreen(
                showId = showId,
                episodeNumber = episodeNumber,
                website = website,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onShowClick = { showId -> navController.navigate(Screen.Detail.createRoute(showId)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Watchlist.route) {
            WatchlistScreen(
                onShowClick = { showId -> navController.navigate(Screen.Detail.createRoute(showId)) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}

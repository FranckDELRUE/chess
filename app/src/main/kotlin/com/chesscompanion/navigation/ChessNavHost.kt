package com.chesscompanion.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chesscompanion.BuildConfig
import com.chesscompanion.enginedebug.EngineDebugScreen
import com.chesscompanion.feature.play.PlayScreen
import com.chesscompanion.feature.repertoire.REPERTOIRE_ID_ARG
import com.chesscompanion.feature.repertoire.RepertoireEditorScreen
import com.chesscompanion.feature.repertoire.RepertoireListScreen
import com.chesscompanion.feature.training.DRILL_REPERTOIRE_ID_ARG
import com.chesscompanion.feature.training.DrillScreen
import com.chesscompanion.feature.training.GAPS_REPERTOIRE_ID_ARG
import com.chesscompanion.feature.training.GapsScreen
import com.chesscompanion.home.HomeScreen

@Composable
fun ChessNavHost() {
    val navController = rememberNavController()
    val entries = NavigationEntries(debugEnabled = BuildConfig.DEBUG)
    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                onOpenPlay = { navController.navigate(ROUTE_PLAY) },
                onOpenPlayEngine = { navController.navigate(ROUTE_PLAY_ENGINE) },
                onOpenRepertoires = { navController.navigate(ROUTE_REPERTOIRES) },
                onOpenEngineDebug =
                    if (entries.showEngineDebug) {
                        { navController.navigate(ROUTE_ENGINE_DEBUG) }
                    } else {
                        null
                    },
            )
        }
        composable(ROUTE_REPERTOIRES) {
            RepertoireListScreen(
                onOpen = { navController.navigate(repertoireRoute(it)) },
                onDrill = { navController.navigate(drillRoute(it)) },
                onGaps = { navController.navigate(gapsRoute(it)) },
            )
        }
        composable(
            ROUTE_REPERTOIRE_PATTERN,
            arguments =
                listOf(
                    navArgument(REPERTOIRE_ID_ARG) {
                        type =
                            NavType.StringType
                    },
                ),
        ) {
            RepertoireEditorScreen()
        }
        composable(
            ROUTE_DRILL_PATTERN,
            arguments = listOf(navArgument(DRILL_REPERTOIRE_ID_ARG) { type = NavType.StringType }),
        ) {
            DrillScreen()
        }
        composable(
            ROUTE_GAPS_PATTERN,
            arguments = listOf(navArgument(GAPS_REPERTOIRE_ID_ARG) { type = NavType.StringType }),
        ) {
            GapsScreen()
        }
        composable(ROUTE_PLAY_PATTERN, arguments = listOf(navArgument("vs") { defaultValue = "" })) { PlayScreen() }
        if (entries.showEngineDebug) {
            composable(ROUTE_ENGINE_DEBUG) { EngineDebugScreen() }
        }
    }
}

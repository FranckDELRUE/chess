package com.chesscompanion.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chesscompanion.enginedebug.EngineDebugScreen
import com.chesscompanion.feature.play.PlayScreen
import com.chesscompanion.home.HomeScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_ENGINE_DEBUG = "engine-debug"
private const val ROUTE_PLAY = "play"

@Composable
fun ChessNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                onOpenPlay = { navController.navigate(ROUTE_PLAY) },
                onOpenEngineDebug = { navController.navigate(ROUTE_ENGINE_DEBUG) },
            )
        }
        composable(ROUTE_PLAY) { PlayScreen() }
        composable(ROUTE_ENGINE_DEBUG) { EngineDebugScreen() }
    }
}

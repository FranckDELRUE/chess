package com.chesscompanion.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chesscompanion.enginedebug.EngineDebugScreen
import com.chesscompanion.home.HomeScreen
import com.chesscompanion.llmdebug.LlmDebugScreen

private const val ROUTE_HOME = "home"
private const val ROUTE_ENGINE_DEBUG = "engine-debug"
private const val ROUTE_LLM_DEBUG = "llm-debug"

@Composable
fun ChessNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROUTE_HOME) {
        composable(ROUTE_HOME) {
            HomeScreen(
                onOpenEngineDebug = { navController.navigate(ROUTE_ENGINE_DEBUG) },
                onOpenLlmDebug = { navController.navigate(ROUTE_LLM_DEBUG) },
            )
        }
        composable(ROUTE_ENGINE_DEBUG) { EngineDebugScreen() }
        composable(ROUTE_LLM_DEBUG) { LlmDebugScreen() }
    }
}

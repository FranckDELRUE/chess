package com.chesscompanion.navigation

internal const val ROUTE_HOME = "home"
internal const val ROUTE_ENGINE_DEBUG = "engine-debug"
internal const val ROUTE_PLAY = "play"
internal const val ROUTE_REPERTOIRES = "repertoires"
internal const val ROUTE_REPERTOIRE_PATTERN = "repertoire/{id}"

internal fun repertoireRoute(id: String) = "repertoire/$id"

internal const val ROUTE_DRILL_PATTERN = "drill/{id}"

internal fun drillRoute(id: String) = "drill/$id"

internal const val ROUTE_GAPS_PATTERN = "gaps/{id}"

internal fun gapsRoute(id: String) = "gaps/$id"

/** `vs=engine` opens the game against Stockfish (read by `PlayViewModel` from its saved state). */
internal const val ROUTE_PLAY_PATTERN = "play?vs={vs}"
internal const val ROUTE_PLAY_ENGINE = "play?vs=engine"

/** Which destinations exist; the debug ones are only reachable when [debugEnabled] (debug builds). */
internal class NavigationEntries(
    val debugEnabled: Boolean,
) {
    val routes: List<String> =
        buildList {
            add(ROUTE_HOME)
            add(ROUTE_PLAY_PATTERN)
            add(ROUTE_REPERTOIRES)
            add(ROUTE_REPERTOIRE_PATTERN)
            add(ROUTE_DRILL_PATTERN)
            add(ROUTE_GAPS_PATTERN)
            if (debugEnabled) add(ROUTE_ENGINE_DEBUG)
        }

    val showEngineDebug: Boolean get() = ROUTE_ENGINE_DEBUG in routes
}

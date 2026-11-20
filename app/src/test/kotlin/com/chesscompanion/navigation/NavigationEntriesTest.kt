package com.chesscompanion.navigation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NavigationEntriesTest {
    @Test
    fun `release exposes no debug route or entry`() {
        val entries = NavigationEntries(debugEnabled = false)
        assertFalse(ROUTE_ENGINE_DEBUG in entries.routes)
        assertFalse(entries.showEngineDebug)
    }

    @Test
    fun `debug exposes the engine debug route and entry`() {
        val entries = NavigationEntries(debugEnabled = true)
        assertTrue(ROUTE_ENGINE_DEBUG in entries.routes)
        assertTrue(entries.showEngineDebug)
    }

    @Test
    fun `game routes exist in both builds`() {
        for (debug in listOf(false, true)) {
            val routes = NavigationEntries(debug).routes
            assertTrue(ROUTE_HOME in routes)
            assertTrue(ROUTE_PLAY_PATTERN in routes)
            assertTrue(ROUTE_REPERTOIRES in routes)
            assertTrue(ROUTE_REPERTOIRE_PATTERN in routes)
        }
    }
}

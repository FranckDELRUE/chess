package com.chesscompanion.feature.play

import com.chesscompanion.core.engine.EngineStrength
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OpponentSettingsTest {
    @Test
    fun `settings survive their text form`() {
        listOf(
            OpponentSettings(),
            OpponentSettings(PlayerColor.BLACK, EngineStrength.Elo(3190), 10_000),
            OpponentSettings(PlayerColor.RANDOM, EngineStrength.Skill(0), 500),
        ).forEach { assertEquals(it, OpponentSettings.decode(it.encode())) }
    }

    @Test
    fun `a malformed or out of range text is refused`() {
        listOf(
            null,
            "",
            "WHITE|elo:1500",
            "GREEN|elo:1500|1000",
            "WHITE|elo:1000|1000",
            "WHITE|elo:abc|1000",
            "WHITE|skill:21|1000",
            "WHITE|full:1|1000",
            "WHITE|elo:1500|0",
            "WHITE|elo:1500|x",
        ).forEach { assertNull(OpponentSettings.decode(it), "decoded: $it") }
    }

    @Test
    fun `the opponent never plays at full strength`() {
        assertThrows<IllegalArgumentException> { OpponentSettings(strength = EngineStrength.Full) }
        assertThrows<IllegalArgumentException> { OpponentSettings(moveTimeMs = 0) }
    }

    @Test
    fun `defaults are an elo of 1500 and one second`() {
        val defaults = OpponentSettings()
        assertEquals(EngineStrength.Elo(1500), defaults.strength)
        assertEquals(1_000, defaults.moveTimeMs)
        assertEquals("Elo 1500", defaults.strengthLabel)
    }
}

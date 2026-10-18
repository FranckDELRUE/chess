package com.chesscompanion.spike.llm

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GenerationTest {
    @Test
    fun `stats from native microseconds`() {
        val s = GenerationStats.fromNative(longArrayOf(120, 2_400_000, 31, 30, 3_000_000, 2_450_000))
        assertEquals(120, s.promptTokens)
        assertEquals(2400.0, s.prefillMs)
        assertEquals(50.0, s.prefillTokensPerSecond, 1e-9)
        assertEquals(10.0, s.decodeTokensPerSecond, 1e-9)
        assertEquals(2450.0, s.firstTokenMs)
    }

    @Test
    fun `zero duration gives zero speed`() {
        assertEquals(0.0, GenerationStats.fromNative(longArrayOf(1, 0, 1, 0, 0, 0)).decodeTokensPerSecond)
    }

    @Test
    fun `wrong native size is rejected`() {
        assertThrows<IllegalArgumentException> { GenerationStats.fromNative(longArrayOf(1, 2, 3)) }
    }

    @Test
    fun `reasoning blocks are removed`() {
        assertEquals("Joue Cd2.", ReasoningFilter.strip("<think>\n\n</think>\n\nJoue Cd2."))
        assertEquals("Joue Cd2.", ReasoningFilter.strip("<think>le cavalier…</think> Joue Cd2. <think>encore"))
        assertEquals("Joue Cd2.", ReasoningFilter.strip("Joue Cd2."))
    }

    @Test
    fun `every test fact names a best move and both prompts carry it`() {
        TeacherPrompt.FACTS.forEach { fact ->
            assertTrue(fact.json.contains("\"best\""), fact.json)
            val json = TeacherPrompt.messages(fact, PromptVariant.JSON)
            assertEquals(ChatMessage.Role.SYSTEM, json.first().role)
            assertTrue(json.last().content.endsWith(fact.json))
            val french = TeacherPrompt.messages(fact, PromptVariant.FRENCH)
            assertTrue(french.last().content.contains(fact.french))
        }
    }

    @Test
    fun `French facts are faithful to their JSON`() {
        TeacherPrompt.FACTS.forEach { fact ->
            val report = FactFidelity.check(fact.json, fact.french)
            assertTrue(report.faithful, "${fact.french} -> $report")
            assertTrue(report.mentionsBest, fact.french)
        }
    }
}

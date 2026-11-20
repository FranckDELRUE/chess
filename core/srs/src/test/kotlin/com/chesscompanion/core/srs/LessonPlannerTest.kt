package com.chesscompanion.core.srs

import com.chesscompanion.core.chess.Side
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class LessonPlannerTest {
    private val now = Instant.parse("2026-03-10T08:00:00Z")

    // White: 1. d4 d5 2. c4 e6 and, as a variation of Black's first reply, 1. d4 Nf6 2. c4 g6.
    private val repertoire = repertoireOf(Side.WHITE, "1. d4 d5 2. c4 e6 *", "1. d4 Nf6 2. c4 g6 *")

    @Test
    fun theLessonListsThePlayersPositionsMainLineFirst() {
        val steps = LessonPlanner.plan(repertoire, emptyMap())
        assertEquals(listOf(0, 2, 2), steps.map { it.path.size })
        assertEquals(listOf("d4", "c4", "c4"), steps.map { it.expected.first().san })
        // The main line (1...d5) comes before the variation (1...Nf6).
        assertEquals("d5", steps[1].path.last().san)
        assertEquals("Nf6", steps[2].path.last().san)
    }

    @Test
    fun aPositionReachedByTwoLinesIsTaughtOnce() {
        val transposed = repertoireOf(Side.WHITE, "1. d4 Nf6 2. c4 e6 3. Nc3 *", "1. c4 e6 2. d4 Nf6 3. Nc3 *")
        val keys = LessonPlanner.plan(transposed, emptyMap()).map { it.id }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun aPositionWithACardIsLearned() {
        val first = LessonPlanner.plan(repertoire, emptyMap()).first()
        val steps = LessonPlanner.plan(repertoire, mapOf(first.id to learnedCard(now)))
        assertTrue(steps.first().learned)
        assertFalse(steps[1].learned)
    }

    @Test
    fun aLearnedCardIsDueTomorrowAfterAFirstSuccess() {
        val card = learnedCard(now)
        assertEquals(now + Duration.ofDays(1), card.due)
        assertEquals(1, card.reps)
    }

    @Test
    fun anEmptyRepertoireHasNoLesson() {
        assertTrue(LessonPlanner.plan(repertoireOf(Side.WHITE), emptyMap()).isEmpty())
    }
}

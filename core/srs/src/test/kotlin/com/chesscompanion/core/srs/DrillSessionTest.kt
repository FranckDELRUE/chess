package com.chesscompanion.core.srs

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.Repertoire
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class DrillSessionTest {
    private val now = Instant.parse("2026-03-10T07:00:00Z")
    private val zone = ZoneId.of("Europe/Paris")

    // White repertoire with two answers at the root (d4 and c4) so that any of them is correct.
    private val repertoire: Repertoire = repertoireOf(Side.WHITE, "1. d4 d5 *", "1. c4 e5 *")

    private fun session() = DrillSession(ReviewQueueBuilder.build(repertoire, emptyMap(), now, zone).items)

    private fun move(uci: String) = checkNotNull(Move.parseUci(uci))

    @Test
    fun theRightMoveWithoutHelpIsGoodAndAnyRepertoireAnswerCounts() {
        val session = session()
        val result = session.answer(move("c2c4"), now)
        assertTrue(result.isCorrect)
        assertEquals(Rating.Good, result.rating)
        assertEquals(1, result.updatedCard!!.reps)
        assertTrue(session.isFinished)
    }

    @Test
    fun aHintMakesItHard() {
        val result = session().answer(move("d2d4"), now, hintUsed = true)
        assertEquals(Rating.Hard, result.rating)
    }

    @Test
    fun theHintIsARepertoireMove() {
        val session = session()
        assertTrue(session.hint!!.move in session.current!!.expected.map { it.move })
    }

    @Test
    fun aDeviationIsAgainDueTomorrowWithTheExpectedMoves() {
        val session = session()
        val item = session.current!!
        val result = session.answer(move("e2e4"), now)
        val outcome = result.outcome as DrillOutcome.Deviation
        assertEquals(move("e2e4"), outcome.played)
        assertEquals(item.expected, outcome.expected)
        assertEquals(Rating.Again, result.rating)
        assertEquals(now + Duration.ofDays(1), result.updatedCard!!.due)
    }

    @Test
    fun aMissedPositionComesBackUngradedUntilFound() {
        val session = session()
        assertEquals(1, session.remaining)
        val graded = session.answer(move("e2e4"), now)
        assertNotNull(graded.updatedCard)
        assertEquals(1, session.remaining) // the miss went back to the end

        val wrongAgain = session.answer(move("e2e4"), now)
        assertNull(wrongAgain.rating)
        assertNull(wrongAgain.updatedCard)
        assertFalse(session.isFinished)

        val found = session.answer(move("d2d4"), now)
        assertTrue(found.isCorrect)
        assertNull(found.updatedCard) // practice only: the schedule keeps the first (failed) grade
        assertTrue(session.isFinished)
    }

    @Test
    fun answeringAnAlreadyStoredCardUpdatesIt() {
        val stored = Fsrs().review(FsrsCard(), Rating.Good, now - Duration.ofDays(3))
        val item =
            ReviewQueueBuilder
                .build(repertoire, emptyMap(), now, zone)
                .items
                .first()
                .copy(card = stored)
        val result = DrillSession(listOf(item)).answer(item.expected.first().move, now)
        assertEquals(2, result.updatedCard!!.reps)
        assertTrue(result.updatedCard!!.stability!! > stored.stability!!)
        assertNotNull(result.updatedCard!!.due)
    }

    @Test
    fun anEmptyQueueIsFinishedAndRefusesAnswers() {
        val session = DrillSession(emptyList())
        assertTrue(session.isFinished)
        assertNull(session.current)
        assertThrows(IllegalStateException::class.java) { session.answer(move("e2e4"), now) }
    }
}

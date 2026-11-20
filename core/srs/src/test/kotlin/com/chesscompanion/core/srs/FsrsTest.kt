package com.chesscompanion.core.srs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class FsrsTest {
    private val fsrs = Fsrs()
    private val t0 = Instant.parse("2026-01-01T09:00:00Z")

    private fun days(n: Long): Duration = Duration.ofDays(n)

    /** Reviews a card each time it falls due, with [ratings], and returns the successive intervals in days. */
    private fun intervalsFor(vararg ratings: Rating): List<Long> {
        var card = FsrsCard()
        var now = t0
        return ratings.map { rating ->
            card = fsrs.review(card, rating, now)
            val interval = Duration.between(now, card.due).toDays()
            now = card.due!!
            interval
        }
    }

    @Test
    fun `a new card is due and has no memory`() {
        val card = FsrsCard()
        assertTrue(card.isNew)
        assertTrue(card.isDue(t0))
        assertEquals(0.0, fsrs.retrievability(card, t0))
    }

    @Test
    fun `first review sets the published initial stability`() {
        val weights = FsrsParameters.DEFAULT_WEIGHTS
        Rating.entries.forEach { rating ->
            val card = fsrs.review(FsrsCard(), rating, t0)
            assertEquals(weights[rating.grade - 1], card.stability!!, 1e-9)
            assertEquals(1, card.reps)
            assertEquals(0, card.lapses)
            assertEquals(t0, card.lastReview)
        }
    }

    @Test
    fun `initial difficulty falls as the rating improves and stays in range`() {
        val difficulties = Rating.entries.map { fsrs.review(FsrsCard(), it, t0).difficulty!! }
        assertEquals(difficulties.sortedDescending(), difficulties)
        assertTrue(difficulties.all { it in 1.0..10.0 })
        assertEquals(5.2824, difficulties[2], 1e-3)
    }

    @Test
    fun `first intervals match the default parameters`() {
        assertEquals(1L, intervalsFor(Rating.Again).single())
        assertEquals(1L, intervalsFor(Rating.Hard).single())
        assertEquals(3L, intervalsFor(Rating.Good).single())
        assertEquals(16L, intervalsFor(Rating.Easy).single())
    }

    @Test
    fun `a failed line comes back the next day`() {
        val card = fsrs.review(FsrsCard(), Rating.Again, t0)
        assertEquals(t0 + days(1), card.due)
        assertFalse(card.isDue(t0 + Duration.ofHours(12)))
        assertTrue(card.isDue(t0 + days(1)))
    }

    @Test
    fun `forgetting a learned line brings it back the next day and counts a lapse`() {
        val learned = intervalsFor(Rating.Good, Rating.Good, Rating.Good)
        assertTrue(learned.last() > 1)
        var card = FsrsCard()
        var now = t0
        repeat(3) {
            card = fsrs.review(card, Rating.Good, now)
            now = card.due!!
        }
        val before = card.stability!!
        val lapsed = fsrs.review(card, Rating.Again, now)
        assertEquals(now + days(1), lapsed.due)
        assertEquals(1, lapsed.lapses)
        assertTrue(lapsed.stability!! < before)
    }

    @Test
    fun `a line passed three times spaces out by several days`() {
        val intervals = intervalsFor(Rating.Good, Rating.Good, Rating.Good)
        assertEquals(3L, intervals[0])
        assertTrue(intervals[1] > intervals[0], "intervals must grow: $intervals")
        assertTrue(intervals[2] > intervals[1], "intervals must grow: $intervals")
        assertTrue(intervals[2] >= 7, "third interval should span at least a week: $intervals")
    }

    @Test
    fun `easy spaces further than good and hard less`() {
        val hard = intervalsFor(Rating.Good, Rating.Hard).last()
        val good = intervalsFor(Rating.Good, Rating.Good).last()
        val easy = intervalsFor(Rating.Good, Rating.Easy).last()
        assertTrue(hard < good, "hard=$hard good=$good")
        assertTrue(good < easy, "good=$good easy=$easy")
    }

    @Test
    fun `retrievability is 90 percent at the due date and decreases with time`() {
        val card = fsrs.review(FsrsCard(), Rating.Good, t0)
        val atDue = fsrs.retrievability(card, card.due!!)
        assertEquals(0.9, atDue, 0.02)
        assertEquals(1.0, fsrs.retrievability(card, t0), 1e-9)
        assertTrue(fsrs.retrievability(card, t0 + days(30)) < atDue)
    }

    @Test
    fun `reviewing later than due grows stability more`() {
        val first = fsrs.review(FsrsCard(), Rating.Good, t0)
        val onTime = fsrs.review(first, Rating.Good, first.due!!)
        val late = fsrs.review(first, Rating.Good, first.due!! + days(10))
        assertTrue(late.stability!! > onTime.stability!!)
    }

    @Test
    fun `a same day passing review never shrinks stability`() {
        val first = fsrs.review(FsrsCard(), Rating.Good, t0)
        val again = fsrs.review(first, Rating.Good, t0 + Duration.ofHours(2))
        assertTrue(again.stability!! >= first.stability!!)
    }

    @Test
    fun `difficulty stays within bounds after many reviews`() {
        var card = FsrsCard()
        var now = t0
        repeat(40) { i ->
            card = fsrs.review(card, if (i % 2 == 0) Rating.Again else Rating.Hard, now)
            now = card.due!!
            assertTrue(card.difficulty!! in 1.0..10.0)
        }
        repeat(40) {
            card = fsrs.review(card, Rating.Easy, now)
            now = card.due!!
            assertTrue(card.difficulty!! in 1.0..10.0)
        }
    }

    @Test
    fun `interval is capped by the maximum`() {
        val capped = Fsrs(FsrsParameters(maximumIntervalDays = 5))
        val card = capped.review(FsrsCard(), Rating.Easy, t0)
        assertEquals(t0 + days(5), card.due)
    }

    @Test
    fun `higher desired retention gives shorter intervals`() {
        val strict = Fsrs(FsrsParameters(desiredRetention = 0.97))
        val relaxed = Fsrs(FsrsParameters(desiredRetention = 0.8))
        assertTrue(strict.nextIntervalDays(20.0) < relaxed.nextIntervalDays(20.0))
    }

    @Test
    fun `review does not mutate the previous card`() {
        val card = fsrs.review(FsrsCard(), Rating.Good, t0)
        val snapshot = card.copy()
        fsrs.review(card, Rating.Again, card.due!!)
        assertEquals(snapshot, card)
        assertNotNull(card.due)
        assertNull(FsrsCard().due)
    }

    @Test
    fun `parameters are validated`() {
        assertThrows(IllegalArgumentException::class.java) { FsrsParameters(weights = listOf(1.0)) }
        assertThrows(IllegalArgumentException::class.java) { FsrsParameters(desiredRetention = 0.5) }
        assertThrows(IllegalArgumentException::class.java) { FsrsParameters(maximumIntervalDays = 0) }
    }
}

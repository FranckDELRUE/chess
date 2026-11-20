package com.chesscompanion.core.srs

import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class ReviewQueueTest {
    private val fsrs = Fsrs()
    private val zone = ZoneId.of("Europe/Paris")
    private val morning = Instant.parse("2026-03-10T07:00:00Z")

    private fun graph(
        side: Side,
        vararg games: String,
    ): Repertoire = repertoireOf(side, *games)

    private val white = graph(Side.WHITE, "1. d4 Nf6 2. c4 e6 *", "1. d4 d5 2. c4 *")

    private fun queue(
        repertoire: Repertoire = white,
        cards: Map<PositionId, FsrsCard> = emptyMap(),
        now: Instant = morning,
        limits: QueueLimits = QueueLimits(),
    ) = ReviewQueueBuilder.build(repertoire, cards, now, zone, limits)

    @Test
    fun onlyThePlayersPositionsWithAnAnswerAreAsked() {
        val items = queue().items
        // White to move: the root, after 1.d4 Nf6 and after 1.d4 d5. Not after 2.c4 e6 (no answer) nor Black's.
        assertEquals(3, items.size)
        assertTrue(items.all { it.node.position.sideToMove == Side.WHITE && it.expected.isNotEmpty() })
        assertEquals(listOf(0, 2, 2), items.map { it.path.size })
    }

    @Test
    fun newPositionsComeTrunkFirstAndAreCapped() {
        val all = queue()
        assertEquals(3, all.totalNew)
        assertEquals(white.root, all.items.first().id)

        val capped = queue(limits = QueueLimits(maxNew = 2))
        assertEquals(2, capped.newCount)
        assertEquals(3, capped.totalNew)
    }

    @Test
    fun aBlackRepertoireStartsAfterWhitesMove() {
        val black = graph(Side.BLACK, "1. d4 d5 2. c4 e6 *")
        val items = queue(black).items
        assertEquals(listOf(1, 3), items.map { it.path.size })
        assertTrue(items.none { it.id == black.root })
    }

    @Test
    fun transposedPathsShareOneCard() {
        val transposed = graph(Side.WHITE, "1. d4 Nf6 2. c4 e6 3. Nc3 *", "1. c4 e6 2. d4 Nf6 3. Nc3 *")
        val ids = queue(transposed).items.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(4, ids.size)
    }

    @Test
    fun aCardDueTodayAppearsBeforeNewPositions() {
        val first = queue().items.first()
        val card = fsrs.review(FsrsCard(), Rating.Good, morning - Duration.ofDays(3))
        val items = queue(cards = mapOf(first.id to card)).items
        assertEquals(first.id, items.first().id)
        assertEquals(1, items.count { !it.isNew })
        assertEquals(2, items.count { it.isNew })
    }

    @Test
    fun aCardDueLaterTodayCountsButTomorrowDoesNot() {
        val id = queue().items.first().id
        val dueTonight = FsrsCard(1.0, 5.0, Instant.parse("2026-03-10T21:00:00Z"), morning, 1, 0)
        val dueTomorrow = dueTonight.copy(due = Instant.parse("2026-03-11T05:00:00Z"))
        assertEquals(1, queue(cards = mapOf(id to dueTonight)).dueCount)
        assertEquals(0, queue(cards = mapOf(id to dueTomorrow)).dueCount)
    }

    @Test
    fun mostOverdueComesFirst() {
        val (a, b) = queue().items.take(2).map { it.id }
        val older = fsrs.review(FsrsCard(), Rating.Good, morning - Duration.ofDays(20))
        val recent = fsrs.review(FsrsCard(), Rating.Good, morning - Duration.ofDays(5))
        val items = queue(cards = mapOf(a to recent, b to older)).items
        assertEquals(listOf(b, a), items.take(2).map { it.id })
    }

    @Test
    fun reviewLimitKeepsTheMostOverdue() {
        val ids = queue().items.map { it.id }
        val cards =
            ids
                .mapIndexed { i, id ->
                    id to
                        fsrs.review(FsrsCard(), Rating.Good, morning - Duration.ofDays(10L + i))
                }.toMap()
        val limited = queue(cards = cards, limits = QueueLimits(maxReviews = 1, maxNew = 0))
        assertEquals(3, limited.totalDue)
        assertEquals(listOf(ids.last()), limited.items.map { it.id })
    }

    @Test
    fun endOfDayIsTheLastMillisecondOfTheLocalDay() {
        val end = endOfDay(Instant.parse("2026-03-10T23:30:00Z"), zone)
        assertEquals(Instant.parse("2026-03-11T22:59:59.999Z"), end)
    }

    @Test
    fun aFailedLineIsNotDueTodayButIsTomorrow() {
        val id = queue().items.first().id
        val failed = fsrs.review(FsrsCard(), Rating.Again, morning)
        assertEquals(0, queue(cards = mapOf(id to failed)).dueCount)
        assertEquals(1, queue(cards = mapOf(id to failed), now = morning + Duration.ofDays(1)).dueCount)
    }

    @Test
    fun aLinePassedThreeTimesIsSpacedOutByDays() {
        val id = queue().items.first().id
        var card = FsrsCard()
        var now = morning
        val gaps =
            (1..3).map {
                card = fsrs.review(card, Rating.Good, now)
                val gap = Duration.between(now, card.due).toDays()
                now = card.due!!
                gap
            }
        assertTrue(gaps.zipWithNext().all { (a, b) -> b > a }, "gaps $gaps")
        assertEquals(0, queue(cards = mapOf(id to card), now = now - Duration.ofDays(1)).dueCount)
    }
}

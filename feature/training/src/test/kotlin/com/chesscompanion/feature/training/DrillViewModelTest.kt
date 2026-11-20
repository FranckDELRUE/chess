package com.chesscompanion.feature.training

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.repertoire.NodeNotes
import com.chesscompanion.core.chess.repertoire.Source
import com.chesscompanion.core.srs.Fsrs
import com.chesscompanion.core.srs.FsrsCard
import com.chesscompanion.core.srs.Rating
import com.chesscompanion.core.srs.ReviewQueueBuilder
import com.chesscompanion.core.ui.board.Arrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DrillViewModelTest {
    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val now = Instant.parse("2026-03-10T08:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("Europe/Paris"))
    private val engine = FakeEngine()

    // White answers at the root (d4) and after 1. d4 d5 (c4): two positions to drill.
    private val repertoire = repertoireOf(Side.WHITE, "1. d4 d5 2. c4 e6 *")

    private fun sq(name: String) = checkNotNull(Square.parse(name))

    /** Cards of both positions, reviewed long ago and due: the drill asks them as reviews. */
    private fun reviewedCards(): FakeReviewCardStore {
        val store = FakeReviewCardStore()
        val queue = ReviewQueueBuilder.build(repertoire, emptyMap(), now, clock.zone)
        queue.items.forEach {
            val card = Fsrs().review(FsrsCard(), Rating.Good, now - Duration.ofDays(30))
            runBlocking { store.put("r", it.id, card) }
        }
        return store
    }

    private fun drill(
        cards: FakeReviewCardStore,
        id: String = "r",
        clock: Clock = this.clock,
        graph: com.chesscompanion.core.chess.repertoire.Repertoire = repertoire,
    ) = DrillViewModel(
        SavedStateHandle(mapOf(DRILL_REPERTOIRE_ID_ARG to id)),
        FakeRepertoireStore("r", "Anglaise", graph),
        cards,
        scheduler(engine),
        clock,
    ).also { it.replyPauseMs = 0 }

    private fun DrillViewModel.play(
        from: String,
        to: String,
    ) {
        onAction(DrillAction.DragStart(sq(from)))
        onAction(DrillAction.Drop(sq(from), sq(to)))
    }

    private fun FakeReviewCardStore.answered() = stored.values.filter { it.lastReview == now }

    @Test
    fun anUnknownRepertoireIsNotFound() {
        assertEquals(DrillStatus.NOT_FOUND, drill(FakeReviewCardStore(), id = "x").state.value.status)
    }

    @Test
    fun aReviewedPositionIsAskedWithItsLine() {
        val state = drill(reviewedCards()).state.value
        assertEquals(DrillStatus.ASKING, state.status)
        assertEquals("Anglaise", state.name)
        assertEquals("", state.lineText)
        assertFalse(state.isNewPosition)
        assertEquals(2, state.remaining)
        assertEquals(Side.WHITE, state.toMove)
    }

    // --- learning (S8-5)

    @Test
    fun aNewPositionIsLearnedWithTheMoveAndItsArrowShown() {
        val state = drill(FakeReviewCardStore()).state.value
        assertEquals(DrillStatus.LEARNING, state.status)
        assertEquals(listOf("d4"), state.learnMoves)
        assertTrue(Arrow(sq("d2"), sq("d4")) in state.annotations.arrows)
        assertFalse(state.learnMiss)
        assertNull(state.hint)
    }

    @Test
    fun aWrongMoveWhileLearningIsNotGradedAndChangesNothing() {
        val cards = FakeReviewCardStore()
        val vm = drill(cards)
        vm.play("e2", "e4")
        val state = vm.state.value
        assertEquals(DrillStatus.LEARNING, state.status)
        assertTrue(state.learnMiss)
        assertTrue(cards.stored.isEmpty())
        assertEquals(DrillSummary(), state.summary)
    }

    @Test
    fun learningCreatesTheCardDueTomorrowAndCountsIt() {
        val cards = FakeReviewCardStore()
        val vm = drill(cards)
        vm.play("d2", "d4")
        val state = vm.state.value
        assertEquals(DrillStatus.FEEDBACK, state.status)
        val feedback = checkNotNull(state.feedback)
        assertTrue(feedback.learned)
        assertEquals("d4", feedback.playedSan)
        assertEquals(
            now + Duration.ofDays(1),
            cards.stored.values
                .single()
                .due,
        )

        vm.onAction(DrillAction.Continue)
        assertEquals(DrillStatus.LEARNING, vm.state.value.status)
        assertEquals("1. d4 d5", vm.state.value.lineText)
        vm.play("c2", "c4")
        vm.onAction(DrillAction.Continue)
        assertEquals(DrillStatus.FINISHED, vm.state.value.status)
        assertEquals(2, vm.state.value.summary.learned)
        assertEquals(0, vm.state.value.summary.graded)
        assertFalse(vm.state.value.nothingToReview)
    }

    @Test
    fun theNotesOfANewPositionAreShownWhileLearning() {
        val notes = NodeNotes(comment = "Plan : c4 puis Cc3", source = Source("Mon livre", "2", "14"))
        val noted = repertoire.withNotes(repertoire.root, notes)
        val state = drill(FakeReviewCardStore(), graph = noted).state.value
        assertEquals("Plan : c4 puis Cc3", state.learnComment)
        assertEquals("Mon livre · 2 · p. 14", state.learnSource)
    }

    @Test
    fun whatWasLearnedIsOnlyDueAgainLater() {
        val cards = FakeReviewCardStore()
        drill(cards).also {
            it.play("d2", "d4")
            it.onAction(DrillAction.Continue)
            it.play("c2", "c4")
            it.onAction(DrillAction.Continue)
        }
        val today = drill(cards).state.value
        assertEquals(DrillStatus.FINISHED, today.status)
        assertTrue(today.nothingToReview)

        val tomorrow = drill(cards, clock = Clock.fixed(now + Duration.ofDays(1), clock.zone)).state.value
        assertEquals(DrillStatus.ASKING, tomorrow.status)
        assertFalse(tomorrow.isNewPosition)
    }

    // --- reviews (S8-4)

    @Test
    fun aCorrectAnswerIsStoredAndFollowedByTheNextPosition() {
        val cards = reviewedCards()
        val vm = drill(cards)
        vm.play("d2", "d4")
        val feedback = checkNotNull(vm.state.value.feedback)
        assertTrue(feedback.correct)
        assertFalse(feedback.learned)
        assertEquals("d4", feedback.playedSan)
        assertEquals(2, cards.answered().single().reps)

        vm.onAction(DrillAction.Continue)
        val next = vm.state.value
        assertEquals(DrillStatus.ASKING, next.status)
        assertEquals("1. d4 d5", next.lineText)
        assertNull(next.feedback)
    }

    @Test
    fun aDeviationShowsTheFactsAndSchedulesTomorrow() {
        val cards = reviewedCards()
        val vm = drill(cards)
        vm.play("e2", "e4")
        val state = vm.state.value
        val feedback = checkNotNull(state.feedback)
        assertFalse(feedback.correct)
        assertFalse(feedback.retry)
        assertEquals("e4", feedback.playedSan)
        assertEquals(listOf("d4"), feedback.expectedSans)
        assertEquals("1. d4", feedback.line)
        assertEquals("-0,20", feedback.playedEvaluation)
        assertEquals("-0,20", feedback.expectedEvaluation)
        assertTrue(feedback.evaluationDone)
        assertEquals(2, engine.searched.size)
        assertEquals(2, state.annotations.arrows.size)
        assertTrue(Arrow(sq("d2"), sq("d4")) in state.annotations.arrows)

        assertEquals(now + Duration.ofDays(1), cards.answered().single().due)
    }

    @Test
    fun theMissedPositionComesBackAsAnUngradedRetry() {
        val cards = reviewedCards()
        val vm = drill(cards)
        vm.play("e2", "e4")
        vm.onAction(DrillAction.Continue)
        vm.play("c2", "c4")
        vm.onAction(DrillAction.Continue)
        val retry = vm.state.value
        assertEquals(DrillStatus.ASKING, retry.status)
        assertTrue(retry.isRetry)
        assertEquals("", retry.lineText)

        val before = cards.stored.values.toList()
        vm.play("d2", "d4")
        assertTrue(checkNotNull(vm.state.value.feedback).retry)
        assertEquals(before, cards.stored.values.toList())

        // The line goes on after the retry: the next position is known but no longer due, so it is practice only.
        vm.onAction(DrillAction.Continue)
        assertTrue(vm.state.value.isPractice)
        vm.play("c2", "c4")
        vm.onAction(DrillAction.Continue)
        assertEquals(DrillStatus.FINISHED, vm.state.value.status)
        assertEquals(DrillSummary(graded = 2, correct = 1, missed = 1), vm.state.value.summary)
    }

    // --- lines played from the start

    @Test
    fun theOpponentRepliesAreScriptedAndShownOnTheBoard() {
        val vm = drill(reviewedCards())
        vm.play("d2", "d4")
        vm.onAction(DrillAction.Continue)
        val state = vm.state.value
        assertEquals(DrillStatus.ASKING, state.status)
        assertEquals("1. d4 d5", state.lineText)
        assertEquals(sq("d5"), state.highlights.lastMove?.to)
    }

    @Test
    fun aBlackRepertoireStartsWithTheOpponentsFirstMove() {
        val black = repertoireOf(Side.BLACK, "1. d4 d5 2. c4 e6 *")
        val cards = FakeReviewCardStore()
        ReviewQueueBuilder.build(black, emptyMap(), now, clock.zone).items.forEach {
            val card = Fsrs().review(FsrsCard(), Rating.Good, now - Duration.ofDays(30))
            runBlocking { cards.put("r", it.id, card) }
        }
        val state = drill(cards, graph = black).state.value
        assertEquals(DrillStatus.ASKING, state.status)
        assertEquals("1. d4", state.lineText)
        assertEquals(Side.BLACK, state.toMove)
    }

    @Test
    fun aKnownPositionNotDueOnTheWayIsAskedAsUngradedPractice() {
        val cards = reviewedCards()
        // The first position was seen today: not due; only the second one is.
        val root = ReviewQueueBuilder.build(repertoire, emptyMap(), now, clock.zone).items.first()
        runBlocking { cards.put("r", root.id, Fsrs().review(FsrsCard(), Rating.Good, now)) }
        val before = cards.stored.values.toList()

        val vm = drill(cards)
        val first = vm.state.value
        assertTrue(first.isPractice)
        assertEquals("", first.lineText)
        vm.play("d2", "d4")
        assertTrue(checkNotNull(vm.state.value.feedback).retry)
        assertEquals(before, cards.stored.values.toList())

        vm.onAction(DrillAction.Continue)
        val target = vm.state.value
        assertEquals(DrillStatus.ASKING, target.status)
        assertFalse(target.isPractice)
        assertEquals("1. d4 d5", target.lineText)
    }

    @Test
    fun withoutAnEngineTheFeedbackStillComes() {
        engine.fail = true
        val vm = drill(reviewedCards())
        vm.play("e2", "e4")
        val feedback = checkNotNull(vm.state.value.feedback)
        assertNull(feedback.playedEvaluation)
        assertTrue(feedback.evaluationDone)
        assertFalse(feedback.correct)
    }

    @Test
    fun theHintIsARepertoireMoveAndGradesLowerThanWithoutIt() {
        val withHint = reviewedCards()
        val vm = drill(withHint)
        vm.onAction(DrillAction.ShowHint)
        assertEquals("d4", vm.state.value.hint)
        vm.play("d2", "d4")

        val without = reviewedCards()
        drill(without).play("d2", "d4")

        val hard = checkNotNull(withHint.answered().single().stability)
        val good = checkNotNull(without.answered().single().stability)
        assertTrue(hard < good, "Hard $hard should be below Good $good")
        assertNotNull(vm.state.value.feedback)
    }

    @Test
    fun anIllegalDropChangesNothing() {
        val cards = reviewedCards()
        val vm = drill(cards)
        vm.play("d2", "d5")
        assertEquals(DrillStatus.ASKING, vm.state.value.status)
        assertTrue(cards.answered().isEmpty())
    }

    @Test
    fun aWholeReviewSessionEndsWithItsSummary() {
        val cards = reviewedCards()
        val vm = drill(cards)
        vm.play("d2", "d4")
        vm.onAction(DrillAction.Continue)
        vm.play("c2", "c4")
        vm.onAction(DrillAction.Continue)
        val state = vm.state.value
        assertEquals(DrillStatus.FINISHED, state.status)
        assertEquals(DrillSummary(graded = 2, correct = 2, missed = 0), state.summary)
        assertEquals(2, cards.answered().size)
    }

    @Test
    fun anEmptyRepertoireHasNothingToReview() {
        val vm = drill(FakeReviewCardStore(), graph = repertoireOf(Side.WHITE))
        assertEquals(DrillStatus.FINISHED, vm.state.value.status)
        assertTrue(vm.state.value.nothingToReview)
    }
}

package com.chesscompanion.feature.training

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.board.Arrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val cards = FakeReviewCardStore()
    private val engine = FakeEngine()

    // White answers at the root (d4) and after 1. d4 d5 (c4): two positions to drill.
    private val repertoire = repertoireOf(Side.WHITE, "1. d4 d5 2. c4 e6 *")

    private fun sq(name: String) = checkNotNull(Square.parse(name))

    private fun drill(
        id: String = "r",
        clock: Clock = this.clock,
    ) = DrillViewModel(
        SavedStateHandle(mapOf(DRILL_REPERTOIRE_ID_ARG to id)),
        FakeRepertoireStore("r", "Anglaise", repertoire),
        cards,
        scheduler(engine),
        clock,
    )

    private fun DrillViewModel.play(
        from: String,
        to: String,
    ) {
        onAction(DrillAction.DragStart(sq(from)))
        onAction(DrillAction.Drop(sq(from), sq(to)))
    }

    @Test
    fun anUnknownRepertoireIsNotFound() {
        assertEquals(DrillStatus.NOT_FOUND, drill(id = "x").state.value.status)
    }

    @Test
    fun theFirstQuestionIsTheRootWithItsLine() {
        val state = drill().state.value
        assertEquals(DrillStatus.ASKING, state.status)
        assertEquals("Anglaise", state.name)
        assertEquals("", state.lineText)
        assertTrue(state.isNewPosition)
        assertEquals(2, state.remaining)
        assertEquals(Side.WHITE, state.toMove)
    }

    @Test
    fun aCorrectAnswerIsStoredAndFollowedByTheNextPosition() {
        val vm = drill()
        vm.play("d2", "d4")
        val feedback = checkNotNull(vm.state.value.feedback)
        assertTrue(feedback.correct)
        assertEquals("d4", feedback.playedSan)
        assertEquals(1, cards.stored.size)

        vm.onAction(DrillAction.Continue)
        val next = vm.state.value
        assertEquals(DrillStatus.ASKING, next.status)
        assertEquals("1. d4 d5", next.lineText)
        assertNull(next.feedback)
    }

    @Test
    fun aDeviationShowsTheFactsAndSchedulesTomorrow() {
        val vm = drill()
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

        assertEquals(
            now + Duration.ofDays(1),
            cards.stored.values
                .single()
                .due,
        )
    }

    @Test
    fun theMissedPositionComesBackAsAnUngradedRetry() {
        val vm = drill()
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

        vm.onAction(DrillAction.Continue)
        assertEquals(DrillStatus.FINISHED, vm.state.value.status)
        assertEquals(DrillSummary(graded = 2, correct = 1, missed = 1), vm.state.value.summary)
    }

    @Test
    fun withoutAnEngineTheFeedbackStillComes() {
        engine.fail = true
        val vm = drill()
        vm.play("e2", "e4")
        val feedback = checkNotNull(vm.state.value.feedback)
        assertNull(feedback.playedEvaluation)
        assertTrue(feedback.evaluationDone)
        assertFalse(feedback.correct)
    }

    @Test
    fun theHintIsARepertoireMoveAndMakesItHard() {
        val vm = drill()
        vm.onAction(DrillAction.ShowHint)
        assertEquals("d4", vm.state.value.hint)
        vm.play("d2", "d4")
        val stability =
            checkNotNull(
                cards.stored.values
                    .single()
                    .stability,
            )
        assertTrue(stability < GOOD_FIRST_STABILITY, "Hard starts below Good: $stability")
        assertNotNull(vm.state.value.feedback)
    }

    @Test
    fun anIllegalDropChangesNothing() {
        val vm = drill()
        vm.play("d2", "d5")
        assertEquals(DrillStatus.ASKING, vm.state.value.status)
        assertTrue(cards.stored.isEmpty())
    }

    @Test
    fun aWholeSessionEndsWithItsSummary() {
        val vm = drill()
        vm.play("d2", "d4")
        vm.onAction(DrillAction.Continue)
        vm.play("c2", "c4")
        vm.onAction(DrillAction.Continue)
        val state = vm.state.value
        assertEquals(DrillStatus.FINISHED, state.status)
        assertEquals(DrillSummary(graded = 2, correct = 2, missed = 0), state.summary)
        assertEquals(2, cards.stored.size)
        assertFalse(state.nothingToReview)
    }

    @Test
    fun whatWasLearnedTodayIsOnlyDueAgainLater() {
        drill().also {
            it.play("d2", "d4")
            it.onAction(DrillAction.Continue)
            it.play("c2", "c4")
            it.onAction(DrillAction.Continue)
        }
        val again = drill().state.value
        assertEquals(DrillStatus.FINISHED, again.status)
        assertTrue(again.nothingToReview)

        val later = drill(clock = Clock.fixed(now + Duration.ofDays(30), clock.zone)).state.value
        assertEquals(DrillStatus.ASKING, later.status)
        assertFalse(later.isNewPosition)
    }

    @Test
    fun anEmptyRepertoireHasNothingToReview() {
        val vm =
            DrillViewModel(
                SavedStateHandle(mapOf(DRILL_REPERTOIRE_ID_ARG to "r")),
                FakeRepertoireStore("r", "Vide", repertoireOf(Side.WHITE)),
                cards,
                scheduler(engine),
                clock,
            )
        assertEquals(DrillStatus.FINISHED, vm.state.value.status)
        assertTrue(vm.state.value.nothingToReview)
    }

    private companion object {
        const val GOOD_FIRST_STABILITY = 3.17
    }
}

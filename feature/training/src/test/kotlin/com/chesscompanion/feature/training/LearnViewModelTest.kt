package com.chesscompanion.feature.training

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.repertoire.NodeNotes
import com.chesscompanion.core.chess.repertoire.Source
import com.chesscompanion.core.data.explorer.ExplorerLevel
import com.chesscompanion.core.data.explorer.ExplorerLookup
import com.chesscompanion.core.data.explorer.ExplorerMove
import com.chesscompanion.core.data.explorer.ExplorerOutcome
import com.chesscompanion.core.data.explorer.ExplorerProblem
import com.chesscompanion.core.data.explorer.ExplorerStats
import com.chesscompanion.core.srs.learnedCard
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class LearnViewModelTest {
    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val now = Instant.parse("2026-03-10T08:00:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("Europe/Paris"))
    private val engine = FakeEngine()

    // White: 1. d4 (then 1...d5 2. c4 e6, and the variation 1...Nf6 2. c4 g6): three positions to learn.
    private val repertoire = repertoireOf(Side.WHITE, "1. d4 d5 2. c4 e6 *", "1. d4 Nf6 2. c4 g6 *")

    private fun sq(name: String) = checkNotNull(Square.parse(name))

    private class FakeExplorer(
        private val outcome: ExplorerOutcome,
    ) : ExplorerLookup {
        override suspend fun lookup(
            fen: String,
            level: ExplorerLevel,
        ) = outcome
    }

    private fun stats(vararg moves: Pair<String, Long>) =
        ExplorerOutcome.Loaded(
            ExplorerStats(
                white = moves.sumOf { it.second } / 2,
                draws = 0,
                black = moves.sumOf { it.second } / 2,
                moves = moves.map { ExplorerMove(it.first, it.second / 2, 0, it.second / 2, null) },
            ),
            fromCache = true,
        )

    private fun learn(
        cards: FakeReviewCardStore = FakeReviewCardStore(),
        id: String = "r",
        graph: com.chesscompanion.core.chess.repertoire.Repertoire = repertoire,
        outcome: ExplorerOutcome = ExplorerOutcome.Failed(ExplorerProblem.OFFLINE),
    ) = LearnViewModel(
        SavedStateHandle(mapOf(LEARN_REPERTOIRE_ID_ARG to id)),
        FakeRepertoireStore("r", "Ma Queen", graph),
        cards,
        scheduler(engine),
        FakeExplorer(outcome),
        clock,
    )

    private fun LearnViewModel.play(
        from: String,
        to: String,
    ) {
        onAction(LearnAction.Gesture(DrillAction.DragStart(sq(from))))
        onAction(LearnAction.Gesture(DrillAction.Drop(sq(from), sq(to))))
    }

    @Test
    fun anUnknownRepertoireIsNotFound() {
        assertEquals(LearnStatus.NOT_FOUND, learn(id = "x").state.value.status)
    }

    @Test
    fun aRepertoireWithoutAnswerHasNoLesson() {
        assertEquals(LearnStatus.EMPTY, learn(graph = repertoireOf(Side.WHITE)).state.value.status)
    }

    @Test
    fun theLessonStartsAtTheFirstPositionWithTheMoveAndItsArrow() {
        val state = learn().state.value
        assertEquals(LearnStatus.LESSON, state.status)
        assertEquals(0, state.index)
        assertEquals(3, state.total)
        assertEquals(0, state.learned)
        assertEquals("", state.lineText)
        assertEquals(listOf("d4"), state.facts.map { it.san })
        assertTrue(Arrow(sq("d2"), sq("d4")) in state.annotations.arrows)
    }

    @Test
    fun theRightMoveLearnsThePositionAndStoresACardDueTomorrow() {
        val cards = FakeReviewCardStore()
        val vm = learn(cards)
        vm.play("d2", "d4")
        val state = vm.state.value
        assertTrue(state.justLearned)
        assertTrue(state.stepLearned)
        assertEquals(1, state.learned)
        assertEquals(
            now + Duration.ofDays(1),
            cards.stored.values
                .single()
                .due,
        )
    }

    @Test
    fun aWrongMoveOnlyShowsAMessage() {
        val cards = FakeReviewCardStore()
        val vm = learn(cards)
        vm.play("e2", "e4")
        val state = vm.state.value
        assertTrue(state.miss)
        assertFalse(state.justLearned)
        assertTrue(cards.stored.isEmpty())
    }

    @Test
    fun theLessonWalksThroughThePositionsAndBack() {
        val vm = learn()
        vm.onAction(LearnAction.Next)
        assertEquals(1, vm.state.value.index)
        assertEquals("1. d4 d5", vm.state.value.lineText)
        vm.onAction(LearnAction.Next)
        assertEquals("1. d4 Cf6", vm.state.value.lineText)
        vm.onAction(LearnAction.Next)
        assertEquals(2, vm.state.value.index)
        vm.onAction(LearnAction.Previous)
        vm.onAction(LearnAction.Previous)
        vm.onAction(LearnAction.Previous)
        assertEquals(0, vm.state.value.index)
    }

    @Test
    fun theLessonOpensOnTheFirstPositionNotLearnedYet() {
        val cards = FakeReviewCardStore()
        val first =
            com.chesscompanion.core.srs.LessonPlanner
                .plan(repertoire, emptyMap())
                .first()
        runBlocking { cards.put("r", first.id, learnedCard(now)) }
        val state = learn(cards).state.value
        assertEquals(1, state.index)
        assertEquals(1, state.learned)
    }

    @Test
    fun theFactsGiveStockfishsEvaluationAndTheDatabaseFigures() {
        val vm = learn(outcome = stats("d2d4" to 8_000, "e2e4" to 12_000))
        val fact =
            vm.state.value.facts
                .single()
        assertEquals("d4", fact.san)
        assertEquals("-0,20", fact.evaluation)
        assertEquals(8_000, fact.games)
        assertEquals(40, fact.percent)
        assertEquals(50, fact.whitePercent)
        assertEquals(50, fact.blackPercent)
        assertTrue(vm.state.value.explorerLoaded)
        assertTrue(vm.state.value.evaluationsDone)
    }

    @Test
    fun aMoveNeverPlayedInTheDatabaseSaysSo() {
        val vm = learn(outcome = stats("e2e4" to 12_000))
        assertEquals(
            0,
            vm.state.value.facts
                .single()
                .games,
        )
    }

    @Test
    fun withoutNetworkOrEngineTheLessonStillWorks() {
        engine.fail = true
        val vm = learn()
        val state = vm.state.value
        assertEquals(ExplorerProblem.OFFLINE, state.explorerProblem)
        assertNull(state.facts.single().evaluation)
        assertTrue(state.evaluationsDone)
        vm.play("d2", "d4")
        assertTrue(vm.state.value.justLearned)
    }

    @Test
    fun theNotesOfThePositionAreShown() {
        val noted =
            repertoire.withNotes(
                repertoire.root,
                NodeNotes(comment = "Plan : c4", source = Source("Mon livre", "2", "14")),
            )
        val state = learn(graph = noted).state.value
        assertEquals("Plan : c4", state.comment)
        assertEquals("Mon livre · 2 · p. 14", state.source)
    }
}

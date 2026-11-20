package com.chesscompanion.feature.training

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.data.explorer.ExplorerLevel
import com.chesscompanion.core.data.explorer.ExplorerLookup
import com.chesscompanion.core.data.explorer.ExplorerMove
import com.chesscompanion.core.data.explorer.ExplorerOutcome
import com.chesscompanion.core.data.explorer.ExplorerProblem
import com.chesscompanion.core.data.explorer.ExplorerStats
import com.chesscompanion.core.data.explorer.GapFinder
import com.chesscompanion.core.data.explorer.LineCompleter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class GapsViewModelTest {
    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val repertoire = repertoireOf(Side.WHITE, "1. d4 d5 *")

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
            ExplorerStats(moves.sumOf { it.second }, 0, 0, moves.map { ExplorerMove(it.first, it.second, 0, 0, null) }),
            fromCache = true,
        )

    private val store = FakeRepertoireStore("r", "Ma Queen", repertoire)

    private fun gaps(
        outcome: ExplorerOutcome,
        id: String = "r",
    ) = GapsViewModel(
        SavedStateHandle(mapOf(GAPS_REPERTOIRE_ID_ARG to id)),
        store,
        GapFinder(FakeExplorer(outcome)),
        LineCompleter(FakeExplorer(outcome)),
    )

    @Test
    fun anUnknownRepertoireIsNotFound() {
        assertEquals(GapsStatus.NOT_FOUND, gaps(stats(), id = "x").state.value.status)
    }

    @Test
    fun nothingIsScannedUntilTheLaunch() {
        val state = gaps(stats()).state.value
        assertEquals(GapsStatus.IDLE, state.status)
        assertEquals("Ma Queen", state.name)
    }

    @Test
    fun theScanListsTheFrequentRepliesWithoutAnAnswer() {
        val vm = gaps(stats("d7d5" to 6_000, "g8f6" to 4_000))
        vm.onAction(GapsAction.Scan)
        val state = vm.state.value
        assertEquals(GapsStatus.DONE, state.status)
        assertNull(state.problem)
        val row = state.gaps.single()
        assertEquals(GapRow(line = "1. d4", moveSan = "Cf6", games = 4_000, percent = 40), row)
        assertEquals(1, state.scanned)
    }

    @Test
    fun aRepertoireWithoutHolesSaysSo() {
        val vm = gaps(stats("d7d5" to 6_000))
        vm.onAction(GapsAction.Scan)
        assertEquals(GapsStatus.DONE, vm.state.value.status)
        assertTrue(
            vm.state.value.gaps
                .isEmpty(),
        )
        assertNull(vm.state.value.problem)
    }

    @Test
    fun withoutNetworkTheProblemIsReported() {
        val vm = gaps(ExplorerOutcome.Failed(ExplorerProblem.OFFLINE))
        vm.onAction(GapsAction.Scan)
        assertEquals(GapsStatus.DONE, vm.state.value.status)
        assertEquals(ExplorerProblem.OFFLINE, vm.state.value.problem)
        assertEquals(0, vm.state.value.scanned)
    }

    @Test
    fun tappingAGapAddsItsMoveToTheRepertoireAndSavesIt() {
        val vm = gaps(stats("d7d5" to 6_000, "g8f6" to 4_000))
        vm.onAction(GapsAction.Scan)
        assertFalse(
            vm.state.value.gaps
                .single()
                .added,
        )

        vm.onAction(GapsAction.Add(0))
        assertTrue(
            vm.state.value.gaps
                .single()
                .added,
        )
        val saved = store.saved.single()
        assertEquals(repertoire.edgeCount + 1, saved.edgeCount)
        assertTrue(saved.allMoves.any { it.move.uci == "g8f6" })
    }

    @Test
    fun aGapIsOnlyAddedOnce() {
        val vm = gaps(stats("d7d5" to 6_000, "g8f6" to 4_000))
        vm.onAction(GapsAction.Scan)
        vm.onAction(GapsAction.Add(0))
        vm.onAction(GapsAction.Add(0))
        assertEquals(1, store.saved.size)
    }

    @Test
    fun anUnknownRowAddsNothing() {
        val vm = gaps(stats("d7d5" to 6_000, "g8f6" to 4_000))
        vm.onAction(GapsAction.Scan)
        vm.onAction(GapsAction.Add(5))
        assertTrue(store.saved.isEmpty())
    }
}

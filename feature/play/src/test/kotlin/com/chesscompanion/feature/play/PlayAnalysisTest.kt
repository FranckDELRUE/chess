package com.chesscompanion.feature.play

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Piece
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
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

/** S2-3: the live analysis seen through the game ViewModel, with a fake engine. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayAnalysisTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val analyzer = FakeAnalyzer()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() = PlayViewModel(SavedStateHandle(), FakeDocuments(), analyzer, FakeOpponent())

    private fun sq(name: String) = Square.parse(name)!!

    private fun PlayViewModel.settle() = dispatcher.scheduler.advanceUntilIdle()

    private val PlayViewModel.ui get() = state.value

    @Test
    fun `the analysis is off by default and the engine is not used`() {
        val vm = newViewModel()
        vm.settle()
        assertFalse(vm.analysisState.value.enabled)
        assertTrue(analyzer.requests.isEmpty())
    }

    @Test
    fun `switching it on analyses the position shown`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ToggleAnalysis)
        assertTrue(vm.analysisState.value.enabled)
        assertTrue(
            vm.analysisState.value.lines
                .isEmpty(),
        )
        vm.settle()
        assertEquals(Fen.START, analyzer.requests.single().fen)
        assertEquals(Fen.START, vm.analysisState.value.fen)
        assertEquals(
            "1.Cf3",
            vm.analysisState.value.best!!
                .text(),
        )
    }

    @Test
    fun `the analysis follows the moves and the navigation`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ToggleAnalysis)
        vm.settle()
        vm.onAction(PlayAction.SquareTap(sq("e2")))
        vm.onAction(PlayAction.SquareTap(sq("e4")))
        // Lines of the previous position are gone as soon as the position changes.
        assertTrue(
            vm.analysisState.value.lines
                .isEmpty(),
        )
        assertEquals(vm.ui.fen, vm.analysisState.value.fen)
        vm.settle()
        vm.onAction(PlayAction.Back)
        vm.settle()
        assertEquals(Fen.START, analyzer.requests.last().fen)
        assertEquals(Fen.START, vm.analysisState.value.fen)
    }

    @Test
    fun `switching it off clears the lines and stops the search`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ToggleAnalysis)
        vm.settle()
        vm.onAction(PlayAction.ToggleAnalysis)
        assertEquals(AnalysisState(), vm.analysisState.value)
        assertEquals(1, analyzer.cancelled)
    }

    @Test
    fun `tapping a line plays its first move`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ToggleAnalysis)
        vm.settle()
        vm.onAction(
            PlayAction.PlayAnalysisMove(
                vm.analysisState.value.best!!
                    .firstMove,
            ),
        )
        assertEquals(Piece(PieceType.KNIGHT, Side.WHITE), vm.ui.board[sq("f3")])
        assertEquals(Side.BLACK, vm.ui.sideToMove)
        assertEquals(Move.parseUci("g1f3"), vm.ui.highlights.lastMove)
    }

    @Test
    fun `a line computed for another position is ignored`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ToggleAnalysis)
        vm.settle()
        val stale =
            vm.analysisState.value.best!!
                .firstMove
        vm.onAction(PlayAction.SquareTap(sq("e2")))
        vm.onAction(PlayAction.SquareTap(sq("e4")))
        val after = vm.ui.fen
        vm.onAction(PlayAction.PlayAnalysisMove(stale))
        assertEquals(after, vm.ui.fen)
    }

    @Test
    fun `an illegal move cannot be played from a line`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ToggleAnalysis)
        vm.settle()
        vm.onAction(PlayAction.PlayAnalysisMove(Move.parseUci("e2e5")!!))
        assertEquals(Fen.START, vm.ui.fen)
    }

    @Test
    fun `a finished game is not analysed`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.ImportText("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"))
        vm.onAction(PlayAction.ToggleAnalysis)
        vm.settle()
        assertTrue(vm.analysisState.value.gameOver)
        assertTrue(analyzer.requests.isEmpty())
        assertNull(vm.analysisState.value.best)
    }
}

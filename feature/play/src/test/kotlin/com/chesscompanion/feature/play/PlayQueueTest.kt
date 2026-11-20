package com.chesscompanion.feature.play

import androidx.lifecycle.SavedStateHandle
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.data.AnalysisJob
import com.chesscompanion.core.data.AnalysisJobState
import com.chesscompanion.core.data.StoredAnalysis
import com.chesscompanion.core.engine.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** S2-5: the background analysis queue seen through the game ViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlayQueueTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val queue = FakeQueue()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() =
        PlayViewModel(SavedStateHandle(), FakeDocuments(), FakeAnalyzer(), FakeOpponent(), queue)

    private fun PlayViewModel.tap(vararg squares: String) =
        squares.forEach {
            onAction(PlayAction.SquareTap(Square.parse(it)!!))
        }

    @Test
    fun `analysing the game sends its pgn to the queue`() {
        val vm = newViewModel()
        vm.tap("e2", "e4", "e7", "e5")
        vm.onAction(PlayAction.AnalyseGame)
        assertEquals(listOf(vm.exportPgn()), queue.enqueued)
    }

    @Test
    fun `cancelling cancels the whole queue`() {
        val vm = newViewModel()
        vm.onAction(PlayAction.CancelGameAnalysis)
        assertEquals(1, queue.cancelled)
    }

    @Test
    fun `the screen follows the latest job`() {
        val vm = newViewModel()
        assertNull(vm.queueState.value)
        val job = AnalysisJob("job-1", "*", AnalysisJobState.RUNNING, done = 3, total = 10)
        queue.latest.value = job
        assertEquals(job, vm.queueState.value)
    }

    @Test
    fun `a stored analysis is shown as soon as the analysis is switched on`() {
        queue.stored[Fen.parse(Fen.START).zobristKey] =
            StoredAnalysis(Fen.START, 18, scoreCp = 31, mateIn = null, bestMove = "e2e4", pv = listOf("e2e4", "c7c5"))
        val vm = newViewModel()
        vm.onAction(PlayAction.ToggleAnalysis)
        val best = vm.analysisState.value.best!!
        assertEquals(18, best.depth)
        assertEquals(Score.Centipawns(31), best.score)
        assertEquals("1.e4 c5", best.text())
    }
}

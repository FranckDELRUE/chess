package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EngineInfo
import com.chesscompanion.core.engine.Score
import com.chesscompanion.core.engine.SearchLimit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisControllerTest {
    private val start = Fen.parse(Fen.START)
    private val afterE4 = start.play(Move.parseUci("e2e4")!!)
    private val afterD4 = start.play(Move.parseUci("d2d4")!!)

    private val scopes = mutableListOf<CoroutineScope>()

    /** On the test scheduler, as a plain child scope: `backgroundScope` is not run by `advanceUntilIdle`. */
    private fun TestScope.controller(analyzer: PositionAnalyzer): AnalysisController {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(dispatcher + Job())
        scopes += scope
        return AnalysisController(analyzer, scope, 150, dispatcher)
    }

    @AfterEach
    fun tearDown() = scopes.forEach { it.cancel() }

    /** A burst of 200 lines at once, then the end of the search: the screen gets few updates, the last one included. */
    @Test
    fun `a burst of results reaches the screen throttled, last one included`() =
        runTest {
            val burst =
                PositionAnalyzer {
                    flow {
                        repeat(200) { depth ->
                            emit(
                                EngineEvent.Info(
                                    EngineInfo(depth = depth + 1, score = Score.Centipawns(depth), pv = listOf("g1f3")),
                                ),
                            )
                        }
                        emit(EngineEvent.BestMove("g1f3"))
                    }
                }
            val controller = controller(burst)
            val seen = mutableListOf<AnalysisState>()
            val collector = launch(StandardTestDispatcher(testScheduler)) { controller.state.collect { seen += it } }
            controller.setEnabled(true)
            controller.show(start, over = false)
            advanceUntilIdle()
            collector.cancel()
            assertTrue(seen.size < 10, "expected a few updates, got ${seen.size}")
            assertEquals(200, controller.state.value.depth)
            assertTrue(controller.state.value.finished)
        }

    @Test
    fun `nothing is searched while disabled`() =
        runTest {
            val analyzer = FakeAnalyzer()
            val controller = controller(analyzer)
            controller.show(start, over = false)
            advanceUntilIdle()
            assertTrue(analyzer.requests.isEmpty())
            assertFalse(controller.state.value.enabled)
        }

    @Test
    fun `enabling searches three lines with the live limits`() =
        runTest {
            val analyzer = FakeAnalyzer()
            val controller = controller(analyzer)
            controller.show(start, over = false)
            controller.setEnabled(true)
            advanceUntilIdle()
            val request = analyzer.requests.single()
            assertEquals(Fen.START, request.fen)
            assertEquals(3, request.multiPv)
            assertEquals(SearchLimit.Depth(24), request.limit)
            assertEquals(Fen.START, controller.state.value.fen)
            assertEquals(
                listOf("Cf3"),
                controller.state.value.best!!
                    .moves,
            )
        }

    @Test
    fun `a new position cancels the search and starts another`() =
        runTest {
            val analyzer = FakeAnalyzer()
            val controller = controller(analyzer)
            controller.show(start, over = false)
            controller.setEnabled(true)
            advanceUntilIdle()
            controller.show(afterE4, over = false)
            // The lines of the previous position are dropped at once.
            assertTrue(
                controller.state.value.lines
                    .isEmpty(),
            )
            advanceUntilIdle()
            assertEquals(2, analyzer.requests.size)
            assertEquals(1, analyzer.cancelled)
            assertEquals(afterE4.toFen(), controller.state.value.fen)
        }

    @Test
    fun `fast navigation only reaches the engine once settled`() =
        runTest {
            val analyzer = FakeAnalyzer()
            val controller = controller(analyzer)
            controller.setEnabled(true)
            controller.show(start, over = false)
            advanceTimeBy(100)
            controller.show(afterE4, over = false)
            advanceTimeBy(100)
            controller.show(afterD4, over = false)
            advanceUntilIdle()
            assertEquals(listOf(afterD4.toFen()), analyzer.requests.map { it.fen })
        }

    @Test
    fun `showing the same position again does not restart the search`() =
        runTest {
            val analyzer = FakeAnalyzer()
            val controller = controller(analyzer)
            controller.setEnabled(true)
            controller.show(start, over = false)
            advanceUntilIdle()
            controller.show(start, over = false)
            advanceUntilIdle()
            assertEquals(1, analyzer.requests.size)
            assertEquals(0, analyzer.cancelled)
        }

    @Test
    fun `disabling stops the search and clears the lines`() =
        runTest {
            val analyzer = FakeAnalyzer()
            val controller = controller(analyzer)
            controller.setEnabled(true)
            controller.show(start, over = false)
            advanceUntilIdle()
            controller.setEnabled(false)
            runCurrent()
            assertEquals(1, analyzer.cancelled)
            assertEquals(AnalysisState(), controller.state.value)
        }

    @Test
    fun `a finished game is not searched`() =
        runTest {
            val analyzer = FakeAnalyzer()
            val controller = controller(analyzer)
            controller.setEnabled(true)
            controller.show(start, over = true)
            advanceUntilIdle()
            assertTrue(analyzer.requests.isEmpty())
            assertTrue(controller.state.value.gameOver)
        }

    @Test
    fun `an engine failure is reported and does not crash`() =
        runTest {
            val controller = controller(FakeAnalyzer(failWith = IllegalStateException("no engine")))
            controller.setEnabled(true)
            controller.show(start, over = false)
            advanceUntilIdle()
            assertTrue(controller.state.value.failed)
        }

    @Test
    fun `cancelling a search is not reported as a failure`() =
        runTest {
            val controller = controller(FakeAnalyzer())
            controller.setEnabled(true)
            controller.show(start, over = false)
            advanceUntilIdle()
            controller.show(afterE4, over = false)
            advanceUntilIdle()
            assertFalse(controller.state.value.failed)
        }
}

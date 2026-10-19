package com.chesscompanion.core.engine

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Real Stockfish on the device, through the scheduler (one engine per process, shared by the tests). */
@RunWith(AndroidJUnit4::class)
class StockfishSchedulerTest {
    @Test
    fun multiPvGivesThreeDistinctLinesAtEachDepth() =
        runBlocking {
            val request = SearchRequest(Fen.START, SearchLimit.Depth(DEPTH), multiPv = 3, newGame = true)
            val events = withTimeout(TIMEOUT_MS) { scheduler.search(EnginePriority.LIVE, request).toList() }
            val infos = events.filterIsInstance<EngineEvent.Info>().map { it.info }
            val last = infos.filter { it.depth == DEPTH }
            assertEquals(listOf(1, 2, 3), last.map { it.multiPv }.sorted())
            infos.groupBy { it.depth }.values.filter { it.size == 3 }.forEach { lines ->
                assertEquals(3, lines.map { it.pv.first() }.toSet().size)
            }
            assertTrue(events.last() is EngineEvent.BestMove)
        }

    @Test
    fun opponentPreemptsBackgroundSearch() =
        runBlocking {
            val collected = mutableListOf<EngineEvent>()
            val background =
                launch(Dispatchers.Default) {
                    val long = SearchRequest(Fen.START, SearchLimit.MoveTime(LONG_MS), newGame = true)
                    scheduler.search(EnginePriority.BACKGROUND, long).collect {
                        synchronized(collected) {
                            collected +=
                                it
                        }
                    }
                }
            withTimeout(TIMEOUT_MS) {
                while (snapshot(collected).none { it is EngineEvent.Info }) delay(POLL_MS)
            }

            val start = System.nanoTime()
            val opponent = SearchRequest(MIDDLEGAME, SearchLimit.MoveTime(MOVE_MS), strength = EngineStrength.Elo(1500))
            val reply = withTimeout(TIMEOUT_MS) { scheduler.search(EnginePriority.OPPONENT, opponent).toList() }
            val elapsedMs = (System.nanoTime() - start) / NS_PER_MS
            val best = (reply.last() as EngineEvent.BestMove).move
            assertTrue("illegal $best", Fen.parse(MIDDLEGAME).isLegal(Move.parseUci(best)!!))
            // The opponent did not wait for the 30 s background search.
            assertTrue("opponent took $elapsedMs ms", elapsedMs < MOVE_MS + MARGIN_MS)

            withTimeout(TIMEOUT_MS) {
                while (EngineEvent.Restarted !in snapshot(collected)) delay(POLL_MS)
            }
            assertTrue(snapshot(collected).none { it is EngineEvent.BestMove })
            background.cancel()
        }

    private fun snapshot(events: MutableList<EngineEvent>) = synchronized(events) { events.toList() }

    private companion object {
        const val DEPTH = 12
        const val LONG_MS = 30_000L
        const val MOVE_MS = 500L
        const val MARGIN_MS = 1_500L
        const val TIMEOUT_MS = 20_000L
        const val POLL_MS = 20L
        const val NS_PER_MS = 1_000_000L
        const val MIDDLEGAME = "r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2NBPN2/PP3PPP/R1BQK2R w KQ - 0 8"

        val scheduler get() = TestEngine.scheduler
    }
}

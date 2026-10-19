package com.chesscompanion.core.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UciEngineTest {
    private val fen = "8/8/8/8/8/8/8/K6k w - - 0 1"

    private fun options(transport: FakeUciTransport) = transport.sent.filter { it.startsWith("setoption") }

    @Test
    fun `start sends options between uci and isready`() =
        runTest {
            val transport = FakeUciTransport()
            UciEngine(transport).start(mapOf("Threads" to "4"))
            assertEquals(listOf("uci", "setoption name Threads value 4", "isready"), transport.sent)
        }

    @Test
    fun `analyze emits infos then bestmove`() =
        runTest {
            val transport = FakeUciTransport()
            val events = UciEngine(transport).analyze(fen, 2).toList()
            assertEquals(3, events.size)
            assertEquals(EngineEvent.BestMove("e2e4", "e7e5"), events.last())
            assertTrue(transport.sent.contains("position fen $fen"))
        }

    @Test
    fun `cancelling the collector stops the search and drains its bestmove`() =
        runTest {
            val transport = FakeUciTransport()
            val engine = UciEngine(transport)
            val request = SearchRequest(fen, SearchLimit.MoveTime(10_000))
            engine.analyze(request).first()
            assertEquals("stop", transport.sent.last())
            // The next search reads its own bestmove, not the one of the stopped search.
            assertEquals(EngineEvent.BestMove("e2e4", "e7e5"), engine.analyze(fen, 1).toList().last())
        }

    @Test
    fun `cancelling a search blocked on the engine sends stop at once`() =
        runTest {
            val transport = FakeUciTransport()
            val events = mutableListOf<EngineEvent>()
            val job =
                launch {
                    UciEngine(transport).analyze(SearchRequest(fen, SearchLimit.Nodes(1_000_000))).collect {
                        events +=
                            it
                    }
                }
            runCurrent()
            assertEquals(1, events.size)
            job.cancel()
            job.join()
            assertEquals(1, transport.sent.count { it == "stop" })
            assertEquals(1, events.size)
        }

    @Test
    fun `a search that ends normally sends no stop`() =
        runTest {
            val transport = FakeUciTransport()
            UciEngine(transport).analyze(fen, 3).toList()
            assertTrue("stop" !in transport.sent)
        }

    @Test
    fun `setOption and newGame wait for readyok`() =
        runTest {
            val transport = FakeUciTransport()
            val engine = UciEngine(transport)
            engine.setOption("Threads", "2")
            engine.newGame()
            assertEquals(
                listOf("setoption name Threads value 2", "isready", "ucinewgame", "isready"),
                transport.sent,
            )
        }

    @Test
    fun `default options are sent before caller options`() =
        runTest {
            val transport = FakeUciTransport()
            UciEngine(transport, mapOf("EvalFile" to "/net.nnue")).start(mapOf("Threads" to "4"))
            assertEquals(
                listOf("uci", "setoption name EvalFile value /net.nnue", "setoption name Threads value 4", "isready"),
                transport.sent,
            )
        }

    @Test
    fun `request is sent as position with moves and go`() =
        runTest {
            val transport = FakeUciTransport()
            val request =
                SearchRequest(
                    fen,
                    SearchLimit.Depth(2),
                    moves = listOf("a1a2", "h1h2"),
                    searchMoves = listOf("a2a3", "a2b1"),
                )
            UciEngine(transport).analyze(request).toList()
            assertTrue(transport.sent.contains("position fen $fen moves a1a2 h1h2"))
            assertTrue(transport.sent.contains("go depth 2 searchmoves a2a3 a2b1"))
        }

    @Test
    fun `go command follows the limit`() {
        assertEquals("go movetime 1500", UciEngine.goCommand(SearchRequest(fen, SearchLimit.MoveTime(1500))))
        assertEquals("go nodes 400000", UciEngine.goCommand(SearchRequest(fen, SearchLimit.Nodes(400_000))))
    }

    @Test
    fun `newGame request sends ucinewgame before the position`() =
        runTest {
            val transport = FakeUciTransport()
            val engine = UciEngine(transport)
            engine.analyze(SearchRequest(fen, SearchLimit.Depth(1), newGame = true)).toList()
            val newGame = transport.sent.indexOf("ucinewgame")
            assertTrue(newGame >= 0 && newGame < transport.sent.indexOf("position fen $fen"))
            assertEquals("isready", transport.sent[newGame + 1])
        }

    @Test
    fun `search options are sent only when they change`() =
        runTest {
            val transport = FakeUciTransport()
            val engine = UciEngine(transport)
            engine.analyze(SearchRequest(fen, SearchLimit.Depth(1), multiPv = 3)).toList()
            assertEquals(
                listOf(
                    "setoption name MultiPV value 3",
                    "setoption name Skill Level value 20",
                    "setoption name UCI_LimitStrength value false",
                ),
                options(transport),
            )
            engine.analyze(SearchRequest(fen, SearchLimit.Depth(1), multiPv = 3)).toList()
            assertEquals(3, options(transport).size)
            assertEquals(1, transport.sent.count { it == "isready" })
        }

    @Test
    fun `strength maps to skill level or elo limit`() {
        val elo = UciEngine.searchOptions(SearchRequest(fen, SearchLimit.Depth(1), strength = EngineStrength.Elo(1500)))
        assertEquals(
            mapOf("MultiPV" to "1", "Skill Level" to "20", "UCI_LimitStrength" to "true", "UCI_Elo" to "1500"),
            elo,
        )
        val skill =
            UciEngine.searchOptions(
                SearchRequest(fen, SearchLimit.Depth(1), strength = EngineStrength.Skill(3)),
            )
        assertEquals(mapOf("MultiPV" to "1", "Skill Level" to "3", "UCI_LimitStrength" to "false"), skill)
    }

    @Test
    fun `full strength after a limited search restores the options`() =
        runTest {
            val transport = FakeUciTransport()
            val engine = UciEngine(transport)
            engine.analyze(SearchRequest(fen, SearchLimit.Depth(1), strength = EngineStrength.Elo(1400))).toList()
            transport.sent.clear()
            engine.analyze(SearchRequest(fen, SearchLimit.Depth(1))).toList()
            assertEquals(listOf("setoption name UCI_LimitStrength value false"), options(transport))
        }
}

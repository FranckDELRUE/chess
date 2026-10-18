package com.chesscompanion.core.engine

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UciEngineTest {
    /** Faux moteur : répond aux commandes avec des lignes préparées. */
    private class FakeTransport : UciTransport {
        val sent = mutableListOf<String>()
        private val pending = ArrayDeque<String>()

        override suspend fun send(line: String) {
            sent += line
            when {
                line == "uci" -> pending += listOf("id name Fake", "uciok")
                line == "isready" -> pending += "readyok"
                line.startsWith("go depth") ->
                    pending +=
                        listOf(
                            "info depth 1 score cp 10 nodes 20 nps 1000 time 1 pv e2e4",
                            "info depth 2 score cp 12 nodes 90 nps 1000 time 2 pv e2e4 e7e5",
                            "bestmove e2e4 ponder e7e5",
                        )
                line == "stop" -> pending += "bestmove e2e4"
                line == "go perft 1" ->
                    pending +=
                        listOf("info string NNUE", "e2e4: 1", "e7e8q: 1", "", "Nodes searched: 2")
                line == "quit" -> Unit
            }
        }

        override suspend fun readLine(): String? = pending.removeFirstOrNull()
    }

    @Test
    fun `start sends options between uci and isready`() =
        runTest {
            val transport = FakeTransport()
            UciEngine(transport).start(mapOf("Threads" to "4"))
            assertEquals(listOf("uci", "setoption name Threads value 4", "isready"), transport.sent)
        }

    @Test
    fun `analyze emits infos then bestmove`() =
        runTest {
            val transport = FakeTransport()
            val events = UciEngine(transport).analyze("8/8/8/8/8/8/8/K6k w - - 0 1", 2).toList()
            assertEquals(3, events.size)
            assertEquals(EngineEvent.BestMove("e2e4", "e7e5"), events.last())
            assertTrue(transport.sent.contains("position fen 8/8/8/8/8/8/8/K6k w - - 0 1"))
        }

    @Test
    fun `cancelling the collector stops the search`() =
        runTest {
            val transport = FakeTransport()
            UciEngine(transport).analyze("8/8/8/8/8/8/8/K6k w - - 0 1", 20).first()
            assertEquals("stop", transport.sent.last())
        }

    @Test
    fun `setOption and newGame wait for readyok`() =
        runTest {
            val transport = FakeTransport()
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
            val transport = FakeTransport()
            UciEngine(transport, mapOf("EvalFile" to "/net.nnue")).start(mapOf("Threads" to "4"))
            assertEquals(
                listOf("uci", "setoption name EvalFile value /net.nnue", "setoption name Threads value 4", "isready"),
                transport.sent,
            )
        }

    @Test
    fun `legalMoves reads perft 1 output`() =
        runTest {
            val transport = FakeTransport()
            val moves = UciEngine(transport).legalMoves("4k3/4P3/8/8/8/8/8/4K3 w - - 0 1")
            assertEquals(setOf("e2e4", "e7e8q"), moves)
            assertEquals(listOf("position fen 4k3/4P3/8/8/8/8/8/4K3 w - - 0 1", "go perft 1"), transport.sent)
        }

    @Test
    fun `searchMoves restricts the search`() =
        runTest {
            val transport = FakeTransport()
            UciEngine(transport).analyze("8/8/8/8/8/8/8/K6k w - - 0 1", 2, listOf("a1a2", "a1b1")).toList()
            assertTrue(transport.sent.contains("go depth 2 searchmoves a1a2 a1b1"))
        }
}

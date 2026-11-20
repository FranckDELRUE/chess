package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.data.AnalysisJobState
import com.chesscompanion.core.data.StoredAnalysis
import com.chesscompanion.core.engine.EngineEvent
import com.chesscompanion.core.engine.EngineInfo
import com.chesscompanion.core.engine.Score
import com.chesscompanion.core.engine.SearchLimit
import com.chesscompanion.core.engine.SearchRequest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameAnalyzerTest {
    private val store = FakeStore()
    private val requests = mutableListOf<SearchRequest>()

    /** Answers each search with [events]; by default the first legal move at depth 18, +0.2 for the side to move. */
    private fun engine(events: (Position) -> List<EngineEvent> = ::firstLegal) =
        BackgroundEngine { request ->
            requests += request
            flowOf(*events(positionOf(request)).toTypedArray())
        }

    private fun positionOf(request: SearchRequest): Position =
        request.moves.fold(Fen.parse(request.fen)) { p, uci -> p.play(Move.parseUci(uci)!!) }

    private fun firstLegal(position: Position): List<EngineEvent> {
        val move =
            position.legalMoves
                .map { it.uci }
                .sorted()
                .first()
        return listOf(info(18, Score.Centipawns(20), move), EngineEvent.BestMove(move))
    }

    private fun info(
        depth: Int,
        score: Score,
        vararg pv: String,
    ) = EngineEvent.Info(EngineInfo(depth = depth, score = score, pv = pv.toList()))

    private suspend fun job(
        pgn: String,
        done: Int = 0,
    ): String {
        val id = store.newJob(pgn, total = 0)
        store.updateJob(id, AnalysisJobState.PENDING, done)
        return id
    }

    private fun stored(position: Position) = store.positions[position.zobristKey]

    private val start = Fen.parse(Fen.START)
    private val afterE4 = start.play(Move.parseUci("e2e4")!!)

    @Test
    fun `every position of the main line is analysed and stored`() =
        runTest {
            val id = job("1. e4 e5 2. Nf3 *")
            val progress = mutableListOf<Pair<Int, Int>>()
            val state = GameAnalyzer(engine(), store).run(id) { done, total -> progress += done to total }
            assertEquals(AnalysisJobState.DONE, state)
            assertEquals(
                listOf(
                    emptyList(),
                    listOf("e2e4"),
                    listOf("e2e4", "e7e5"),
                    listOf("e2e4", "e7e5", "g1f3"),
                ),
                requests.map {
                    it.moves
                },
            )
            assertTrue(requests.all { it.fen == Fen.START && it.limit == SearchLimit.Depth(18) && it.multiPv == 1 })
            assertEquals(listOf(1 to 4, 2 to 4, 3 to 4, 4 to 4), progress)
            assertEquals(4, store.positions.size)
            assertEquals(AnalysisJobState.DONE, store.job(id)!!.state)
            assertEquals(4, store.job(id)!!.done)
        }

    @Test
    fun `a position where the game is over is not searched`() =
        runTest {
            val id = job("1. f3 e5 2. g4 Qh4# 0-1")
            GameAnalyzer(engine(), store).run(id)
            assertEquals(4, requests.size)
            assertEquals(AnalysisJobState.DONE, store.job(id)!!.state)
        }

    @Test
    fun `a position already analysed as deep is skipped, a shallower one is searched again`() =
        runTest {
            store.savePosition(start.zobristKey, StoredAnalysis(Fen.START, 20, 15, null, "e2e4", listOf("e2e4")))
            store.savePosition(
                afterE4.zobristKey,
                StoredAnalysis(afterE4.toFen(), 12, 15, null, "e7e5", listOf("e7e5")),
            )
            GameAnalyzer(engine(), store).run(job("1. e4 *"))
            assertEquals(listOf(listOf("e2e4")), requests.map { it.moves })
            assertEquals(20, stored(start)!!.depth)
            assertEquals(18, stored(afterE4)!!.depth)
        }

    @Test
    fun `an analysis resumes where it stopped`() =
        runTest {
            val id = job("1. e4 e5 2. Nf3 *", done = 2)
            GameAnalyzer(engine(), store).run(id)
            assertEquals(listOf(listOf("e2e4", "e7e5"), listOf("e2e4", "e7e5", "g1f3")), requests.map { it.moves })
            assertEquals(4, store.job(id)!!.done)
        }

    @Test
    fun `scores are stored from white's point of view`() =
        runTest {
            val events = { position: Position ->
                listOf(info(18, Score.Centipawns(40), "e7e5"), EngineEvent.BestMove("e7e5")).takeIf {
                    position ==
                        afterE4
                }
                    ?: firstLegal(position)
            }
            GameAnalyzer(engine(events), store).run(job("1. e4 *"))
            assertEquals(-40, stored(afterE4)!!.scoreCp)
            assertNull(stored(afterE4)!!.mateIn)
            assertEquals(20, stored(start)!!.scoreCp)
        }

    @Test
    fun `a mate score is stored as a mate seen from white`() =
        runTest {
            GameAnalyzer(engine { listOf(info(18, Score.Mate(3), "e7e5"), EngineEvent.BestMove("e7e5")) }, store)
                .run(job("1. e4 *"))
            assertEquals(-3, stored(afterE4)!!.mateIn)
        }

    @Test
    fun `an illegal best move is not stored, an illegal line is cut`() =
        runTest {
            val events = { position: Position ->
                if (position == start) {
                    listOf(info(18, Score.Centipawns(0), "e2e5"), EngineEvent.BestMove("e2e5"))
                } else {
                    listOf(info(18, Score.Centipawns(0), "e7e5", "g1f3", "g1f3"), EngineEvent.BestMove("e7e5"))
                }
            }
            GameAnalyzer(engine(events), store).run(job("1. e4 *"))
            assertNull(stored(start))
            assertEquals(listOf("e7e5", "g1f3"), stored(afterE4)!!.pv)
        }

    @Test
    fun `after a restart the deepest result is kept`() =
        runTest {
            val events = { _: Position ->
                listOf(
                    info(18, Score.Centipawns(30), "e2e4"),
                    EngineEvent.Restarted,
                    info(5, Score.Centipawns(-100), "a2a3"),
                    EngineEvent.BestMove("e2e4"),
                )
            }
            GameAnalyzer(engine(events), store).run(job("*"))
            assertEquals(18, stored(start)!!.depth)
            assertEquals(30, stored(start)!!.scoreCp)
        }

    @Test
    fun `a cancelled job is left alone and a broken game fails`() =
        runTest {
            val cancelled = job("1. e4 *")
            store.updateJob(cancelled, AnalysisJobState.CANCELLED, 0)
            assertEquals(AnalysisJobState.CANCELLED, GameAnalyzer(engine(), store).run(cancelled))
            assertTrue(requests.isEmpty())
            val broken = job("1. e5 *")
            assertEquals(AnalysisJobState.FAILED, GameAnalyzer(engine(), store).run(broken))
            assertEquals(AnalysisJobState.FAILED, store.job(broken)!!.state)
            assertEquals(AnalysisJobState.FAILED, GameAnalyzer(engine(), store).run("missing"))
        }
}

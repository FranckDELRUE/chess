package com.chesscompanion.core.data.explorer

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.repertoire.Repertoire
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** S8b-1: a gap is added together with the rest of the line, from the most played moves of both sides. */
class LineCompleterTest {
    /** Answers the k-th question with the k-th reply (`uci to games`); fails when it runs out. */
    private class ScriptedExplorer(
        private val replies: List<List<Pair<String, Long>>>,
    ) : ExplorerLookup {
        var asked = 0
            private set

        override suspend fun lookup(
            fen: String,
            level: ExplorerLevel,
        ): ExplorerOutcome {
            val reply = replies.getOrNull(asked++) ?: return ExplorerOutcome.Failed(ExplorerProblem.OFFLINE)
            val moves = reply.map { ExplorerMove(it.first, it.second, 0, 0, null) }
            return ExplorerOutcome.Loaded(ExplorerStats(moves.sumOf { it.games }, 0, 0, moves), fromCache = true)
        }
    }

    // White's repertoire answers 1. d4 d5 only; Black's 1...Nf6 is the gap.
    private val repertoire: Repertoire =
        PgnReader
            .readAll("[Event \"t\"]\n\n1. d4 d5 *")
            .games
            .fold(Repertoire.empty(Side.WHITE)) { r, g -> r.merge(g).repertoire }
    private val afterD4 = repertoire.movesFrom(repertoire.root).single().to
    private val knightF6 = checkNotNull(Move.parseUci("g8f6"))
    private val settings = GapSettings(pauseMs = 0, completionPlies = 3)

    private fun reply(vararg moves: Pair<String, Long>) = moves.toList()

    @Test
    fun theLineFollowsTheMostPlayedMovesOfBothSidesAndEndsOnThePlayersMove() =
        runTest {
            val explorer =
                ScriptedExplorer(
                    listOf(
                        reply("c2c4" to 900, "g1f3" to 500), // White's answer to 1...Nf6
                        reply("e7e6" to 800), // Black
                        reply("b1c3" to 700), // White again: 4 plies, Black to move, the limit is reached
                    ),
                )
            val done = LineCompleter(explorer).complete(repertoire, afterD4, knightF6, settings)

            assertEquals(4, done.addedMoves)
            assertNull(done.problem)
            assertEquals(repertoire.edgeCount + 4, done.repertoire.edgeCount)
            assertEquals(3, explorer.asked)
        }

    @Test
    fun withoutTheExplorerOnlyTheGapMoveIsAddedAndTheProblemIsReported() =
        runTest {
            val done = LineCompleter(ScriptedExplorer(emptyList())).complete(repertoire, afterD4, knightF6, settings)
            assertEquals(1, done.addedMoves)
            assertEquals(ExplorerProblem.OFFLINE, done.problem)
            assertEquals(repertoire.edgeCount + 1, done.repertoire.edgeCount)
        }

    @Test
    fun aPositionTheExplorerKnowsTooLittleEndsTheLine() =
        runTest {
            val few = ScriptedExplorer(listOf(reply("c2c4" to 10)))
            val done = LineCompleter(few).complete(repertoire, afterD4, knightF6, settings)
            assertEquals(1, done.addedMoves)
            assertNull(done.problem)
        }

    @Test
    fun anIllegalMoveFromTheExplorerIsSkippedForTheNextLegalOne() =
        runTest {
            // e7e5 cannot be played by White: the rules engine rejects it and the next most played move is used.
            val explorer = ScriptedExplorer(listOf(reply("e7e5" to 900, "c2c4" to 500), reply("a7a6" to 1)))
            val done = LineCompleter(explorer).complete(repertoire, afterD4, knightF6, settings)
            assertEquals(2, done.addedMoves)
            val answer =
                done.repertoire
                    .movesFrom(
                        done.repertoire
                            .movesFrom(afterD4)
                            .last()
                            .to,
                    ).single()
            assertEquals("c2c4", answer.move.uci)
        }
}

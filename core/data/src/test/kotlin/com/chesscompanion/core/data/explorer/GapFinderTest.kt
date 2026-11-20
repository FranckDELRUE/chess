package com.chesscompanion.core.data.explorer

import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.chess.repertoire.Repertoire
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** S8-6: the frequent replies that a repertoire does not answer. */
class GapFinderTest {
    /** Explorer with a prepared answer per position (placement + side, the first two FEN fields). */
    private class FakeExplorer(
        private val answers: Map<String, ExplorerOutcome> = emptyMap(),
        private val default: ExplorerOutcome = ExplorerOutcome.Failed(ExplorerProblem.OFFLINE),
    ) : ExplorerLookup {
        val asked = mutableListOf<String>()

        override suspend fun lookup(
            fen: String,
            level: ExplorerLevel,
        ): ExplorerOutcome {
            asked += fen
            return answers[fen.split(' ').take(2).joinToString(" ")] ?: default
        }
    }

    private fun repertoire(
        side: Side,
        vararg games: String,
    ): Repertoire {
        val pgn = games.joinToString("\n\n") { "[Event \"t\"]\n\n$it" }
        return PgnReader.readAll(pgn).games.fold(Repertoire.empty(side)) { r, g -> r.merge(g).repertoire }
    }

    private fun stats(vararg moves: Pair<String, Long>) =
        ExplorerOutcome.Loaded(
            ExplorerStats(
                white = moves.sumOf { it.second },
                draws = 0,
                black = 0,
                moves = moves.map { ExplorerMove(it.first, it.second, 0, 0, null) },
            ),
            fromCache = true,
        )

    private val settings = GapSettings(pauseMs = 0)

    /** A position about which the explorer knows almost nothing: no gap there. */
    private val quiet = stats("a7a6" to 1)

    // After 1. d4 (White's repertoire move) Black replies; the repertoire only answers 1...d5.
    private val afterD4 = "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b"

    @Test
    fun aFrequentReplyWithoutAnswerIsAGap() =
        runTest {
            val white = repertoire(Side.WHITE, "1. d4 d5 2. c4 *")
            val explorer = FakeExplorer(mapOf(afterD4 to stats("d7d5" to 6_000, "g8f6" to 3_900, "e7e6" to 100)), quiet)
            val report = GapFinder(explorer).find(white, settings)

            assertNull(report.problem)
            val gap = report.gaps.single()
            assertEquals("g8f6", gap.move.uci)
            assertEquals(3_900L, gap.games)
            assertEquals(39, gap.percent)
            assertEquals(listOf("d2d4"), gap.path.map { it.move.uci })
        }

    @Test
    fun anAnsweredOrRareReplyIsNotAGap() =
        runTest {
            val white = repertoire(Side.WHITE, "1. d4 d5 2. c4 *")
            // d5 is answered; e6 is 1 % of the games; a6 has too few games.
            val explorer = FakeExplorer(mapOf(afterD4 to stats("d7d5" to 9_800, "e7e6" to 100, "a7a6" to 50)), quiet)
            assertTrue(GapFinder(explorer).find(white, settings).gaps.isEmpty())
        }

    @Test
    fun aReplyThatIsNotLegalIsIgnored() =
        runTest {
            val white = repertoire(Side.WHITE, "1. d4 d5 2. c4 *")
            val explorer =
                FakeExplorer(mapOf(afterD4 to stats("d7d5" to 5_000, "e2e4" to 5_000, "zzzz" to 5_000)), quiet)
            assertTrue(GapFinder(explorer).find(white, settings).gaps.isEmpty())
        }

    @Test
    fun aPositionWithTooFewGamesIsSkipped() =
        runTest {
            val white = repertoire(Side.WHITE, "1. d4 d5 *")
            val explorer = FakeExplorer(mapOf(afterD4 to stats("g8f6" to 60, "d7d5" to 30)))
            assertTrue(GapFinder(explorer).find(white, settings).gaps.isEmpty())
        }

    @Test
    fun theBlackRepertoireIsScannedFromTheRoot() =
        runTest {
            val black = repertoire(Side.BLACK, "1. d4 d5 *")
            val root = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w"
            val explorer = FakeExplorer(mapOf(root to stats("d2d4" to 5_000, "e2e4" to 5_000)))
            val report = GapFinder(explorer).find(black, settings)
            assertEquals(listOf("e2e4"), report.gaps.map { it.move.uci })
            assertTrue(
                report.gaps
                    .single()
                    .path
                    .isEmpty(),
            )
        }

    @Test
    fun gapsComeMostFrequentFirst() =
        runTest {
            val white = repertoire(Side.WHITE, "1. d4 d5 2. c4 e6 *")
            val afterC4 = "rnbqkbnr/ppp1pppp/8/3p4/2PP4/8/PP2PPPP/RNBQKBNR b"
            val explorer =
                FakeExplorer(
                    mapOf(
                        afterD4 to stats("d7d5" to 5_000, "g8f6" to 1_000),
                        afterC4 to stats("e7e6" to 5_000, "c7c6" to 3_000),
                    ),
                )
            val gaps = GapFinder(explorer).find(white, settings).gaps
            assertEquals(listOf("c7c6", "g8f6"), gaps.map { it.move.uci })
        }

    @Test
    fun theScanStopsAtTheFirstProblemAndKeepsWhatItFound() =
        runTest {
            val white = repertoire(Side.WHITE, "1. d4 d5 2. c4 e6 *")
            val explorer =
                FakeExplorer(
                    answers = mapOf(afterD4 to stats("d7d5" to 5_000, "g8f6" to 5_000)),
                    default = ExplorerOutcome.Failed(ExplorerProblem.RATE_LIMITED),
                )
            val report = GapFinder(explorer).find(white, settings)
            assertEquals(ExplorerProblem.RATE_LIMITED, report.problem)
            assertEquals(1, report.scanned)
            assertEquals(listOf("g8f6"), report.gaps.map { it.move.uci })
            assertFalse(report.truncated)
        }

    @Test
    fun theScanIsLimitedAndSaysSo() =
        runTest {
            val white = repertoire(Side.WHITE, "1. d4 d5 2. c4 e6 3. Nc3 *")
            val explorer = FakeExplorer(default = stats("d7d5" to 5_000))
            val report = GapFinder(explorer).find(white, settings.copy(maxPositions = 1))
            assertEquals(1, report.scanned)
            assertEquals(3, report.available)
            assertTrue(report.truncated)
            assertEquals(1, explorer.asked.size)
        }

    @Test
    fun progressIsReported() =
        runTest {
            val white = repertoire(Side.WHITE, "1. d4 d5 2. c4 *")
            val steps = mutableListOf<Pair<Int, Int>>()
            GapFinder(FakeExplorer(default = stats("d7d5" to 5_000))).find(white, settings) { done, total ->
                steps += done to total
            }
            assertEquals(listOf(1 to 2, 2 to 2), steps)
        }

    @Test
    fun anEmptyWhiteRepertoireHasNothingToScan() =
        runTest {
            val report = GapFinder(FakeExplorer()).find(Repertoire.empty(Side.WHITE), settings)
            assertEquals(0, report.available)
            assertTrue(report.gaps.isEmpty())
            assertNull(report.problem)
        }
}

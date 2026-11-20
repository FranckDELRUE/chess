package com.chesscompanion.enginedebug

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.pgn.PgnReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * S2-2 benchmark positions: the candidates file is stage 1 applied to the Heidoji corpus, and the
 * middlegames file keeps 20 of them, in order (stage 2, Stockfish on the PC). No hand edits.
 * Regenerate the candidates with `WRITE_HEIDOJI_CANDIDATES=1 gradlew :app:testDebugUnitTest`.
 */
class HeidojiPositionsTest {
    private val corpus = File("../testdata/pgn/chesscom-heidoji-100.pgn")
    private val candidatesFile = File("../testdata/positions/heidoji-candidates.txt")
    private val middlegamesFile = File("../testdata/positions/heidoji-middlegames.txt")

    private fun entries(file: File) = file.readLines().filter { it.isNotBlank() && !it.startsWith("#") }

    private fun fen(entry: String) = entry.substringBefore(" ;")

    @Test
    fun `candidates file is stage 1 applied to the corpus`() {
        val import = PgnReader.readAll(corpus.readText())
        assertTrue(import.errors.isEmpty())
        val lines = HeidojiSelection.candidates(import.games).map { it.line() }
        if (System.getenv("WRITE_HEIDOJI_CANDIDATES") == "1") {
            candidatesFile.parentFile.mkdirs()
            candidatesFile.writeText((listOf(CANDIDATES_HEADER) + lines).joinToString("\n", postfix = "\n"))
        }
        assertEquals(lines, entries(candidatesFile))
    }

    @Test
    fun `middlegames are twenty candidates kept in order, one per game`() {
        val candidates = entries(candidatesFile)
        val kept = entries(middlegamesFile)
        assertEquals(20, kept.size)
        val indices = kept.map { candidates.indexOf(it) }
        assertFalse(indices.contains(-1), "every middlegame must be a candidate")
        assertEquals(indices.sorted(), indices)
        val games = kept.map { it.substringAfter("; game ").substringBefore(" ply") }
        assertEquals(games.distinct(), games, "at most one position per game")
        kept.forEach { Fen.parse(fen(it)) }
    }

    @Test
    fun `benchmark positions are the middlegames`() {
        assertEquals(entries(middlegamesFile).map { fen(it) }, BenchmarkPositions.FENS)
    }

    @Test
    fun `stage 1 rejects checks, material gaps and endgames`() {
        assertTrue(
            HeidojiSelection.isMiddlegame(
                Fen.parse("r1bq1rk1/pp2bppp/2n1pn2/3p4/2PP4/2NBPN2/PP3PPP/R1BQK2R w KQ - 0 8"),
            ),
        )
        // White is a knight up.
        assertFalse(
            HeidojiSelection.isMiddlegame(Fen.parse("r1bq1rk1/pp2bppp/4pn2/3p4/2PP4/2NBPN2/PP3PPP/R1BQK2R w KQ - 0 8")),
        )
        // Black in check.
        assertFalse(
            HeidojiSelection.isMiddlegame(Fen.parse("rnbqkbnr/ppp2ppp/8/1B1pp3/4P3/8/PPPP1PPP/RNBQK1NR b KQkq - 1 3")),
        )
        // Rook endgame: one rook each, no queen.
        assertFalse(HeidojiSelection.isMiddlegame(Fen.parse("6k1/5ppp/8/8/8/8/r4PPP/R5K1 w - - 0 1")))
    }

    private companion object {
        const val CANDIDATES_HEADER =
            "# S2-2 stage 1 (HeidojiSelection): the positions of plies 30..40 of each game of " +
                "chesscom-heidoji-100.pgn not in check, material equal within a pawn, " +
                "a queen or two rooks each side. Generated, do not edit."
    }
}

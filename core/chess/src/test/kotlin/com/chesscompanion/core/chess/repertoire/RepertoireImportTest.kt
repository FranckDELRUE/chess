package com.chesscompanion.core.chess.repertoire

import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.toSan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class RepertoireImportTest {
    private fun import(
        pgn: String,
        into: Repertoire = Repertoire.empty(Side.WHITE),
    ) = into.importPgn(pgn)

    private fun game(moves: String) = "[Event \"x\"]\n\n$moves *\n\n"

    @Test
    fun `games are merged and counted`() {
        val report = import(game("1. e4 e5 2. Nf3") + game("1. e4 c5 2. Nf3"))
        assertEquals(2, report.gamesInFile)
        assertEquals(2, report.gamesMerged)
        assertEquals(5, report.addedMoves)
        assertEquals(1, report.existingMoves)
        assertTrue(report.rejected.isEmpty())
        assertTrue(report.changed)
        assertEquals(5, report.repertoire.edgeCount)
    }

    @Test
    fun `importing the same file again adds nothing`() {
        val pgn = game("1. e4 e5 (1... c5 2. Nf3) 2. Nf3")
        val once = import(pgn)
        val twice = import(pgn, once.repertoire)
        assertEquals(0, twice.addedMoves)
        assertFalse(twice.changed)
        assertEquals(once.repertoire.edgeCount, twice.repertoire.edgeCount)
    }

    @Test
    fun `an illegal move rejects its game with the line, the others are merged`() {
        val report = import(game("1. e4 e5") + game("1. e4 e6 2. Ke4") + game("1. d4 d5"))
        assertEquals(3, report.gamesInFile)
        assertEquals(2, report.gamesMerged)
        val rejection = report.rejected.single()
        assertEquals(2, rejection.gameNumber)
        assertEquals(RejectionReason.UNREADABLE, rejection.reason)
        assertTrue((rejection.line ?: 0) > 0)
        assertTrue(rejection.detail.isNotEmpty())
        assertEquals(4, report.repertoire.edgeCount)
    }

    @Test
    fun `a game from another position is rejected as such`() {
        val other = "[Event \"x\"]\n[SetUp \"1\"]\n[FEN \"4k3/8/8/8/8/8/4P3/4K3 w - - 0 1\"]\n\n1. e4 *\n\n"
        val report = import(game("1. e4") + other + game("1. d4"))
        val rejection = report.rejected.single()
        assertEquals(2, rejection.gameNumber)
        assertEquals(RejectionReason.DIFFERENT_START, rejection.reason)
        assertEquals(2, report.gamesMerged)
        assertEquals(2, report.repertoire.edgeCount)
    }

    @Test
    fun `rejections keep the file order whatever their kind`() {
        val other = "[Event \"x\"]\n[SetUp \"1\"]\n[FEN \"4k3/8/8/8/8/8/4P3/4K3 w - - 0 1\"]\n\n1. e4 *\n\n"
        val report = import(other + game("1. e4 Ke4") + game("1. e4"))
        assertEquals(listOf(1, 2), report.rejected.map { it.gameNumber })
        assertEquals(1, report.gamesMerged)
    }

    @Test
    fun `a bare move text without tags is a game`() {
        val report = import("1. e4 e5 2. Nf3 Nc6 *")
        assertEquals(1, report.gamesMerged)
        assertEquals(4, report.addedMoves)
    }

    @Test
    fun `an empty text is a clean empty report`() {
        val report = import("")
        assertEquals(0, report.gamesInFile)
        assertFalse(report.changed)
    }

    @Test
    fun `comments and marks come with the import`() {
        val report = import(game("1. e4 {Coup du roi [%cal Ge2e4]} e5"))
        val repertoire = report.repertoire
        val node = repertoire.movesFrom(repertoire.root).single().to
        assertEquals("Coup du roi", repertoire.notes(node).comment)
        assertEquals(1, report.notedPositions)
    }

    @Test
    fun `progress reaches the number of games`() {
        val seen = mutableListOf<Pair<Int, Int>>()
        Repertoire.empty(Side.WHITE).importPgn(game("1. e4") + game("1. e4 Ke4") + game("1. d4")) { done, total ->
            seen += done to total
        }
        assertEquals(3 to 3, seen.last())
        assertTrue(seen.zipWithNext().all { (a, b) -> a.first <= b.first })
    }

    /** Random games from the start position, as PGN text, and the same lines played one move at a time. */
    private fun randomLines(
        count: Int,
        depth: Int,
    ): Pair<String, Repertoire> {
        val random = Random(7)
        var expected = Repertoire.empty(Side.WHITE)
        val pgn = StringBuilder()
        repeat(count) {
            var position = Position.START
            var id = expected.root
            val san = mutableListOf<String>()
            repeat(depth) {
                val moves = position.legalMoves
                if (moves.isEmpty()) return@repeat
                val move = moves[random.nextInt(moves.size)]
                san += position.toSan(move)
                expected = expected.add(id, move).also { id = it.target }.repertoire
                position = position.play(move)
            }
            pgn.append(
                game(
                    san.chunked(2).withIndex().joinToString(" ") { (i, pair) -> "${i + 1}. ${pair.joinToString(" ")}" },
                ),
            )
        }
        return pgn.toString() to expected
    }

    @Test
    fun `500 lines are imported without loss, transpositions share their node`() {
        val (pgn, expected) = randomLines(count = 500, depth = 12)
        val report = import(pgn)
        assertTrue(report.rejected.isEmpty())
        assertEquals(500, report.gamesMerged)
        val result = report.repertoire
        assertEquals(expected.edgeCount, result.edgeCount)
        assertEquals(expected.nodeCount, result.nodeCount)
        assertEquals(
            expected.allMoves.map { Triple(it.from, it.to, it.move) }.toSet(),
            result.allMoves.map { Triple(it.from, it.to, it.move) }.toSet(),
        )
        assertTrue(result.edgeCount >= result.nodeCount - 1)
    }

    @Test
    fun `two move orders reach the same position once`() {
        val report = import(game("1. e4 e5 2. Nf3 Nc6") + game("1. Nf3 Nc6 2. e4 e5"))
        assertEquals(8, report.addedMoves)
        assertEquals(7, report.addedPositions)
        assertEquals(8, report.repertoire.nodeCount)
    }
}

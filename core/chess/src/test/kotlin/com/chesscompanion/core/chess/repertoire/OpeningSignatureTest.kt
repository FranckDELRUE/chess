package com.chesscompanion.core.chess.repertoire

import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.PgnReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** S8b-5: the starting moves that make an opening what it is. */
class OpeningSignatureTest {
    private fun repertoire(
        side: Side,
        vararg games: String,
    ): Repertoire {
        val pgn = games.joinToString("\n\n") { "[Event \"t\"]\n\n$it" }
        return PgnReader.readAll(pgn).games.fold(Repertoire.empty(side)) { r, g -> r.merge(g).repertoire }
    }

    private val scotch =
        repertoire(
            Side.WHITE,
            "1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Nxd4 *",
            "1. e4 e5 2. Nf3 Nc6 3. d4 d6 4. dxe5 *",
        )

    @Test
    fun theSignatureIsTheMovesBeforeTheFirstBranch() {
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6", "d4"), scotch.signature().edges.map { it.san })
    }

    @Test
    fun onlyThePositionsAfterTheSignatureBelongToTheOpening() {
        val signature = scotch.signature()
        assertTrue(signature.covers(signature.edges.last().to))
        assertFalse(signature.covers(signature.edges.first().to))
        assertFalse(signature.covers(signature.edges.first().from))
    }

    @Test
    fun aStrayLineDoesNotStopTheSignatureNorBelongToTheOpening() {
        // The Scotch with a lone 1...c5 and a lone 2. d4: the main moves carry 90 % of the positions or more.
        val withStrays =
            repertoire(
                Side.WHITE,
                "1. e4 e5 2. Nf3 Nc6 3. d4 exd4 4. Nxd4 Nf6 5. Nc3 Bb4 *",
                "1. e4 e5 2. Nf3 Nc6 3. d4 d6 4. dxe5 Bd7 5. Nc3 Nf6 *",
                "1. e4 c5 2. Nf3 d6 *",
                "1. e4 e5 2. d4 exd4 *",
            )
        val signature = withStrays.signature()
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6", "d4"), signature.edges.map { it.san })
        val afterC5 = withStrays.movesFrom(signature.edges.first().to).first { it.san == "c5" }.to
        assertFalse(signature.covers(afterC5))
    }

    @Test
    fun aRealForkEndsTheSignature() {
        val fork = repertoire(Side.WHITE, "1. e4 e5 2. Nf3 Nc6 *", "1. e4 c5 2. Nf3 d6 *")
        assertEquals(listOf("e4"), fork.signature().edges.map { it.san })
    }

    @Test
    fun aRepertoireThatBranchesAtTheStartHasNoSignature() {
        val both = repertoire(Side.WHITE, "1. e4 e5 *", "1. d4 d5 *")
        assertTrue(both.signature().isEmpty)
    }

    @Test
    fun aSingleLineIsAllSignatureAndOnlyItsLastPositionRemains() {
        val line = repertoire(Side.WHITE, "1. e4 e5 2. Nf3 *")
        val signature = line.signature()
        assertEquals(listOf("e4", "e5", "Nf3"), signature.edges.map { it.san })
        assertTrue(signature.covers(signature.edges.last().to))
    }

    @Test
    fun anEmptyRepertoireHasNoSignature() {
        assertTrue(Repertoire.empty(Side.BLACK).signature().isEmpty)
    }

    @Test
    fun theBlackRepertoireStartsWithTheOpponentsFirstMove() {
        val black = repertoire(Side.BLACK, "1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5 *", "1. e4 e5 2. Nf3 Nc6 3. Bc4 Nf6 *")
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6", "Bc4"), black.signature().edges.map { it.san })
    }
}

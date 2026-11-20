package com.chesscompanion.core.engine.maia

import com.chesscompanion.core.chess.Fen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Lc0PolicyMapTest {
    @Test
    fun `generated table matches lc0 kMoveStrs`() {
        val lc0 = MaiaReference.resource("lc0-policy-moves.txt").filter { it.isNotBlank() }
        assertEquals(Lc0PolicyMap.SIZE, lc0.size)
        assertEquals(lc0, Lc0PolicyMap.moves)
    }

    @Test
    fun `indices match lc0 for every legal move of the 20 test positions`() {
        val rows = MaiaReference.rows.filter { it.net == "1100" }
        assertEquals(20, rows.map { it.fen }.distinct().size)
        rows.groupBy { it.fen }.forEach { (fen, moves) ->
            val indices = PolicyDecoder.policyIndices(Fen.parse(fen), moves.map { it.move })
            moves.forEach { assertEquals(it.index, indices[it.move], "${it.move} in $fen") }
        }
    }

    @Test
    fun `black moves are mirrored, castling is king takes rook, knight promotion is the plain move`() {
        assertEquals(Lc0PolicyMap.moves.indexOf("e2e4"), Lc0PolicyMap.indexOf("e7e5", blackToMove = true, false))
        assertEquals(Lc0PolicyMap.moves.indexOf("e1h1"), Lc0PolicyMap.indexOf("e1g1", blackToMove = false, true))
        assertEquals(Lc0PolicyMap.moves.indexOf("e1a1"), Lc0PolicyMap.indexOf("e8c8", blackToMove = true, true))
        assertEquals(Lc0PolicyMap.moves.indexOf("e1g1"), Lc0PolicyMap.indexOf("e1g1", blackToMove = false, false))
        assertEquals(Lc0PolicyMap.moves.indexOf("b7a8"), Lc0PolicyMap.indexOf("b7a8n", blackToMove = false, false))
        assertEquals(Lc0PolicyMap.moves.indexOf("b7a8q"), Lc0PolicyMap.indexOf("b2a1q", blackToMove = true, false))
    }
}

package com.chesscompanion.core.chess

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ModelTest {
    @Test
    fun `square coordinates and names`() {
        assertEquals("a1", Square.A1.name)
        assertEquals("h8", Square.H8.name)
        val e4 = Square.of(4, 3)
        assertEquals(28, e4.index)
        assertEquals("e4", e4.name)
        assertEquals(e4, Square.parse("e4"))
        assertEquals(Square.all, Square.all.map { Square.parse(it.name) })
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "e", "e44", "i1", "a0", "a9", "E4"])
    fun `malformed square names`(name: String) {
        assertNull(Square.parse(name))
    }

    @Test
    fun `square out of range is rejected`() {
        assertThrows<IllegalArgumentException> { Square(64) }
        assertThrows<IllegalArgumentException> { Square.of(8, 0) }
    }

    @Test
    fun `uci moves`() {
        val move = Move.parseUci("e7e8q")
        assertEquals(Move(Square.parse("e7")!!, Square.E8, PieceType.QUEEN), move)
        assertEquals("e7e8q", move?.uci)
        assertEquals("e1g1", Move.parseUci("e1g1")?.uci)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "e2", "e2e", "e2e2", "e7e8k", "e7e8p", "e7e8qq", "z2e4"])
    fun `malformed uci moves`(text: String) {
        assertNull(Move.parseUci(text))
    }

    @Test
    fun `castling rights`() {
        val rights = CastlingRights.parseFen("Kq")!!
        assertTrue(rights.has(Side.WHITE, kingside = true))
        assertFalse(rights.has(Side.WHITE, kingside = false))
        assertTrue(rights.has(Side.BLACK, kingside = false))
        assertEquals("q", rights.without(Side.WHITE).toFen())
        assertEquals("-", CastlingRights.NONE.toFen())
        assertEquals("KQkq", CastlingRights.ALL.toFen())
        assertNull(CastlingRights.parseFen(""))
        assertNull(CastlingRights.parseFen("KK"))
    }

    @Test
    fun `piece fen letters`() {
        assertEquals('N', Piece(PieceType.KNIGHT, Side.WHITE).fenChar)
        assertEquals(Piece(PieceType.QUEEN, Side.BLACK), Piece.fromFenChar('q'))
        assertNull(Piece.fromFenChar('x'))
    }

    @Test
    fun `sides are opposite`() {
        assertEquals(Side.BLACK, Side.WHITE.opposite)
        assertEquals(Side.WHITE, Side.BLACK.opposite)
    }
}

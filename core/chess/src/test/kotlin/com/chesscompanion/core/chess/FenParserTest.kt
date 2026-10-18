package com.chesscompanion.core.chess

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FenParserTest {
    @Test
    fun `start position has the expected pieces`() {
        val board = FenParser.parseBoard(FenParser.START_POSITION)
        assertEquals(Piece(PieceType.ROOK, Side.WHITE), board.pieceAt(0, 0))
        assertEquals(Piece(PieceType.KING, Side.WHITE), board.pieceAt(4, 0))
        assertEquals(Piece(PieceType.PAWN, Side.WHITE), board.pieceAt(3, 1))
        assertNull(board.pieceAt(3, 3))
        assertEquals(Piece(PieceType.PAWN, Side.BLACK), board.pieceAt(7, 6))
        assertEquals(Piece(PieceType.QUEEN, Side.BLACK), board.pieceAt(3, 7))
    }

    @Test
    fun `sparse endgame position`() {
        val board = FenParser.parseBoard("8/5k2/8/3K4/8/8/4P3/8 w - - 0 1")
        assertEquals(Piece(PieceType.KING, Side.BLACK), board.pieceAt(5, 6))
        assertEquals(Piece(PieceType.KING, Side.WHITE), board.pieceAt(3, 4))
        assertEquals(Piece(PieceType.PAWN, Side.WHITE), board.pieceAt(4, 1))
        assertNull(board.pieceAt(0, 0))
    }

    @Test
    fun `placement-only string is accepted`() {
        val board = FenParser.parseBoard("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R")
        assertEquals(Piece(PieceType.KNIGHT, Side.WHITE), board.pieceAt(5, 2))
        assertEquals(Piece(PieceType.KNIGHT, Side.BLACK), board.pieceAt(2, 5))
    }

    @Test
    fun `wrong rank count is rejected`() {
        assertThrows<InvalidFenException> { FenParser.parseBoard("8/8/8/8/8/8/8") }
    }

    @Test
    fun `short and long ranks are rejected`() {
        assertThrows<InvalidFenException> { FenParser.parseBoard("7/8/8/8/8/8/8/8") }
        assertThrows<InvalidFenException> { FenParser.parseBoard("9/8/8/8/8/8/8/8") }
        assertThrows<InvalidFenException> { FenParser.parseBoard("pppppppp1/8/8/8/8/8/8/8") }
    }

    @Test
    fun `unknown piece letter is rejected`() {
        assertThrows<InvalidFenException> { FenParser.parseBoard("x7/8/8/8/8/8/8/8") }
    }
}

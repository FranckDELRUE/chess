package com.chesscompanion.core.chess

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FenTest {
    @Test
    fun `start position has the expected pieces`() {
        val board = Fen.parseBoard(Fen.START)
        assertEquals(Piece(PieceType.ROOK, Side.WHITE), board.pieceAt(0, 0))
        assertEquals(Piece(PieceType.KING, Side.WHITE), board.pieceAt(4, 0))
        assertEquals(Piece(PieceType.PAWN, Side.WHITE), board.pieceAt(3, 1))
        assertNull(board.pieceAt(3, 3))
        assertEquals(Piece(PieceType.PAWN, Side.BLACK), board.pieceAt(7, 6))
        assertEquals(Piece(PieceType.QUEEN, Side.BLACK), board.pieceAt(3, 7))
    }

    @Test
    fun `start position fields`() {
        val position = Fen.parse(Fen.START)
        assertEquals(Side.WHITE, position.sideToMove)
        assertEquals(CastlingRights.ALL, position.castling)
        assertNull(position.enPassant)
        assertEquals(0, position.halfmoveClock)
        assertEquals(1, position.fullmoveNumber)
        assertEquals(Position.START, position)
    }

    @Test
    fun `placement-only string is accepted by parseBoard`() {
        val board = Fen.parseBoard("r1bqkbnr/pppp1ppp/2n5/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R")
        assertEquals(Piece(PieceType.KNIGHT, Side.WHITE), board.pieceAt(5, 2))
        assertEquals(Piece(PieceType.KNIGHT, Side.BLACK), board.pieceAt(2, 5))
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            Fen.START,
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1",
            "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3",
            // Perft reference positions (chessprogramming.org).
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/3P1N2/PPP1NPPP/R4RK1 w - - 0 10",
            "8/5k2/8/3K4/8/8/4P3/8 b - - 99 120",
            "4k3/8/8/8/8/8/8/R3K3 w Q - 0 1",
        ],
    )
    fun `fen round trip`(fen: String) {
        assertEquals(fen, Fen.parse(fen).toFen())
    }

    @Test
    fun `four-field fen gets default counters`() {
        val position = Fen.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -")
        assertEquals(Fen.START, position.toFen())
    }

    @Test
    fun `extra whitespace is tolerated and castling letters are normalised`() {
        val position = Fen.parse("  r3k2r/8/8/8/8/8/8/R3K2R   w   qkQK  -  3  12 ")
        assertEquals("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 3 12", position.toFen())
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            // Malformed fields.
            "8/8/8/8/8/8/8 w - - 0 1",
            "7/8/8/8/8/8/8/8 w - - 0 1",
            "9/8/8/8/8/8/8/8 w - - 0 1",
            "x7/8/8/8/8/8/8/8 w - - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR x KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkx - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KKq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq e9 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - -1 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 0",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - a 1",
            // Kings.
            "8/8/8/8/8/8/8/8 w - - 0 1",
            "4k3/8/8/8/8/8/8/8 w - - 0 1",
            "4k3/8/8/8/8/8/8/3KK3 w - - 0 1",
            // Pawns.
            "P3k3/8/8/8/8/8/8/4K3 w - - 0 1",
            "4k3/8/8/8/8/8/8/p3K3 w - - 0 1",
            "4k3/8/8/8/8/PPPPPPPP/P7/4K3 w - - 0 1",
            // Castling rights without king or rook.
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBN1 w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/1NBQKBNR w KQkq - 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQ1BNR w KQkq - 0 1",
            "rnbq1bnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            // En passant: wrong rank for the side to move, no pawn, blocked squares.
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e3 0 1",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR b KQkq e3 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/4N3/PPPP1PPP/RNBQKB1R b KQkq e3 0 1",
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPPNPPP/RNBQKB1R b KQkq e3 0 1",
        ],
    )
    fun `invalid fen is rejected`(fen: String) {
        assertThrows<InvalidFenException> { Fen.parse(fen) }
    }

    @Test
    fun `error message explains the problem`() {
        val error =
            assertThrows<InvalidFenException> {
                Fen.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBN1 w KQkq - 0 1")
            }
        assertEquals("Castling right white kingside without the rook on its square", error.message)
    }

    @Test
    fun `invalid position built in code is rejected`() {
        val board = Fen.parseBoard("8/8/8/8/8/8/8/4K3")
        assertThrows<InvalidPositionException> {
            Position(board, Side.WHITE, CastlingRights.NONE, null, 0, 1)
        }
    }
}

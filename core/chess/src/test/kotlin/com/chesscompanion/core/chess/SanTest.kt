package com.chesscompanion.core.chess

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class SanTest {
    private fun san(
        fen: String,
        uci: String,
        letters: PieceLetters = PieceLetters.ENGLISH,
    ): String = Fen.parse(fen).toSan(Move.parseUci(uci)!!, letters)

    private fun parse(
        fen: String,
        text: String,
    ): String = San.parse(Fen.parse(fen), text).uci

    @ParameterizedTest(name = "{2} in {0}")
    @CsvSource(
        delimiter = ';',
        value = [
            // Plain moves, captures, castling.
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; e2e4; e4",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; g1f3; Nf3",
            "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2; e4d5; exd5",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1; e1g1; O-O",
            "r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1; e8c8; O-O-O",
            "5k2/8/8/8/8/8/8/4K2R w K - 0 1; e1g1; O-O+",
            // Disambiguation by file, rank, then square.
            "4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1; b1d2; Nbd2",
            "4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1; f1d2; Nfd2",
            "4k3/8/8/R7/8/8/8/R3K3 w - - 0 1; a1a3; R1a3",
            "4k3/8/8/R7/8/8/8/R3K3 w - - 0 1; a5a3; R5a3",
            "8/8/1k6/8/4Q2Q/8/8/K6Q w - - 0 1; h4e1; Qh4e1",
            "8/8/1k6/8/4Q2Q/8/8/K6Q w - - 0 1; e4e1; Qee1",
            "8/8/1k6/8/4Q2Q/8/8/K6Q w - - 0 1; h1e1; Q1e1",
            // A pinned knight is not a rival: no disambiguation.
            "4k3/8/8/b7/8/2N3N1/8/4K3 w - - 0 1; g3e2; Ne2",
            // En passant with check, promotions with check and mate.
            "7k/8/8/8/3Pp3/8/2K5/8 b - d3 0 1; e4d3; exd3+",
            "k7/4P3/1K6/8/8/8/8/8 w - - 0 1; e7e8q; e8=Q#",
            "k7/4P3/1K6/8/8/8/8/8 w - - 0 1; e7e8r; e8=R#",
            "k7/4P3/1K6/8/8/8/8/8 w - - 0 1; e7e8n; e8=N",
            "8/P1k5/K7/8/8/8/8/8 w - - 0 1; a7a8n; a8=N+",
            // Checkmate.
            "rnbqkbnr/pppp1ppp/8/4p3/6P1/5P2/PPPPP2P/RNBQKBNR b KQkq - 0 2; d8h4; Qh4#",
        ],
    )
    fun `writes san`(
        fen: String,
        uci: String,
        expected: String,
    ) {
        assertEquals(expected, san(fen, uci))
        assertEquals(uci, parse(fen, expected))
    }

    @Test
    fun `french piece letters for display`() {
        assertEquals("Cbd2", san("4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1", "b1d2", PieceLetters.FRENCH))
        assertEquals("e8=D#", san("k7/4P3/1K6/8/8/8/8/8 w - - 0 1", "e7e8q", PieceLetters.FRENCH))
        assertEquals("T1a3", san("4k3/8/8/R7/8/8/8/R3K3 w - - 0 1", "a1a3", PieceLetters.FRENCH))
        assertEquals("Fxc3", san("4k3/8/8/8/8/2n5/1P1B4/4K3 w - - 0 1", "d2c3", PieceLetters.FRENCH))
        assertEquals("O-O", san("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1", "e1g1", PieceLetters.FRENCH))
    }

    @ParameterizedTest(name = "{1} in {0}")
    @CsvSource(
        delimiter = ';',
        value = [
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; Nf3!?; g1f3",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; Nf3+; g1f3",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; Ng1f3; g1f3",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; Ng1-f3; g1f3",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; e2e4; e2e4",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; e2-e4; e2e4",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1; '  e4  '; e2e4",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1; 0-0; e1g1",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1; 0-0-0; e1c1",
            "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1; O-O-O+; e1c1",
            "8/4P3/8/8/8/k7/8/4K3 w - - 0 1; e8=Q; e7e8q",
            "8/4P3/8/8/8/k7/8/4K3 w - - 0 1; e8Q; e7e8q",
            "8/4P3/8/8/8/k7/8/4K3 w - - 0 1; e8=q; e7e8q",
            "8/4P3/8/8/8/k7/8/4K3 w - - 0 1; e8(N); e7e8n",
            "8/4P3/8/8/8/k7/8/4K3 w - - 0 1; e8=R+!; e7e8r",
            "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3; exf6 e.p.; e5f6",
            "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3; exf6ep; e5f6",
            "rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3; ef6; e5f6",
            "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2; e4:d5; e4d5",
            "4k3/8/8/8/8/2n5/1P1B4/4K3 w - - 0 1; bxc3; b2c3",
            "4k3/8/8/8/8/2n5/1P1B4/4K3 w - - 0 1; Bxc3; d2c3",
            "4k3/8/8/8/8/2n5/1P1B4/4K3 w - - 0 1; Bc3; d2c3",
        ],
    )
    fun `reads san leniently`(
        fen: String,
        text: String,
        uci: String,
    ) {
        assertEquals(uci, parse(fen, text))
    }

    @Test
    fun `ambiguous san is rejected`() {
        val error = assertThrows<InvalidMoveNotationException> { parse("4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1", "Nd2") }
        assertEquals(
            "Ambiguous SAN 'Nd2' (b1d2, f1d2) in 4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1",
            error.message,
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Zz9", "e9", "Nf", "O-0", "nf3", "K", "e4e5e6"])
    fun `malformed san is rejected`(text: String) {
        assertThrows<InvalidMoveNotationException> { San.parse(Position.START, text) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["e5", "Nf4", "Ke2", "O-O", "exd3", "e4=Q", "Bb5"])
    fun `illegal san is rejected`(text: String) {
        assertThrows<IllegalMoveException> { San.parse(Position.START, text) }
    }

    @Test
    fun `promotion without a piece is rejected`() {
        assertThrows<IllegalMoveException> { parse("8/4P3/8/8/8/k7/8/4K3 w - - 0 1", "e8") }
    }

    @Test
    fun `writing an illegal move is rejected`() {
        assertThrows<IllegalMoveException> { Position.START.toSan(Move.parseUci("e2e5")!!) }
    }

    @Test
    fun `every legal move round-trips through a unique san`() {
        listOf(
            Fen.START,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
            "8/8/1k6/8/4Q2Q/8/8/K6Q w - - 0 1",
        ).forEach { checkRoundTrip(Fen.parse(it), depth = 2) }
    }

    private fun checkRoundTrip(
        position: Position,
        depth: Int,
    ) {
        val sans =
            position.legalMoves.map { move ->
                position.toSan(move).also { assertEquals(move, San.parse(position, it)) }
            }
        assertEquals(sans.size, sans.toSet().size, "Duplicate SAN in ${position.toFen()}: $sans")
        if (depth > 1) position.legalMoves.forEach { checkRoundTrip(position.play(it), depth - 1) }
    }

    @Test
    fun `play helpers`() {
        val position =
            Position.START
                .playSan("e4")
                .playSan("e5")
                .playUci("g1f3")
        assertEquals("rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2", position.toFen())
    }

    @Test
    fun `uci is checked against the legal moves`() {
        assertEquals("e2e4", Uci.parse(Position.START, " e2e4 ").uci)
        assertThrows<IllegalMoveException> { Uci.parse(Position.START, "e1g1") }
        assertThrows<IllegalMoveException> { Uci.parse(Position.START, "e2e5") }
        assertThrows<InvalidMoveNotationException> { Uci.parse(Position.START, "e2e9") }
        assertThrows<InvalidMoveNotationException> { Uci.parse(Position.START, "Nf3") }
    }
}

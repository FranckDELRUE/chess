package com.chesscompanion.core.chess.pgn

import com.chesscompanion.core.chess.DrawReason
import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.IllegalMoveException
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GameSessionTest {
    private fun GameSession.play(vararg uci: String): GameSession =
        uci.fold(this) { s, m -> s.play(Move.parseUci(m)!!) }

    @Test
    fun `new session starts from the initial position`() {
        val session = GameSession.new()
        assertEquals(Position.START, session.position)
        assertNull(session.lastMove)
        assertEquals(GameStatus.Ongoing, session.status())
        assertEquals(Game.SEVEN_TAG_ROSTER, session.game.tags)
    }

    @Test
    fun `playing moves builds the main line`() {
        val session = GameSession.new().play("e2e4", "e7e5", "g1f3")
        assertEquals(listOf("e4", "e5", "Nf3"), session.game.mainline.map { it.san })
        assertEquals(listOf(0, 0, 0), session.path)
        assertEquals(Move.parseUci("g1f3"), session.lastMove)
        assertEquals(Side.BLACK, session.position.sideToMove)
    }

    @Test
    fun `an existing move is followed, a new one becomes a variation`() {
        val played = GameSession.new().play("e2e4", "e7e5")
        val back = played.copy(path = listOf(0))
        assertEquals(played, back.play("e7e5"))
        val variation = back.play("c7c5")
        assertEquals(listOf(0, 1), variation.path)
        assertEquals(
            listOf("e5", "c5"),
            variation.game.moves[0]
                .children
                .map { it.san },
        )
        assertEquals(listOf("e4", "e5"), variation.game.mainline.map { it.san })
    }

    @Test
    fun `illegal moves are rejected`() {
        assertThrows<IllegalMoveException> { GameSession.new().play("e2e5") }
    }

    @Test
    fun `status counts repetitions along the line`() {
        val shuffle = arrayOf("g1f3", "g8f6", "f3g1", "f6g8")
        val session = GameSession.new().play(*shuffle, *shuffle)
        assertEquals(GameStatus.Draw(DrawReason.THREEFOLD_REPETITION), session.status())
    }

    @Test
    fun `finished game exports its result`() {
        val mate = GameSession.new().play("f2f3", "e7e5", "g2g4", "d8h4")
        assertEquals(GameStatus.Checkmate(Side.BLACK), mate.status())
        val game = mate.toGame()
        assertEquals(GameResult.BLACK_WINS, game.result)
        assertEquals("0-1", game.tag("Result"))
        assertEquals(game, PgnReader.read(PgnWriter.write(game)))
    }

    @Test
    fun `new game from a position records SetUp and FEN`() {
        val start = Fen.parse("4k3/8/8/8/8/8/4P3/4K3 w - - 0 1")
        val game = GameSession.new(start).play("e2e4").toGame()
        assertEquals("1", game.tag("SetUp"))
        assertEquals(start.toFen(), game.tag("FEN"))
        assertEquals(game, PgnReader.read(PgnWriter.write(game)))
    }

    @Test
    fun `navigation moves along the current line`() {
        val end = GameSession.new().play("e2e4", "e7e5", "g1f3")
        val start = end.toStart()
        assertEquals(Position.START, start.position)
        assertEquals(false, start.canGoBack)
        assertEquals(listOf(0), start.forward().path)
        assertEquals(end, start.toEnd())
        assertEquals(listOf(0, 0), end.back().path)
        assertEquals(end, end.forward())
        assertEquals(start, start.back())
        assertEquals(false, end.canGoForward)
    }

    @Test
    fun `go to a variation and continue to its end`() {
        val main = GameSession.new().play("e2e4", "e7e5", "g1f3")
        val withVariation = main.goTo(listOf(0)).play("c7c5", "g1f3")
        assertEquals(listOf(0, 1, 0), withVariation.path)
        val inVariation = withVariation.goTo(listOf(0, 1))
        assertEquals(Move.parseUci("c7c5"), inVariation.lastMove)
        assertEquals(listOf(0, 1, 0), inVariation.toEnd().path)
        assertEquals(listOf("Nf3"), inVariation.nextMoves.map { it.san })
        assertThrows<IllegalArgumentException> { main.goTo(listOf(0, 3)) }
    }

    @Test
    fun `an unfinished position keeps the result read from pgn`() {
        val resigned = PgnReader.read("[Result \"0-1\"]\n\n1. e4 e5 0-1")
        assertEquals(GameResult.BLACK_WINS, GameSession(resigned).toGame().result)
        assertEquals(
            GameResult.UNKNOWN,
            GameSession
                .new()
                .play("e2e4")
                .toGame()
                .result,
        )
    }
}

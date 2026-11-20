package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.pgn.GameSession
import com.chesscompanion.core.chess.pgn.PgnReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MoveListBuilderTest {
    /** Compact text of the list: moves, numbers, comments in braces, variations in parentheses, current in brackets. */
    private fun render(session: GameSession): String =
        MoveListBuilder.build(session).joinToString(" ") { item ->
            when (item) {
                is MoveListItem.Number -> item.text
                is MoveListItem.Move -> if (item.current) "[${item.text}]" else item.text
                is MoveListItem.Comment -> "{${item.text}}"
                is MoveListItem.VariationStart -> "("
                is MoveListItem.VariationEnd -> ")"
            }
        }

    private fun session(pgn: String) = PgnReader.read(pgn).let { GameSession(it) }

    @Test
    fun `main line in french notation`() {
        val game = session("1. e4 e5 2. Nf3 Nc6 3. Bb5 a6 4. O-O Qe7 5. Re1 *")
        assertEquals("1. e4 e5 2. Cf3 Cc6 3. Fb5 a6 4. O-O De7 5. Te1", render(game))
    }

    @Test
    fun `variations, numbering after them and the current move`() {
        val game = session("1. e4 e5 (1... c5 2. Nf3 (2. c3) 2... d6) 2. Nf3 *")
        val current = game.goTo(listOf(0, 1, 1))
        assertEquals("1. e4 e5 ( 1... c5 2. Cf3 ( 2. [c3] ) 2... d6 ) 2. Cf3", render(current))
    }

    @Test
    fun `depth of nested variations`() {
        val items = MoveListBuilder.build(session("1. e4 e5 (1... c5 2. Nf3 (2. c3)) *"))
        val depths = items.filterIsInstance<MoveListItem.Move>().associate { it.text to it.depth }
        assertEquals(mapOf("e4" to 0, "e5" to 0, "c5" to 1, "Cf3" to 1, "c3" to 2), depths)
    }

    @Test
    fun `annotations shown, clock commands hidden`() {
        val game = session("1. e4! {[%clk 0:10:00]} e5?! {[%clk 0:09:58] Solide} 2. Nf3 \$14 *")
        assertEquals("1. e4! e5?! {Solide} 2. Cf3", render(game))
    }

    @Test
    fun `game from a position with black to move`() {
        val fen = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"
        val game = session("[SetUp \"1\"]\n[FEN \"$fen\"]\n\n1... e5 2. Nf3 *")
        assertEquals("1... e5 2. Cf3", render(game))
    }
}

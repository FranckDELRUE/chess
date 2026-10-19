package com.chesscompanion.core.chess.pgn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/** Sprint S1 acceptance: the 100 real games of Heidoji are legal and re-imported identically. */
class HeidojiCorpusTest {
    private val text = File("../../testdata/pgn/chesscom-heidoji-100.pgn").readText()

    @Test
    fun `all games are legal and match chess com's final position`() {
        val start = System.nanoTime()
        val import = PgnReader.readAll(text)
        val millis = (System.nanoTime() - start) / NANOS_PER_MILLI
        val plies = import.games.sumOf { it.mainline.size }
        println("Heidoji corpus: ${import.games.size} games, $plies plies read in $millis ms")

        assertEquals(emptyList<PgnIssue>(), import.errors)
        assertEquals(emptyList<PgnIssue>(), import.warnings)
        assertEquals(GAMES, import.games.size)
        import.games.forEach { game ->
            val link = game.tag("Link")
            // Chess.com writes 4 or 6 FEN fields: compare placement, side, castling and en passant.
            val expected = game.tag("CurrentPosition")!!.split(' ').take(FEN_FIELDS_COMPARED)
            assertEquals(
                expected,
                game.finalPosition
                    .toFen()
                    .split(' ')
                    .take(FEN_FIELDS_COMPARED),
                link,
            )
            // Chess.com writes one clock comment per move.
            val clocks = game.mainline.count { it.commentsAfter.single().startsWith("[%clk ") }
            assertEquals(game.mainline.size, clocks, link)
        }
    }

    @Test
    fun `import, export, import gives identical games and stable text`() {
        val games = PgnReader.readAll(text).games
        val exported = PgnWriter.writeAll(games)
        val reimport = PgnReader.readAll(exported)
        assertEquals(emptyList<PgnIssue>(), reimport.errors)
        assertEquals(games, reimport.games)
        assertEquals(exported, PgnWriter.writeAll(reimport.games))
    }

    private companion object {
        const val GAMES = 100
        const val FEN_FIELDS_COMPARED = 4
        const val NANOS_PER_MILLI = 1_000_000
    }
}

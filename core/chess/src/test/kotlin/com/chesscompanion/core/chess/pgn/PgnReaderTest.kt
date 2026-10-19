package com.chesscompanion.core.chess.pgn

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PgnReaderTest {
    private val annotated =
        """
        [Event "Test"]
        [Site "?"]
        [Date "2026.10.19"]
        [Round "1"]
        [White "A"]
        [Black "B"]
        [Result "1-0"]

        {Opening comment} 1. e4 ${'$'}1 {Best by
        test} e5 (1... c5 2. Nf3 (2. c3) 2... d6) (1... e6!?) 2. Nf3 Nc6?! ; line comment
        % escaped line, ignored
        3. Bb5 1-0
        """.trimIndent()

    private fun roundTrip(game: Game): Game = PgnReader.read(PgnWriter.write(game))

    @Test
    fun `moves, variations, comments and annotations`() {
        val game = PgnReader.read(annotated)
        assertEquals(listOf("e4", "e5", "Nf3", "Nc6", "Bb5"), game.mainline.map { it.san })
        val e4 = game.moves.single()
        assertEquals(listOf("Opening comment"), e4.commentsBefore)
        assertEquals(listOf(1), e4.nags)
        assertEquals(listOf("Best by test"), e4.commentsAfter)
        assertEquals(listOf("e5", "c5", "e6"), e4.children.map { it.san })
        val c5 = e4.children[1]
        assertEquals(listOf("Nf3", "c3"), c5.children.map { it.san })
        assertEquals(listOf("d6"), c5.children[0].children.map { it.san })
        assertEquals(listOf(5), e4.children[2].nags)
        val nc6 = game.mainline[3]
        assertEquals(listOf(6), nc6.nags)
        assertEquals(listOf("line comment"), nc6.commentsAfter)
        assertEquals(GameResult.WHITE_WINS, game.result)
        assertEquals(game, roundTrip(game))
    }

    @Test
    fun `canonical export`() {
        val text = PgnWriter.write(PgnReader.read(annotated))
        val (header, movetext) = text.split("\n\n")
        assertEquals(
            listOf("Event", "Site", "Date", "Round", "White", "Black", "Result"),
            header.lines().map { it.substringAfter('[').substringBefore(' ') },
        )
        assertTrue(text.lines().all { it.length <= PgnWriter.MAX_LINE_LENGTH }, text)
        assertEquals(
            "{Opening comment} 1. e4 \$1 {Best by test} 1... e5 ( 1... c5 2. Nf3 ( 2. c3 ) 2... d6 ) " +
                "( 1... e6 \$5 ) 2. Nf3 Nc6 \$6 {line comment} 3. Bb5 1-0",
            movetext.trim().replace('\n', ' '),
        )
        assertEquals(text, PgnWriter.write(PgnReader.read(text)))
    }

    @Test
    fun `game starting from a fen with black to move`() {
        val game =
            PgnReader.read(
                """
                [SetUp "1"]
                [FEN "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1"]

                1... e5 2. Nf3 *
                """.trimIndent(),
            )
        assertEquals(Side.BLACK, game.startPosition.sideToMove)
        assertEquals(listOf("e5", "Nf3"), game.mainline.map { it.san })
        assertEquals(
            listOf("Event", "Site", "Date", "Round", "White", "Black", "Result", "SetUp", "FEN"),
            game.tags.map { it.name },
        )
        assertEquals("????.??.??", game.tag("Date"))
        assertTrue(PgnWriter.write(game).contains("1... e5 2. Nf3 *"))
        assertEquals(game, roundTrip(game))
    }

    @Test
    fun `several games, a faulty one is skipped and located`() {
        val text =
            """
            [Event "One"]

            1. e4 e5 1-0

            [Event "Two"]
            [Result "*"]

            1. e4 e5
            2. Ke3 *

            [Event "Three"]

            1. d4 d5 0-1
            """.trimIndent()
        val import = PgnReader.readAll(text)
        assertEquals(listOf("One", "Three"), import.games.map { it.tag("Event") })
        val error = import.errors.single()
        assertEquals(2, error.gameNumber)
        assertEquals(9, error.line)
        assertTrue(error.message.startsWith("Move 2. Ke3: No legal move matches 'Ke3'"), error.message)
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "1. e4 {unterminated *",
            "[Event Test]\n\n1. e4 *",
            "[FEN \"8/8/8/8/8/8/8/8 w - - 0 1\"]\n\n1. e4 *",
            "1. e4 e5 2. Nf3 Nc6 3. Nc3 Nf6 4. Ne2 h6 5. h3 a6 6. Nd4 *",
            "1. e4 e5 ) *",
            "1. e4 (1. d4 *",
            "( 1. e4 ) *",
            "\$1 1. e4 *",
            "1. e4 1-0 e5",
            "1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7# Ke7 1-0",
            "1. e4 Zz9 *",
        ],
    )
    fun `invalid pgn is rejected with a line`(text: String) {
        val error = assertThrows<PgnException> { PgnReader.read(text) }
        assertTrue(error.line >= 1)
    }

    @Test
    fun `ambiguous move error names the candidates`() {
        val error =
            assertThrows<PgnException> {
                PgnReader.read("1. e4 e5 2. Nf3 Nc6 3. Nc3 Nf6 4. Ne2 h6 5. h3 a6 6. Nd4 *")
            }
        assertEquals(1, error.line)
        assertTrue(error.detail.startsWith("Move 6. Nd4: Ambiguous SAN 'Nd4' (e2d4, f3d4)"), error.detail)
    }

    @Test
    fun `result marker wins over the tag, the tag fills a missing marker`() {
        val mismatch = PgnReader.readAll("[Result \"0-1\"]\n\n1. e4 1-0")
        assertEquals(GameResult.WHITE_WINS, mismatch.games.single().result)
        assertEquals("1-0", mismatch.games.single().tag("Result"))
        assertEquals(1, mismatch.warnings.size)

        val missing = PgnReader.readAll("[Result \"1/2-1/2\"]\n\n1. e4 e5")
        assertEquals(GameResult.DRAW, missing.games.single().result)
        assertTrue(
            missing.warnings
                .single()
                .message
                .startsWith("Missing result"),
        )
    }

    @Test
    fun `escapes, bom, crlf, glued move numbers and duplicate tags`() {
        val text = "[White \"O\\\"Neil \\\\ Jr\"]\r\n[White \"Again\"]\r\n\r\n1.e4 e5 2.Nf3 *\r\n"
        val import = PgnReader.readAll(text)
        val game = import.games.single()
        assertEquals("Again", game.tag("White"))
        assertEquals(listOf("e4", "e5", "Nf3"), game.mainline.map { it.san })
        assertTrue(
            import.warnings
                .single()
                .message
                .startsWith("Duplicate tag 'White'"),
        )

        val escaped = PgnReader.read("[White \"O\\\"Neil \\\\ Jr\"]\n\n*")
        assertEquals("O\"Neil \\ Jr", escaped.tag("White"))
        assertEquals(escaped, roundTrip(escaped))
    }

    @Test
    fun `game without moves keeps its comment`() {
        val game = PgnReader.read("{Only a comment} *")
        assertEquals(listOf("Only a comment"), game.comments)
        assertEquals(Position.START, game.finalPosition)
        assertEquals(game, roundTrip(game))
    }

    @Test
    fun `game status follows the main line`() {
        val game = PgnReader.read("1. f3 e5 2. g4 Qh4# 0-1")
        assertEquals(GameStatus.Checkmate(Side.BLACK), game.status())
        assertEquals(Fen.parse("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3"), game.finalPosition)
    }

    @Test
    fun `brackets inside comments do not split games`() {
        val text = "[Event \"A\"]\n\n1. e4 {multi\n[not a tag]} e5 *\n\n[Event \"B\"]\n\n1. d4 *"
        val import = PgnReader.readAll(text)
        assertEquals(listOf("A", "B"), import.games.map { it.tag("Event") })
        assertEquals(listOf("multi [not a tag]"), import.games[0].mainline[0].commentsAfter)
    }
}

package com.chesscompanion.core.chess.repertoire

import com.chesscompanion.core.chess.Square

/** A PGN comment split into its text and the marks written in it (`[%cal ...]`, `[%csl ...]`). */
data class ParsedComment(
    val text: String,
    val arrows: List<RepertoireArrow>,
    val squares: List<RepertoireSquareMark>,
)

/**
 * Reads and writes the drawing commands that Lichess and Chess.com put in PGN comments:
 * `[%cal Ge2e4,Rd7d5]` for arrows and `[%csl Gd4]` for squares, each item being a colour letter
 * followed by the squares. Green, red and blue are kept; any other colour (yellow) is dropped, as
 * is any malformed item.
 */
object PgnMarks {
    private val command = Regex("""\[%(cal|csl)\s+([^\]]*)]""")
    private val spaces = Regex("[ \\t]{2,}")
    private val letters = mapOf('G' to MarkColor.GREEN, 'R' to MarkColor.RED, 'B' to MarkColor.BLUE)
    private const val SQUARE_LENGTH = 2

    fun parse(comment: String): ParsedComment {
        val arrows = mutableListOf<RepertoireArrow>()
        val squares = mutableListOf<RepertoireSquareMark>()
        command.findAll(comment).forEach { match ->
            val items =
                match.groupValues[2]
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            if (match.groupValues[1] == "cal") {
                items.mapNotNullTo(arrows, ::arrowOf)
            } else {
                items.mapNotNullTo(squares, ::squareOf)
            }
        }
        val text = comment.replace(command, " ").replace(spaces, " ").trim()
        return ParsedComment(text, arrows.distinct(), squares.distinct())
    }

    /** The comment to write for [notes]: its text then its marks; null when there is nothing to write. */
    fun format(notes: NodeNotes): String? {
        val parts =
            listOfNotNull(
                notes.comment.takeIf { it.isNotEmpty() },
                notes.squares.takeIf { it.isNotEmpty() }?.joinToString(",", "[%csl ", "]") { item ->
                    "${letterOf(item.color)}${item.square}"
                },
                notes.arrows.takeIf { it.isNotEmpty() }?.joinToString(",", "[%cal ", "]") { item ->
                    "${letterOf(item.color)}${item.from}${item.to}"
                },
            )
        return parts.joinToString(" ").ifEmpty { null }
    }

    private fun letterOf(color: MarkColor): Char = letters.entries.first { it.value == color }.key

    private fun arrowOf(item: String): RepertoireArrow? {
        val color = letters[item.firstOrNull()] ?: return null
        val (from, to) = twoSquares(item.drop(1)) ?: return null
        return if (from == to) null else RepertoireArrow(from, to, color)
    }

    private fun twoSquares(text: String): Pair<Square, Square>? {
        val from = Square.parse(text.take(SQUARE_LENGTH))
        val to = Square.parse(text.drop(SQUARE_LENGTH))
        return if (text.length == 2 * SQUARE_LENGTH && from != null && to != null) from to to else null
    }

    private fun squareOf(item: String): RepertoireSquareMark? {
        val color = letters[item.firstOrNull()] ?: return null
        return Square.parse(item.drop(1))?.let { RepertoireSquareMark(it, color) }
    }
}

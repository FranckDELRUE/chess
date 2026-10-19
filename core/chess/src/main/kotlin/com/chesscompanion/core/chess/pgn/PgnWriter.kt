package com.chesscompanion.core.chess.pgn

import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side

/**
 * Canonical PGN export: tags in the game's order (Seven Tag Roster first after import), NAGs as
 * `$n`, a move number before every White move and before a Black move that follows a comment or a
 * variation, lines of at most 80 characters (a longer comment stays on its own line).
 */
object PgnWriter {
    const val MAX_LINE_LENGTH = 80

    fun write(game: Game): String =
        buildString {
            game.tags.forEach {
                append('[')
                    .append(it.name)
                    .append(" \"")
                    .append(escape(it.value))
                    .append("\"]\n")
            }
            append('\n')
            val tokens = mutableListOf<String>()
            writeLine(game.startPosition, game.moves, tokens)
            game.comments.forEach { tokens += comment(it) }
            tokens += game.result.token
            appendWrapped(tokens)
            append('\n')
        }

    fun writeAll(games: List<Game>): String = games.joinToString("\n") { write(it) }

    /** Writes [alternatives] (a main move and its variations) played from [before], then the continuation. */
    private fun writeLine(
        before: Position,
        alternatives: List<MoveNode>,
        tokens: MutableList<String>,
    ) {
        var position = before
        var nodes = alternatives
        var needNumber = true
        while (nodes.isNotEmpty()) {
            val main = nodes.first()
            needNumber = writeMove(position, main, needNumber, tokens)
            for (variation in nodes.drop(1)) {
                tokens += "("
                writeLine(position, listOf(variation), tokens)
                tokens += ")"
                needNumber = true
            }
            position = main.position
            nodes = main.children
        }
    }

    /** Writes one move with its comments and NAGs; returns whether the next move needs its number. */
    private fun writeMove(
        before: Position,
        node: MoveNode,
        needNumber: Boolean,
        tokens: MutableList<String>,
    ): Boolean {
        node.commentsBefore.forEach { tokens += comment(it) }
        val white = before.sideToMove == Side.WHITE
        if (white) {
            tokens += "${before.fullmoveNumber}."
        } else if (needNumber || node.commentsBefore.isNotEmpty()) {
            tokens += "${before.fullmoveNumber}..."
        }
        tokens += node.san
        node.nags.forEach { tokens += "$$it" }
        node.commentsAfter.forEach { tokens += comment(it) }
        return node.commentsAfter.isNotEmpty()
    }

    /** A comment cannot contain `}` in PGN: it is replaced by `)`. */
    private fun comment(text: String): String = "{${text.replace('}', ')')}}"

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun StringBuilder.appendWrapped(tokens: List<String>) {
        var lineLength = 0
        for (token in tokens) {
            if (lineLength > 0 && lineLength + 1 + token.length > MAX_LINE_LENGTH) {
                append('\n')
                lineLength = 0
            }
            if (lineLength > 0) {
                append(' ')
                lineLength++
            }
            append(token)
            lineLength += token.length
        }
    }
}

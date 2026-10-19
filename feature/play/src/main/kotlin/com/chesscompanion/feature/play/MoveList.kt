package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.PieceLetters
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.San
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.GameSession
import com.chesscompanion.core.chess.pgn.MoveNode

/** One element of the move list shown next to the board. [depth] 0 is the main line. */
sealed interface MoveListItem {
    val depth: Int

    /** "12." before a White move, "12..." before a Black move that does not follow its White move. */
    data class Number(
        val text: String,
        override val depth: Int,
    ) : MoveListItem

    data class Move(
        /** Path of the move in the game tree, for [GameSession.goTo]. */
        val path: List<Int>,
        /** SAN in French letters, with its annotation (`!?`). */
        val text: String,
        val current: Boolean,
        override val depth: Int,
    ) : MoveListItem

    data class Comment(
        val text: String,
        override val depth: Int,
    ) : MoveListItem

    data class VariationStart(
        override val depth: Int,
    ) : MoveListItem

    data class VariationEnd(
        override val depth: Int,
    ) : MoveListItem
}

/**
 * Flattens the game tree into the move list, PGN style: each variation follows the move it
 * replaces, between [MoveListItem.VariationStart] and [MoveListItem.VariationEnd]. Moves are in
 * French notation (choice of S1-7); clock commands such as `[%clk 0:05:00]` are not shown.
 */
object MoveListBuilder {
    private val NAG_SYMBOLS = mapOf(1 to "!", 2 to "?", 3 to "!!", 4 to "??", 5 to "!?", 6 to "?!")
    private val COMMAND_REGEX = Regex("""\[%[^]]*]""")

    fun build(session: GameSession): List<MoveListItem> {
        val items = mutableListOf<MoveListItem>()
        Builder(
            session.path,
            items,
        ).line(
            session.game.startPosition,
            session.game.moves
                .withIndex()
                .toList(),
            emptyList(),
            0,
        )
        return items
    }

    private class Builder(
        private val current: List<Int>,
        private val items: MutableList<MoveListItem>,
    ) {
        /** Writes [alternatives] (main move first, then its variations) played from [before], then the continuation. */
        fun line(
            before: Position,
            alternatives: List<IndexedValue<MoveNode>>,
            parentPath: List<Int>,
            depth: Int,
        ) {
            var position = before
            var parent = parentPath
            var nodes = alternatives
            var needNumber = true
            while (nodes.isNotEmpty()) {
                val (index, main) = nodes.first()
                val path = parent + index
                needNumber = move(position, main, path, needNumber, depth)
                for (variation in nodes.drop(1)) {
                    items += MoveListItem.VariationStart(depth + 1)
                    line(position, listOf(variation), parent, depth + 1)
                    items += MoveListItem.VariationEnd(depth + 1)
                    needNumber = true
                }
                position = main.position
                parent = path
                nodes = main.children.withIndex().toList()
            }
        }

        /** Adds one move with its comments; returns whether the next move needs its number. */
        private fun move(
            before: Position,
            node: MoveNode,
            path: List<Int>,
            needNumber: Boolean,
            depth: Int,
        ): Boolean {
            val commentsBefore = node.commentsBefore.mapNotNull(::visible)
            commentsBefore.forEach { items += MoveListItem.Comment(it, depth) }
            val white = before.sideToMove == Side.WHITE
            if (white || needNumber || commentsBefore.isNotEmpty()) {
                items += MoveListItem.Number("${before.fullmoveNumber}${if (white) "." else "..."}", depth)
            }
            val san = San.write(before, node.move, PieceLetters.FRENCH)
            val annotation = node.nags.mapNotNull { NAG_SYMBOLS[it] }.joinToString("")
            items += MoveListItem.Move(path, san + annotation, path == current, depth)
            val commentsAfter = node.commentsAfter.mapNotNull(::visible)
            commentsAfter.forEach { items += MoveListItem.Comment(it, depth) }
            return commentsAfter.isNotEmpty()
        }

        /** Comment text without embedded commands, or null when nothing is left. */
        private fun visible(comment: String): String? = comment.replace(COMMAND_REGEX, "").trim().ifEmpty { null }
    }
}

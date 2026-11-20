package com.chesscompanion.core.chess.pgn

import com.chesscompanion.core.chess.GameRules
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position

enum class GameResult(
    val token: String,
) {
    WHITE_WINS("1-0"),
    BLACK_WINS("0-1"),
    DRAW("1/2-1/2"),
    UNKNOWN("*"),
    ;

    companion object {
        fun fromToken(token: String): GameResult? = entries.firstOrNull { it.token == token }
    }
}

data class Tag(
    val name: String,
    val value: String,
)

/**
 * A move in the game tree (ADR 0006). [children]: the first child continues this line, the
 * others are variations replacing that first child. Immutable: an edit rebuilds the path.
 */
data class MoveNode(
    val move: Move,
    /** Canonical English SAN, as written by the rules. */
    val san: String,
    /** Position after the move. */
    val position: Position,
    val commentsBefore: List<String> = emptyList(),
    val commentsAfter: List<String> = emptyList(),
    val nags: List<Int> = emptyList(),
    val children: List<MoveNode> = emptyList(),
)

/**
 * A game: tags (Seven Tag Roster first, then the others in their original order), start
 * position, move tree and result. [moves] holds the first move and its variations.
 */
data class Game(
    val tags: List<Tag>,
    val startPosition: Position,
    val moves: List<MoveNode>,
    val result: GameResult,
    /** Comments with no move to attach to (a game without moves). */
    val comments: List<String> = emptyList(),
) {
    fun tag(name: String): String? = tags.lastOrNull { it.name == name }?.value

    /** Main line: first child at each step. */
    val mainline: List<MoveNode>
        get() = generateSequence(moves.firstOrNull()) { it.children.firstOrNull() }.toList()

    val finalPosition: Position get() = mainline.lastOrNull()?.position ?: startPosition

    /** Rules status at the end of the main line (repetition counted along it). */
    fun status(): GameStatus {
        val positions = listOf(startPosition) + mainline.map { it.position }
        return GameRules.status(positions.last(), positions.dropLast(1).map { it.zobristKey })
    }

    companion object {
        /** Seven Tag Roster, in export order, with the values used when a tag is missing. */
        val SEVEN_TAG_ROSTER =
            listOf(
                Tag("Event", "?"),
                Tag("Site", "?"),
                Tag("Date", "????.??.??"),
                Tag("Round", "?"),
                Tag("White", "?"),
                Tag("Black", "?"),
                Tag("Result", GameResult.UNKNOWN.token),
            )
    }
}

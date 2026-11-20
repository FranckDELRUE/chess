package com.chesscompanion.core.chess.repertoire

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.pgn.Game
import com.chesscompanion.core.chess.pgn.GameResult
import com.chesscompanion.core.chess.pgn.MoveNode
import com.chesscompanion.core.chess.pgn.Tag

/**
 * The repertoire as one PGN game: a tree of moves, the first move of each position being the
 * continuation and the others its variations. A position reached by several moves is expanded the
 * first time only; its notes are written there too (comment text and `[%cal]`/`[%csl]` marks), so
 * reading the game back into an empty repertoire gives the same graph and the same notes.
 * The notes of the start position have no move to hang on and are not exported.
 *
 * @param event value of the `Event` tag, usually the repertoire's name.
 */
fun Repertoire.toGame(event: String = "?"): Game {
    val start = requireNotNull(node(root)) { "The root position is missing" }.position
    val expanded = HashSet<PositionId>()

    fun expand(
        id: PositionId,
        position: Position,
    ): List<MoveNode> {
        if (!expanded.add(id)) return emptyList()
        return movesFrom(id).map { edge ->
            val reached = position.play(edge.move)
            val firstVisit = edge.to !in expanded
            MoveNode(
                move = edge.move,
                san = edge.san,
                position = reached,
                commentsAfter = if (firstVisit) listOfNotNull(PgnMarks.format(notes(edge.to))) else emptyList(),
                children = expand(edge.to, reached),
            )
        }
    }

    val tags =
        Game.SEVEN_TAG_ROSTER.map { if (it.name == "Event") Tag("Event", event) else it } +
            if (start == Position.START) {
                emptyList()
            } else {
                listOf(Tag("SetUp", "1"), Tag("FEN", Fen.write(start)))
            }
    return Game(tags, start, expand(root, start), GameResult.UNKNOWN)
}

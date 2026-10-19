package com.chesscompanion.core.chess.pgn

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.GameRules
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.San
import com.chesscompanion.core.chess.Side

/**
 * A game being played or browsed: the [game] tree and the [path] to the current move (index of
 * the child chosen at each step; empty = start position). Immutable, suited to a `StateFlow`.
 */
data class GameSession(
    val game: Game,
    val path: List<Int> = emptyList(),
) {
    /** Moves from the start to the current one. */
    val line: List<MoveNode> by lazy {
        var nodes = game.moves
        path.map { index -> nodes[index].also { nodes = it.children } }
    }

    val position: Position get() = line.lastOrNull()?.position ?: game.startPosition

    val lastMove: Move? get() = line.lastOrNull()?.move

    /** Rules status of the current position, repetition counted along [line]. */
    fun status(): GameStatus {
        val earlier = listOf(game.startPosition) + line.dropLast(1).map { it.position }
        return GameRules.status(position, earlier.map { it.zobristKey })
    }

    /**
     * Plays [move] from the current position: follows it if it is already in the tree, otherwise
     * adds it (as a variation when the current move already has a continuation). Throws
     * `IllegalMoveException` if the move is not legal.
     */
    fun play(move: Move): GameSession {
        val children = line.lastOrNull()?.children ?: game.moves
        val existing = children.indexOfFirst { it.move == move }
        if (existing >= 0) return copy(path = path + existing)
        val before = position
        val node = MoveNode(move = move, san = San.write(before, move), position = before.play(move))
        return GameSession(game.copy(moves = insert(game.moves, path, node)), path + children.size)
    }

    /** The game with its result set from the end of the main line, ready for PGN export. */
    fun toGame(): Game {
        val result = resultOf(GameSession(game, game.mainline.map { 0 }).status())
        return game.copy(
            result = result,
            tags = game.tags.map { if (it.name == "Result") it.copy(value = result.token) else it },
        )
    }

    private fun insert(
        nodes: List<MoveNode>,
        at: List<Int>,
        node: MoveNode,
    ): List<MoveNode> {
        if (at.isEmpty()) return nodes + node
        val index = at.first()
        return nodes.mapIndexed { i, child ->
            if (i == index) child.copy(children = insert(child.children, at.drop(1), node)) else child
        }
    }

    companion object {
        /** A new game without moves from [start], with the Seven Tag Roster (and SetUp/FEN if needed). */
        fun new(start: Position = Position.START): GameSession {
            val setUp =
                if (start == Position.START) emptyList() else listOf(Tag("SetUp", "1"), Tag("FEN", Fen.write(start)))
            return GameSession(Game(Game.SEVEN_TAG_ROSTER + setUp, start, emptyList(), GameResult.UNKNOWN))
        }

        fun resultOf(status: GameStatus): GameResult =
            when (status) {
                is GameStatus.Checkmate ->
                    if (status.winner ==
                        Side.WHITE
                    ) {
                        GameResult.WHITE_WINS
                    } else {
                        GameResult.BLACK_WINS
                    }
                GameStatus.Stalemate, is GameStatus.Draw -> GameResult.DRAW
                GameStatus.Ongoing -> GameResult.UNKNOWN
            }
    }
}

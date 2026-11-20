package com.chesscompanion.core.chess.repertoire

import com.chesscompanion.core.chess.Fen
import com.chesscompanion.core.chess.IllegalMoveException
import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.pgn.Game
import com.chesscompanion.core.chess.pgn.MoveNode
import com.chesscompanion.core.chess.toSan

/**
 * Identity of a repertoire position (ADR 0012): the Zobrist key, checked by [fen], the position
 * written without move counters and with the en passant square only when a capture is possible
 * (the same convention as the key). Two paths that transpose reach the same id; two different
 * positions never share one, even if their keys collided.
 */
data class PositionId(
    val key: Long,
    val fen: String,
) {
    companion object {
        fun of(position: Position): PositionId = PositionId(position.zobristKey, canonicalFen(position))

        private fun canonicalFen(position: Position): String =
            listOf(
                Fen.writeBoard(position.board),
                if (position.sideToMove == Side.WHITE) "w" else "b",
                position.castling.toFen(),
                position.enPassant?.takeIf { position.isEnPassantCapturable }?.name ?: "-",
            ).joinToString(" ")
    }
}

/**
 * A position of the repertoire. [position] is the one reached the first time; its move counters
 * depend on that path and mean nothing for the repertoire.
 */
data class RepertoireNode(
    val id: PositionId,
    val position: Position,
)

/** A move from [from] to [to]; [san] is the canonical English SAN in the position [from]. */
data class RepertoireEdge(
    val from: PositionId,
    val to: PositionId,
    val move: Move,
    val san: String,
)

enum class MarkColor { GREEN, RED, BLUE }

data class RepertoireArrow(
    val from: Square,
    val to: Square,
    val color: MarkColor,
) {
    /** True when [other] joins the same two squares, whatever its colour. */
    fun sameSquares(other: RepertoireArrow): Boolean = from == other.from && to == other.to
}

data class RepertoireSquareMark(
    val square: Square,
    val color: MarkColor,
)

/** Where a line comes from in a book: the title, and the chapter and page when known. */
data class Source(
    val book: String,
    val chapter: String? = null,
    val page: String? = null,
)

/**
 * What the player or an author wrote on a position (ADR 0012): comment, marks drawn on the board, source, and the
 * three questions that explain a move (S8b-3): its [idea], the [plan] that follows, the [trap] to watch for.
 */
data class NodeNotes(
    val comment: String = "",
    val arrows: List<RepertoireArrow> = emptyList(),
    val squares: List<RepertoireSquareMark> = emptyList(),
    val source: Source? = null,
    val idea: String = "",
    val plan: String = "",
    val trap: String = "",
) {
    val isEmpty: Boolean
        get() =
            comment.isEmpty() &&
                arrows.isEmpty() &&
                squares.isEmpty() &&
                source == null &&
                idea.isEmpty() &&
                plan.isEmpty() &&
                trap.isEmpty()
}

/** Outcome of [Repertoire.add]: [target] is where the move leads, [created] tells if the move was new. */
data class AddResult(
    val repertoire: Repertoire,
    val target: PositionId,
    val created: Boolean,
)

/** Outcome of [Repertoire.merge]. */
data class MergeResult(
    val repertoire: Repertoire,
    val addedMoves: Int,
    val existingMoves: Int,
    val addedPositions: Int,
    /** Positions whose notes (comment, marks) changed because the game commented them. */
    val notedPositions: Int = 0,
)

/** Outcome of [Repertoire.mergeAll]: [mismatched] are the indices of the games left out. */
data class BatchMerge(
    val merged: MergeResult,
    val mismatched: List<Int>,
)

/** The start position of a game is not the repertoire's. */
class StartMismatchException(
    message: String,
) : IllegalArgumentException(message)

/**
 * An opening repertoire for one [side] (ADR 0012): a directed graph whose nodes are positions and
 * whose edges are moves. Immutable. It may contain cycles (a position that repeats), so any
 * traversal must remember the nodes already visited.
 */
class Repertoire private constructor(
    val side: Side,
    val root: PositionId,
    private val nodes: Map<PositionId, RepertoireNode>,
    private val outgoing: Map<PositionId, List<RepertoireEdge>>,
    private val notes: Map<PositionId, NodeNotes>,
    private val idOf: (Position) -> PositionId,
) {
    val nodeCount: Int get() = nodes.size

    val edgeCount: Int get() = outgoing.values.sumOf { it.size }

    val allNodes: Collection<RepertoireNode> get() = nodes.values

    fun node(id: PositionId): RepertoireNode? = nodes[id]

    /** Every move of the repertoire, grouped by origin in the order they were added. */
    val allMoves: List<RepertoireEdge> get() = outgoing.values.flatten()

    /** What is written on the position [id]; empty if nothing. */
    fun notes(id: PositionId): NodeNotes = notes[id] ?: NodeNotes()

    /** The repertoire with [value] written on the position [id]; empty notes erase the entry. */
    fun withNotes(
        id: PositionId,
        value: NodeNotes,
    ): Repertoire {
        require(id in nodes) { "Unknown position $id" }
        val updated = if (value.isEmpty) notes - id else notes + (id to value)
        return Repertoire(side, root, nodes, outgoing, updated, idOf)
    }

    /** Moves played from [id], in the order they were added. */
    fun movesFrom(id: PositionId): List<RepertoireEdge> = outgoing[id].orEmpty()

    /** Plays [move] from [from] (a position of this repertoire); throws if it is illegal. */
    fun add(
        from: PositionId,
        move: Move,
    ): AddResult {
        val builder = Builder(this)
        val source = requireNotNull(nodes[from]) { "Unknown position $from" }
        if (!source.position.isLegal(move)) {
            throw IllegalMoveException("Illegal move $move in ${source.position.toFen()}")
        }
        val (target, created) = builder.addMove(source, move)
        return AddResult(builder.build(), target, created)
    }

    /**
     * Merges all lines of [game] (main line and variations), without duplicates. The game must
     * start from the repertoire's start position. Moves were already checked by `PgnReader`.
     */
    fun merge(game: Game): MergeResult {
        val builder = Builder(this)
        if (!builder.mergeGame(game)) {
            throw StartMismatchException("The game does not start from the repertoire position")
        }
        return builder.result()
    }

    /**
     * Merges [games] in one pass (one copy of the graph, not one per game). A game that does not start
     * from the repertoire position is left out and its index in [games] is reported; [onGame] is told
     * how many games were handled so far.
     */
    fun mergeAll(
        games: List<Game>,
        onGame: (Int) -> Unit = {},
    ): BatchMerge {
        val builder = Builder(this)
        val mismatched = mutableListOf<Int>()
        games.forEachIndexed { index, game ->
            if (!builder.mergeGame(game)) mismatched += index
            onGame(index + 1)
        }
        return BatchMerge(builder.result(), mismatched)
    }

    /**
     * Removes the move [move] played from [from], then every position that can no longer be reached
     * from the root (with its moves and notes). A position still reached by another move stays.
     */
    fun removeMove(
        from: PositionId,
        move: Move,
    ): Repertoire {
        val moves = outgoing[from].orEmpty()
        if (moves.none { it.move == move }) return this
        val remaining = outgoing + (from to moves.filterNot { it.move == move })
        val reachable = HashSet<PositionId>().apply { add(root) }
        val pending = ArrayDeque(listOf(root))
        while (pending.isNotEmpty()) {
            remaining[pending.removeLast()].orEmpty().forEach { if (reachable.add(it.to)) pending.addLast(it.to) }
        }
        return Repertoire(
            side,
            root,
            nodes.filterKeys { it in reachable },
            remaining.filter { (id, list) -> id in reachable && list.isNotEmpty() },
            notes.filterKeys { it in reachable },
            idOf,
        )
    }

    private class Builder(
        private val origin: Repertoire,
    ) {
        val nodes = LinkedHashMap(origin.nodes)
        private val outgoing = LinkedHashMap(origin.outgoing)
        private val notes = LinkedHashMap(origin.notes)
        var notedPositions = 0
        var addedMoves = 0
        var existingMoves = 0
        var addedPositions = 0

        /** Adds the move if it is new; [reached] is the position after it, computed if absent. */
        fun addMove(
            source: RepertoireNode,
            move: Move,
            reached: Position? = null,
        ): Pair<PositionId, Boolean> {
            val known = outgoing[source.id].orEmpty().firstOrNull { it.move == move }
            if (known != null) {
                existingMoves++
                return known.to to false
            }
            val position = reached ?: source.position.play(move)
            val targetId = origin.idOf(position)
            if (targetId !in nodes) {
                nodes[targetId] = RepertoireNode(targetId, position)
                addedPositions++
            }
            val edge = RepertoireEdge(source.id, targetId, move, source.position.toSan(move))
            outgoing[source.id] = outgoing[source.id].orEmpty() + edge
            addedMoves++
            return targetId to true
        }

        /**
         * Writes what the PGN [comments] say on the position [id]. A text already there is not repeated,
         * a different one is added on a new line; a mark already on the same squares keeps its colour.
         */
        fun addNotes(
            id: PositionId,
            comments: List<String>,
        ) {
            for (raw in comments) {
                val parsed = PgnMarks.parse(raw)
                val current = notes[id] ?: NodeNotes()
                val text =
                    when {
                        parsed.text.isEmpty() -> current.comment
                        current.comment.isEmpty() -> parsed.text
                        parsed.text in current.comment -> current.comment
                        else -> current.comment + "\n" + parsed.text
                    }
                val merged =
                    current.copy(
                        comment = text,
                        arrows =
                            current.arrows + parsed.arrows.filter { a -> current.arrows.none { it.sameSquares(a) } },
                        squares =
                            current.squares +
                                parsed.squares.filter { a -> current.squares.none { it.square == a.square } },
                    )
                if (merged != current) {
                    notes[id] = merged
                    notedPositions++
                }
            }
        }

        /** Merges every line of [game]; false (and nothing changed) if it starts elsewhere than the root. */
        fun mergeGame(game: Game): Boolean {
            if (origin.idOf(game.startPosition) != origin.root) return false
            val pending = ArrayDeque<Pair<PositionId, MoveNode>>()
            game.moves.asReversed().forEach { pending.addLast(origin.root to it) }
            while (pending.isNotEmpty()) {
                val (fromId, moveNode) = pending.removeLast()
                val source = nodes.getValue(fromId)
                val (target, _) = addMove(source, moveNode.move, moveNode.position)
                addNotes(target, moveNode.commentsAfter)
                moveNode.children.asReversed().forEach { pending.addLast(target to it) }
            }
            return true
        }

        fun result() = MergeResult(build(), addedMoves, existingMoves, addedPositions, notedPositions)

        fun build(): Repertoire = Repertoire(origin.side, origin.root, nodes, outgoing, notes, origin.idOf)
    }

    companion object {
        /**
         * Rebuilds a stored repertoire. Throws [IllegalArgumentException] if the root is missing or a move
         * joins unknown positions; moves are not replayed (they were validated before being stored).
         */
        fun restore(
            side: Side,
            root: PositionId,
            nodes: List<RepertoireNode>,
            moves: List<RepertoireEdge>,
            notes: Map<PositionId, NodeNotes>,
        ): Repertoire {
            val byId = nodes.associateBy { it.id }
            require(root in byId) { "The root position is missing" }
            moves.forEach { require(it.from in byId && it.to in byId) { "Move ${it.move} joins an unknown position" } }
            return Repertoire(
                side,
                root,
                byId,
                moves.groupBy { it.from },
                notes.filterValues { !it.isEmpty }.filterKeys { it in byId },
                PositionId::of,
            )
        }

        /** An empty repertoire for [side], starting from [start] (the initial position by default). */
        fun empty(
            side: Side,
            start: Position = Position.START,
        ): Repertoire = empty(side, start, PositionId::of)

        internal fun empty(
            side: Side,
            start: Position,
            idOf: (Position) -> PositionId,
        ): Repertoire {
            val id = idOf(start)
            return Repertoire(side, id, mapOf(id to RepertoireNode(id, start)), emptyMap(), emptyMap(), idOf)
        }
    }
}

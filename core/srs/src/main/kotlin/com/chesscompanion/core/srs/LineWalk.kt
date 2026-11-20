package com.chesscompanion.core.srs

import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.repertoire.RepertoireEdge
import kotlin.random.Random

/**
 * One line of a drill, played from the start of the repertoire (S8, "révision par lignes"): the player's moves are
 * asked, the opponent's replies are played by the app. The line follows [plan] (the path to the position to review)
 * as long as the player does; once the plan is done or left, the opponent's replies are drawn among the repertoire's.
 * Pure: it only walks the repertoire graph.
 */
class LineWalk(
    private val repertoire: Repertoire,
    private val plan: List<RepertoireEdge>,
    private val random: Random = Random.Default,
) {
    private val edges = mutableListOf<RepertoireEdge>()
    private var onPlan = true

    /** Moves played so far, from the repertoire root. */
    val played: List<RepertoireEdge> get() = edges.toList()

    /** Where the line stands. */
    var position: PositionId = repertoire.root
        private set

    /** Repertoire moves from here. */
    val moves: List<RepertoireEdge> get() = repertoire.movesFrom(position)

    /** The line has no move left (a leaf of the repertoire) or is too long to be a real line. */
    val isOver: Boolean get() = moves.isEmpty() || edges.size >= MAX_PLIES

    /** The player is to move. */
    val isPlayerTurn: Boolean
        get() = repertoire.node(position)?.position?.sideToMove == repertoire.side

    /** Plays [edge], which must start from the current position. */
    fun advance(edge: RepertoireEdge) {
        require(edge.from == position) { "The move does not start from the current position" }
        onPlan = onPlan && plan.getOrNull(edges.size) == edge
        edges += edge
        position = edge.to
    }

    /** The opponent's reply: the plan's while on it, else one of the repertoire's at random; null at the end. */
    fun opponentReply(): RepertoireEdge? =
        planEdge() ?: moves.takeIf { it.isNotEmpty() }?.let { it[random.nextInt(it.size)] }

    /** The player's move to show or play among [expected]: the plan's if it is one of them, else the first. */
    fun preferred(expected: List<RepertoireEdge>): RepertoireEdge? =
        expected.firstOrNull { it == planEdge() } ?: expected.firstOrNull()

    private fun planEdge(): RepertoireEdge? = if (onPlan) plan.getOrNull(edges.size) else null

    private companion object {
        const val MAX_PLIES = 200
    }
}

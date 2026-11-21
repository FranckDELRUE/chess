package com.chesscompanion.core.srs

import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.repertoire.RepertoireEdge
import com.chesscompanion.core.chess.repertoire.RepertoireNode
import java.time.Duration
import java.time.Instant

/**
 * One position of an opening lesson (S8b-2): the player is to move and the repertoire has an answer.
 *
 * @property path moves from the repertoire root to [node], along the lesson's order.
 * @property expected every repertoire move from [node].
 * @property learned the position already has a review card (it was learned or reviewed).
 */
data class LessonStep(
    val node: RepertoireNode,
    val path: List<RepertoireEdge>,
    val expected: List<RepertoireEdge>,
    val learned: Boolean,
) {
    val id: PositionId get() = node.id
}

/** The steps of a lesson in teaching order, and how many are learned. */
object LessonPlanner {
    /**
     * The player's positions of [repertoire] in lesson order: depth first, the main line (first move of each position)
     * before its variations, so that a line is followed to its end before the next one starts. A position reached
     * by several lines is taught once, the first time.
     */
    fun plan(
        repertoire: Repertoire,
        cards: Map<PositionId, FsrsCard>,
    ): List<LessonStep> {
        val steps = mutableListOf<LessonStep>()
        val seen = HashSet<PositionId>()

        fun visit(
            id: PositionId,
            path: List<RepertoireEdge>,
        ) {
            if (!seen.add(id)) return
            val node = repertoire.node(id) ?: return
            val moves = repertoire.movesFrom(id)
            if (moves.isNotEmpty() && node.position.sideToMove == repertoire.side) {
                steps += LessonStep(node, path, moves, learned = cards[id]?.isNew == false)
            }
            moves.forEach { visit(it.to, path + it) }
        }
        visit(repertoire.root, emptyList())
        return steps
    }
}

/**
 * The memory state of a position the player has just learned: a first success (Good) that comes back the next day,
 * so that the first real recall is soon. Same rule as the drill's learning step.
 */
fun learnedCard(
    now: Instant,
    fsrs: Fsrs = Fsrs(),
): FsrsCard = fsrs.review(FsrsCard(), Rating.Good, now).copy(due = now.plus(Duration.ofDays(1)))

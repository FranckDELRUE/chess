package com.chesscompanion.core.srs

import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.repertoire.RepertoireEdge
import com.chesscompanion.core.chess.repertoire.RepertoireNode
import java.time.Instant
import java.time.ZoneId

/** Daily limits of a review queue. */
data class QueueLimits(
    val maxReviews: Int = DEFAULT_MAX_REVIEWS,
    val maxNew: Int = DEFAULT_MAX_NEW,
) {
    init {
        require(maxReviews >= 0 && maxNew >= 0) { "Limits cannot be negative" }
    }

    companion object {
        const val DEFAULT_MAX_REVIEWS = 100
        const val DEFAULT_MAX_NEW = 10
    }
}

/**
 * One position to drill: the player must find a move of [expected] from [node].
 *
 * @property card the stored memory state, or null for a position never reviewed (new).
 * @property path moves from the repertoire root to [node] (the shortest one), to replay the line before asking.
 * @property expected every repertoire move from [node]; any of them is a correct answer.
 */
data class ReviewItem(
    val node: RepertoireNode,
    val card: FsrsCard?,
    val path: List<RepertoireEdge>,
    val expected: List<RepertoireEdge>,
) {
    val id: PositionId get() = node.id

    val isNew: Boolean get() = card == null || card.isNew
}

/**
 * The day's work: [items] are the reviews due first (most overdue first), then new positions in repertoire
 * order (trunk before branches). [totalDue] and [totalNew] count everything available before the limits.
 */
data class ReviewQueue(
    val items: List<ReviewItem>,
    val totalDue: Int,
    val totalNew: Int,
) {
    val dueCount: Int get() = items.count { !it.isNew }
    val newCount: Int get() = items.count { it.isNew }
}

/** The last instant of the local day of [now]: a card due at any time today is due today. */
fun endOfDay(
    now: Instant,
    zone: ZoneId,
): Instant =
    now
        .atZone(zone)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(zone)
        .toInstant()
        .minusMillis(1)

/** Builds the daily queue of a repertoire. Pure: the caller supplies the stored [cards] and the clock. */
object ReviewQueueBuilder {
    /**
     * A position is drilled when the player is to move in it and the repertoire has an answer: one card per
     * such position, however many paths reach it (ADR 0012). Opponent positions are not asked.
     */
    fun build(
        repertoire: Repertoire,
        cards: Map<PositionId, FsrsCard>,
        now: Instant,
        zone: ZoneId,
        limits: QueueLimits = QueueLimits(),
    ): ReviewQueue {
        val limit = endOfDay(now, zone)
        val decisions = decisionItems(repertoire, cards)
        val due =
            decisions
                .filter { item -> item.card?.takeUnless { it.isNew }?.isDue(limit) == true }
                .sortedBy { it.card?.due }
        val new = decisions.filter { it.isNew }
        return ReviewQueue(
            items = due.take(limits.maxReviews) + new.take(limits.maxNew),
            totalDue = due.size,
            totalNew = new.size,
        )
    }

    /** Decision positions in breadth-first order from the root, each with its shortest path. */
    private fun decisionItems(
        repertoire: Repertoire,
        cards: Map<PositionId, FsrsCard>,
    ): List<ReviewItem> {
        val parents = mutableMapOf<PositionId, RepertoireEdge?>(repertoire.root to null)
        val order = ArrayDeque<PositionId>().apply { add(repertoire.root) }
        val items = mutableListOf<ReviewItem>()
        while (order.isNotEmpty()) {
            val id = order.removeFirst()
            val node = repertoire.node(id) ?: continue
            val moves = repertoire.movesFrom(id)
            if (moves.isNotEmpty() && node.position.sideToMove == repertoire.side) {
                items += ReviewItem(node, cards[id], pathTo(id, parents), moves)
            }
            for (edge in moves) {
                if (edge.to !in parents) {
                    parents[edge.to] = edge
                    order.add(edge.to)
                }
            }
        }
        return items
    }

    private fun pathTo(
        id: PositionId,
        parents: Map<PositionId, RepertoireEdge?>,
    ): List<RepertoireEdge> {
        val path = ArrayDeque<RepertoireEdge>()
        var edge = parents[id]
        while (edge != null) {
            path.addFirst(edge)
            edge = parents[edge.from]
        }
        return path
    }
}

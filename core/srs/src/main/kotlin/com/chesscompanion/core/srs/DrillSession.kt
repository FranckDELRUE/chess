package com.chesscompanion.core.srs

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.repertoire.RepertoireEdge
import java.time.Duration
import java.time.Instant

/** How the played move compares with the repertoire. */
sealed interface DrillOutcome {
    /** The move is in the repertoire; [edge] is the matching repertoire move. */
    data class Correct(
        val edge: RepertoireEdge,
    ) : DrillOutcome

    /** The move is not in the repertoire: the player deviated from [expected]. */
    data class Deviation(
        val played: Move,
        val expected: List<RepertoireEdge>,
    ) : DrillOutcome
}

/**
 * Result of one answer. [updatedCard] is the new memory state to store, or null when the answer was a
 * practice retry after a miss (it does not change the schedule). [rating] is null in that case too.
 */
data class DrillResult(
    val item: ReviewItem,
    val outcome: DrillOutcome,
    val rating: Rating?,
    val updatedCard: FsrsCard?,
) {
    val isCorrect: Boolean get() = outcome is DrillOutcome.Correct
}

/**
 * A drill over a [ReviewQueue] (S8-3). Pure state machine: it never touches storage or the clock; the caller
 * stores each non-null [DrillResult.updatedCard]. Rating rule: the right move without help is [Rating.Good],
 * with the hint [Rating.Hard], a deviation [Rating.Again]. A missed position comes back at the end of the
 * session until it is found, but only the first answer is graded (a miss is seen again the next day).
 */
class DrillSession(
    items: List<ReviewItem>,
    private val fsrs: Fsrs = Fsrs(),
) {
    private class Pending(
        val item: ReviewItem,
        val graded: Boolean,
    )

    private val pending = ArrayDeque(items.map { Pending(it, graded = true) })

    /** The position to answer now, or null when the session is over. */
    val current: ReviewItem? get() = pending.firstOrNull()?.item

    /** Positions left, retries included. */
    val remaining: Int get() = pending.size

    val isFinished: Boolean get() = pending.isEmpty()

    /** True when the current position is a practice retry after a miss (its answer is not graded). */
    val isRetry: Boolean get() = pending.firstOrNull()?.graded == false

    /**
     * True when the current position is new: it is learned (the answer is shown, [learn]) before it is ever
     * asked. A retry is never a learning step.
     */
    val isLearning: Boolean get() = pending.firstOrNull()?.let { it.graded && it.item.isNew } == true

    /** The move to show as a hint for the current position: the first repertoire move. */
    val hint: RepertoireEdge? get() = current?.expected?.firstOrNull()

    /** Answers the current position with [played]; [hintUsed] tells if the hint was shown first. */
    fun answer(
        played: Move,
        now: Instant,
        hintUsed: Boolean = false,
    ): DrillResult {
        val entry = checkNotNull(pending.removeFirstOrNull()) { "The drill is finished" }
        val item = entry.item
        val match = item.expected.firstOrNull { it.move == played }
        val outcome = if (match != null) DrillOutcome.Correct(match) else DrillOutcome.Deviation(played, item.expected)
        if (match == null) pending.addLast(Pending(item, graded = false))
        if (!entry.graded) return DrillResult(item, outcome, rating = null, updatedCard = null)
        val rating =
            when {
                match == null -> Rating.Again
                hintUsed -> Rating.Hard
                else -> Rating.Good
            }
        return DrillResult(item, outcome, rating, fsrs.review(item.card ?: FsrsCard(), rating, now))
    }

    /**
     * Learning step of a new position: the player plays the repertoire move that was shown. A right move ends the
     * step and creates the card, rated [Rating.Good] but due the next day, so that the first real recall comes
     * soon; any other move changes nothing (nothing is graded while learning).
     */
    fun learn(
        played: Move,
        now: Instant,
    ): LearnResult {
        val entry = checkNotNull(pending.firstOrNull()) { "The drill is finished" }
        check(entry.graded && entry.item.isNew) { "The current position is not being learned" }
        val edge =
            entry.item.expected.firstOrNull { it.move == played } ?: return LearnResult.NotTheMove(entry.item.expected)
        pending.removeFirst()
        val card = fsrs.review(FsrsCard(), Rating.Good, now).copy(due = now.plus(Duration.ofDays(1)))
        return LearnResult.Learned(entry.item, edge, card)
    }
}

/** Outcome of [DrillSession.learn]. */
sealed interface LearnResult {
    /** The move was a repertoire move: [card] is the new memory state to store. */
    data class Learned(
        val item: ReviewItem,
        val edge: RepertoireEdge,
        val card: FsrsCard,
    ) : LearnResult

    /** The move is not one to learn; [expected] are the repertoire moves. */
    data class NotTheMove(
        val expected: List<RepertoireEdge>,
    ) : LearnResult
}

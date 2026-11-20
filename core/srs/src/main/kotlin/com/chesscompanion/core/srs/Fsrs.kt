package com.chesscompanion.core.srs

import java.time.Duration
import java.time.Instant
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * FSRS-5 scheduler (Free Spaced Repetition Scheduler, MIT). Pure and deterministic: no fuzz, no clock.
 * Intervals are whole days, at least one: a card rated [Rating.Again] comes back the next day.
 * Weight indices (w[4], w[17]...) follow the published FSRS-5 formulas and are not worth naming one by one.
 */
@Suppress("TooManyFunctions", "MagicNumber")
class Fsrs(
    private val parameters: FsrsParameters = FsrsParameters(),
) {
    private val w = parameters.weights

    /** Probability of recalling [card] at [now]; 0 for a new card. */
    fun retrievability(
        card: FsrsCard,
        now: Instant,
    ): Double {
        val stability = card.stability ?: return 0.0
        val last = card.lastReview ?: return 0.0
        return retrievability(elapsedDays(last, now), stability)
    }

    /** Applies a review of [card] at [now] and returns the updated card. */
    fun review(
        card: FsrsCard,
        rating: Rating,
        now: Instant,
    ): FsrsCard {
        val stability = card.stability
        val difficulty = card.difficulty
        val last = card.lastReview
        val (newStability, newDifficulty) =
            if (stability == null || difficulty == null || last == null) {
                initialStability(rating) to initialDifficulty(rating)
            } else {
                val elapsed = elapsedDays(last, now)
                nextStability(stability, difficulty, elapsed, rating) to nextDifficulty(difficulty, rating)
            }
        // A failed line is always seen again the next day, whatever the memory model predicts.
        val interval = if (rating == Rating.Again) 1 else nextIntervalDays(newStability)
        val lapsed = !card.isNew && rating == Rating.Again
        return FsrsCard(
            stability = newStability,
            difficulty = newDifficulty,
            due = now.plus(Duration.ofDays(interval.toLong())),
            lastReview = now,
            reps = card.reps + 1,
            lapses = card.lapses + if (lapsed) 1 else 0,
        )
    }

    /** Interval in whole days to apply to a memory of the given [stability]. */
    fun nextIntervalDays(stability: Double): Int {
        val raw = stability / FACTOR * (parameters.desiredRetention.pow(1.0 / DECAY) - 1.0)
        return raw.roundToInt().coerceIn(1, parameters.maximumIntervalDays)
    }

    private fun retrievability(
        elapsedDays: Double,
        stability: Double,
    ): Double = (1.0 + FACTOR * elapsedDays / stability).pow(DECAY)

    private fun initialStability(rating: Rating): Double = max(w[rating.grade - 1], MIN_STABILITY)

    private fun rawInitialDifficulty(rating: Rating): Double = w[4] - exp(w[5] * (rating.grade - 1)) + 1.0

    private fun initialDifficulty(rating: Rating): Double =
        rawInitialDifficulty(rating).coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)

    private fun nextDifficulty(
        difficulty: Double,
        rating: Rating,
    ): Double {
        val delta = -w[6] * (rating.grade - Rating.Good.grade)
        val damped = difficulty + delta * (MAX_DIFFICULTY - difficulty) / (MAX_DIFFICULTY - MIN_DIFFICULTY)
        val reverted = w[7] * rawInitialDifficulty(Rating.Easy) + (1.0 - w[7]) * damped
        return reverted.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    private fun nextStability(
        stability: Double,
        difficulty: Double,
        elapsedDays: Double,
        rating: Rating,
    ): Double {
        if (elapsedDays < 1.0) return sameDayStability(stability, rating)
        val r = retrievability(elapsedDays, stability)
        return if (rating == Rating.Again) {
            forgetStability(stability, difficulty, r)
        } else {
            recallStability(stability, difficulty, r, rating)
        }
    }

    private fun sameDayStability(
        stability: Double,
        rating: Rating,
    ): Double {
        val next = stability * exp(w[17] * (rating.grade - Rating.Good.grade + w[18]))
        // A passing same-day review must never shrink the memory.
        return max(if (rating.grade >= 3) max(next, stability) else next, MIN_STABILITY)
    }

    private fun recallStability(
        stability: Double,
        difficulty: Double,
        r: Double,
        rating: Rating,
    ): Double {
        val hardPenalty = if (rating == Rating.Hard) w[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) w[16] else 1.0
        val growth =
            exp(w[8]) * (MAX_DIFFICULTY + 1.0 - difficulty) * stability.pow(-w[9]) *
                (exp(w[10] * (1.0 - r)) - 1.0) * hardPenalty * easyBonus
        return max(stability * (1.0 + growth), MIN_STABILITY)
    }

    private fun forgetStability(
        stability: Double,
        difficulty: Double,
        r: Double,
    ): Double {
        val next = w[11] * difficulty.pow(-w[12]) * ((stability + 1.0).pow(w[13]) - 1.0) * exp(w[14] * (1.0 - r))
        return max(min(next, stability), MIN_STABILITY)
    }

    private fun elapsedDays(
        from: Instant,
        to: Instant,
    ): Double = max(0.0, Duration.between(from, to).toMillis() / MILLIS_PER_DAY)

    private companion object {
        const val DECAY = -0.5
        const val FACTOR = 19.0 / 81.0
        const val MIN_STABILITY = 0.01
        const val MIN_DIFFICULTY = 1.0
        const val MAX_DIFFICULTY = 10.0
        const val MILLIS_PER_DAY = 86_400_000.0
    }
}

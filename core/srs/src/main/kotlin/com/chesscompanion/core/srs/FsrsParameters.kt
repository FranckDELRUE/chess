package com.chesscompanion.core.srs

/**
 * FSRS-5 parameters. [weights] holds the 19 model weights (default: the published defaults).
 *
 * @property desiredRetention target probability of recall at the due date (0.7 to 0.99).
 * @property maximumIntervalDays upper bound for an interval.
 */
data class FsrsParameters(
    val weights: List<Double> = DEFAULT_WEIGHTS,
    val desiredRetention: Double = 0.9,
    val maximumIntervalDays: Int = 36_500,
) {
    init {
        require(weights.size == WEIGHT_COUNT) { "FSRS-5 needs $WEIGHT_COUNT weights, got ${weights.size}" }
        require(desiredRetention in MIN_RETENTION..MAX_RETENTION) { "desiredRetention out of range: $desiredRetention" }
        require(maximumIntervalDays >= 1) { "maximumIntervalDays must be positive" }
    }

    companion object {
        const val WEIGHT_COUNT = 19
        const val MIN_RETENTION = 0.7
        const val MAX_RETENTION = 0.99

        @Suppress("MagicNumber")
        val DEFAULT_WEIGHTS: List<Double> =
            listOf(
                0.40255,
                1.18385,
                3.173,
                15.69105,
                7.1949,
                0.5345,
                1.4604,
                0.0046,
                1.54575,
                0.1192,
                1.01925,
                1.9395,
                0.11,
                0.29605,
                2.2698,
                0.2315,
                2.9898,
                0.51655,
                0.6621,
            )
    }
}

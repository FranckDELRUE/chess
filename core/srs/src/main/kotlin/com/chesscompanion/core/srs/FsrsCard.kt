package com.chesscompanion.core.srs

import java.time.Instant

/**
 * Memory state of one review card. A card never reviewed has no [stability] and no [difficulty].
 *
 * @property stability days for retrievability to fall from 100 % to 90 %; null while new.
 * @property difficulty from 1 (easy) to 10 (hard); null while new.
 * @property due when the card should next be reviewed; null while new (always available).
 * @property lastReview instant of the last review; null while new.
 * @property reps number of reviews done.
 * @property lapses number of times the card was rated [Rating.Again] after being learned.
 */
data class FsrsCard(
    val stability: Double? = null,
    val difficulty: Double? = null,
    val due: Instant? = null,
    val lastReview: Instant? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
) {
    val isNew: Boolean get() = stability == null

    fun isDue(now: Instant): Boolean = due == null || !due.isAfter(now)
}

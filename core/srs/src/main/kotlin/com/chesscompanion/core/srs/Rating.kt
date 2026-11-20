package com.chesscompanion.core.srs

/** Self-assessed outcome of a review, in FSRS order (grade 1 to 4). */
enum class Rating {
    Again,
    Hard,
    Good,
    Easy,
    ;

    /** FSRS grade: 1 for [Again] up to 4 for [Easy]. */
    val grade: Int get() = ordinal + 1
}

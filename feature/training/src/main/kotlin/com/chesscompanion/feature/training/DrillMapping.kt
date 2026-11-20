package com.chesscompanion.feature.training

import com.chesscompanion.core.chess.PieceLetters
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.repertoire.Source
import com.chesscompanion.core.chess.toSan
import com.chesscompanion.core.srs.DrillOutcome
import com.chesscompanion.core.srs.DrillResult
import com.chesscompanion.core.srs.LearnResult
import com.chesscompanion.core.srs.Rating
import com.chesscompanion.core.srs.ReviewItem
import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.Arrow
import com.chesscompanion.core.ui.board.BoardAnnotations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Changes the feedback being shown, if any (the evaluations arrive after the answer). */
internal fun MutableStateFlow<DrillUiState>.updateFeedback(change: (DrillFeedback) -> DrillFeedback) =
    update { state -> state.feedback?.let { state.copy(feedback = change(it)) } ?: state }

/** `Livre · chapitre · p. 12`, from the source of a position; null when it has none. */
internal fun Source.toText(): String = listOfNotNull(book, chapter, page?.let { "p. $it" }).joinToString(" · ")

/** One more position learned. */
internal fun DrillSummary.learnedOne(): DrillSummary = copy(learned = learned + 1)

/** Square of the king in check in this position, or null. */
internal fun Position.checkSquare(): Square? = if (isCheck) board.kingSquare(sideToMove) else null

/** `#` when White gives mate, `-#` when Black does; [mover] is the side that just moved. */
internal fun matedText(mover: Side) = if (mover == Side.WHITE) "#" else "-#"

/** These counts after one more answer; a retry ([rating] null) is not counted. */
internal fun DrillSummary.plus(rating: Rating?): DrillSummary =
    if (rating == null) {
        this
    } else {
        val missed = rating == Rating.Again
        copy(
            graded = graded + 1,
            correct = correct + if (missed) 0 else 1,
            missed = missed.let { if (it) this.missed + 1 else this.missed },
        )
    }

internal fun feedbackOf(
    item: ReviewItem,
    result: DrillResult,
    repertoire: Repertoire,
): DrillFeedback {
    val position = item.node.position
    val played =
        when (val outcome = result.outcome) {
            is DrillOutcome.Correct -> outcome.edge.move
            is DrillOutcome.Deviation -> outcome.played
        }
    return DrillFeedback(
        correct = result.isCorrect,
        retry = result.rating == null,
        playedSan = position.toSan(played, PieceLetters.FRENCH),
        expectedSans = item.expected.map { position.toSan(it.move, PieceLetters.FRENCH) },
        line = lineText(repertoire, item.path + item.expected.first()),
        evaluationDone = result.isCorrect,
    )
}

internal fun learnedFeedback(
    item: ReviewItem,
    result: LearnResult.Learned,
    repertoire: Repertoire,
): DrillFeedback {
    val position = item.node.position
    val san = position.toSan(result.edge.move, PieceLetters.FRENCH)
    return DrillFeedback(
        correct = true,
        retry = false,
        learned = true,
        playedSan = san,
        expectedSans = item.expected.map { position.toSan(it.move, PieceLetters.FRENCH) },
        line = lineText(repertoire, item.path + result.edge),
        evaluationDone = true,
    )
}

/** Green arrows for the repertoire moves shown while learning. */
internal fun expectedArrows(item: ReviewItem): BoardAnnotations =
    BoardAnnotations(arrows = item.expected.associate { Arrow(it.move.from, it.move.to) to AnnotationColor.GREEN })

internal fun DrillStatus.takesMoves(): Boolean = this == DrillStatus.ASKING || this == DrillStatus.LEARNING

/** Green arrows for the repertoire moves, a red one for the move played when it deviated. */
internal fun annotationsOf(
    item: ReviewItem,
    result: DrillResult,
): BoardAnnotations {
    val arrows = mutableMapOf<Arrow, AnnotationColor>()
    item.expected.forEach { arrows[Arrow(it.move.from, it.move.to)] = AnnotationColor.GREEN }
    (result.outcome as? DrillOutcome.Deviation)?.let {
        arrows[Arrow(it.played.from, it.played.to)] = AnnotationColor.RED
    }
    return BoardAnnotations(arrows = arrows)
}

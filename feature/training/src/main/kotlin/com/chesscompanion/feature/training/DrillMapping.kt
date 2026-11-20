package com.chesscompanion.feature.training

import com.chesscompanion.core.chess.PieceLetters
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.toSan
import com.chesscompanion.core.srs.DrillOutcome
import com.chesscompanion.core.srs.DrillResult
import com.chesscompanion.core.srs.Rating
import com.chesscompanion.core.srs.ReviewItem
import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.Arrow
import com.chesscompanion.core.ui.board.BoardAnnotations

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

package com.chesscompanion.feature.training

import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.board.BoardAnnotations
import com.chesscompanion.core.ui.board.BoardHighlights

/**
 * [LEARNING]: a new position, the repertoire move is shown and the player plays it; [ASKING]: a review question;
 * [REPLYING]: the app plays the opponent's move.
 */
enum class DrillStatus { LOADING, NOT_FOUND, REPLYING, LEARNING, ASKING, FEEDBACK, FINISHED }

/**
 * What the player is told after an answer (facts only: the explanation by the teacher comes in S4).
 * Evaluations are Stockfish's, as White sees them; null while pending ([evaluationDone] false) or unavailable.
 */
data class DrillFeedback(
    val correct: Boolean,
    /** An ungraded practice retry after a miss: the schedule was not changed. */
    val retry: Boolean,
    /** The answer completed the learning of a new position (it comes back tomorrow). */
    val learned: Boolean = false,
    val playedSan: String,
    val expectedSans: List<String>,
    /** The repertoire line up to the expected move, e.g. `1. d4 d5 2. c4`. */
    val line: String,
    val playedEvaluation: String? = null,
    val expectedEvaluation: String? = null,
    val evaluationDone: Boolean = false,
)

/** End of session counts: positions graded, found first time, and missed. */
data class DrillSummary(
    val graded: Int = 0,
    val correct: Int = 0,
    val missed: Int = 0,
    val learned: Int = 0,
)

data class DrillUiState(
    val status: DrillStatus = DrillStatus.LOADING,
    val name: String = "",
    val board: Board = Position.START.board,
    val flipped: Boolean = false,
    val highlights: BoardHighlights = BoardHighlights(),
    val annotations: BoardAnnotations = BoardAnnotations.EMPTY,
    val promotionSide: Side? = null,
    /** The line played so far, e.g. `1. d4 d5`; empty at the start of the repertoire. */
    val lineText: String = "",
    val toMove: Side = Side.WHITE,
    val isNewPosition: Boolean = false,
    val isRetry: Boolean = false,
    /** A known position met on the line but not due: asked for practice, nothing is graded. */
    val isPractice: Boolean = false,
    val remaining: Int = 0,
    val hint: String? = null,
    /** Learning a new position: the repertoire moves to play (SAN), what the author wrote, and the last try missed. */
    val learnMoves: List<String> = emptyList(),
    val learnComment: String = "",
    val learnSource: String? = null,
    val learnMiss: Boolean = false,
    val feedback: DrillFeedback? = null,
    val summary: DrillSummary = DrillSummary(),
    /** Positions waiting beyond today's limits (only meaningful once finished). */
    val leftDue: Int = 0,
    val leftNew: Int = 0,
    /** Nothing was queued at all: no answer in the repertoire yet, or everything is done for today. */
    val nothingToReview: Boolean = false,
)

sealed interface DrillAction {
    data class SquareTap(
        val square: Square,
    ) : DrillAction

    data class DragStart(
        val square: Square,
    ) : DrillAction

    data class Drop(
        val from: Square,
        val to: Square,
    ) : DrillAction

    data class Promote(
        val type: PieceType,
    ) : DrillAction

    data object CancelPromotion : DrillAction

    data object ShowHint : DrillAction

    /** Leaves the feedback and goes to the next position. */
    data object Continue : DrillAction
}

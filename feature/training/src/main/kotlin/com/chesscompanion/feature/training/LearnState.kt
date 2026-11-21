package com.chesscompanion.feature.training

import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.data.explorer.ExplorerProblem
import com.chesscompanion.core.ui.board.BoardAnnotations
import com.chesscompanion.core.ui.board.BoardHighlights

enum class LearnStatus { LOADING, NOT_FOUND, EMPTY, LESSON }

/**
 * What is known about one repertoire move of the position: Stockfish's evaluation (as White sees it) and how the
 * Lichess database treats it. Facts only; `null` while pending or unavailable.
 */
data class MoveFact(
    val san: String,
    val evaluation: String? = null,
    /** Games that played this move, and their share of the games of the position (0 when the move is never played). */
    val games: Long? = null,
    val percent: Int? = null,
    /** Results of the games that played it, in percent: White wins, draws, Black wins. */
    val whitePercent: Int? = null,
    val drawPercent: Int? = null,
    val blackPercent: Int? = null,
)

data class LearnUiState(
    val status: LearnStatus = LearnStatus.LOADING,
    val name: String = "",
    /** Position of the lesson (from 0), how many there are and how many are learned. */
    val index: Int = 0,
    val total: Int = 0,
    val learned: Int = 0,
    val board: Board = Position.START.board,
    val flipped: Boolean = false,
    val highlights: BoardHighlights = BoardHighlights(),
    val annotations: BoardAnnotations = BoardAnnotations.EMPTY,
    val promotionSide: Side? = null,
    val lineText: String = "",
    val toMove: Side = Side.WHITE,
    val facts: List<MoveFact> = emptyList(),
    val evaluationsDone: Boolean = false,
    /** The explorer was asked: [explorerLoaded] when it answered, else [explorerProblem] says why not. */
    val explorerLoaded: Boolean = false,
    val explorerProblem: ExplorerProblem? = null,
    /** What the author wrote on the position (comment and source), empty if nothing. */
    val comment: String = "",
    val source: String? = null,
    /** The author's idea, plan and trap for the position (S8b-3), empty if not written. */
    val idea: String = "",
    val plan: String = "",
    val trap: String = "",
    /** The position is learned (before this visit, or by the move just played). */
    val stepLearned: Boolean = false,
    /** The move just played learned it. */
    val justLearned: Boolean = false,
    /** The last move tried was not a repertoire move. */
    val miss: Boolean = false,
)

sealed interface LearnAction {
    /** A board gesture, handled like the drill's. */
    data class Gesture(
        val action: DrillAction,
    ) : LearnAction

    data object Next : LearnAction

    data object Previous : LearnAction
}

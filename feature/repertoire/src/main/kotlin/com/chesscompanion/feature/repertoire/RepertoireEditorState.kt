package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.Board
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.BoardAnnotations
import com.chesscompanion.core.ui.board.BoardHighlights

enum class EditorStatus { LOADING, READY, NOT_FOUND }

/** A move that can follow the position shown: [uci] to play it, [label] in French SAN. */
data class Continuation(
    val uci: String,
    val label: String,
)

/** The three fields of the source of a position (book, chapter, page). */
enum class SourceField { BOOK, CHAPTER, PAGE }

/** The three questions that explain a move (S8b-3): what it is for, what comes next, what to watch out for. */
enum class InsightField { IDEA, PLAN, TRAP }

data class RepertoireEditorUiState(
    val status: EditorStatus = EditorStatus.LOADING,
    val name: String = "",
    val side: Side = Side.WHITE,
    val board: Board = Position.START.board,
    val flipped: Boolean = false,
    val highlights: BoardHighlights = BoardHighlights(),
    /** Side whose pawn waits for a promotion piece, null when no choice is pending. */
    val promotionSide: Side? = null,
    /** Moves from the start to the position shown, e.g. `1. e4 e5 2. Cf3`. */
    val lineText: String = "",
    val canGoBack: Boolean = false,
    val continuations: List<Continuation> = emptyList(),
    val comment: String = "",
    val idea: String = "",
    val plan: String = "",
    val trap: String = "",
    val sourceBook: String = "",
    val sourceChapter: String = "",
    val sourcePage: String = "",
    /** Marks of the position shown, which are its notes. */
    val annotations: BoardAnnotations = BoardAnnotations.EMPTY,
    /** "Annoter" mode: taps and drags mark the board instead of playing. */
    val annotating: Boolean = false,
    val annotationColor: AnnotationColor = AnnotationColor.GREEN,
    /** Deleting the last move asks for a confirmation first. */
    val confirmDelete: Boolean = false,
    val moveCount: Int = 0,
    /** The PGN import dialog (S7-5). */
    val import: ImportUiState = ImportUiState.Closed,
)

sealed interface EditorAction {
    sealed interface Board : EditorAction

    data class SquareTap(
        val square: Square,
    ) : Board

    data class DragStart(
        val square: Square,
    ) : Board

    data class Drop(
        val from: Square,
        val to: Square,
    ) : Board

    data class Promote(
        val type: PieceType,
    ) : EditorAction

    data object CancelPromotion : EditorAction

    data object Back : EditorAction

    data object ToStart : EditorAction

    data class Follow(
        val uci: String,
    ) : EditorAction

    /** Plays [uci] from the position shown: follows it if the repertoire has it, adds it otherwise. */
    data class PlayUci(
        val uci: String,
    ) : EditorAction

    /** An action of the explorer panel (level, token). */
    data class Explorer(
        val action: ExplorerAction,
    ) : EditorAction

    data object Flip : EditorAction

    data object ToggleAnnotate : EditorAction

    data class PickAnnotationColor(
        val color: AnnotationColor,
    ) : EditorAction

    data object ClearAnnotations : EditorAction

    data class EditComment(
        val text: String,
    ) : EditorAction

    data class EditInsight(
        val field: InsightField,
        val text: String,
    ) : EditorAction

    data class EditSource(
        val field: SourceField,
        val text: String,
    ) : EditorAction

    /** Asks to delete the last move (and what only it led to). */
    data object AskDelete : EditorAction

    data object ConfirmDelete : EditorAction

    data object DismissDelete : EditorAction

    /** Opens the PGN import dialog. */
    data object OpenImport : EditorAction

    data object CloseImport : EditorAction

    /** Merges the games of [text] into the repertoire. */
    data class RunImport(
        val text: String,
    ) : EditorAction

    /** Gets a public Lichess study ([input]: its identifier or address) and merges it. */
    data class FetchStudy(
        val input: String,
    ) : EditorAction

    /** The chosen file could not be read. */
    data object ImportFileUnreadable : EditorAction
}

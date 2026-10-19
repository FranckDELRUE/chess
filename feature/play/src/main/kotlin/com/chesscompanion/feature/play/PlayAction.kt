package com.chesscompanion.feature.play

import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.chess.pgn.PgnIssue
import com.chesscompanion.core.ui.board.AnnotationColor

/** Everything the game screen can ask its ViewModel (unidirectional data flow). */
sealed interface PlayAction {
    /** Moves and board settings. */
    sealed interface Board : PlayAction

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
    ) : Board

    data object CancelPromotion : Board

    data object NewGame : Board

    data object Flip : Board

    data class ShowLegalMoves(
        val show: Boolean,
    ) : Board

    /** Moving through the game tree. */
    sealed interface Navigation : PlayAction

    data object ToStart : Navigation

    data object Back : Navigation

    data object Forward : Navigation

    data object ToEnd : Navigation

    data class GoTo(
        val path: List<Int>,
    ) : Navigation

    /** Coloured squares and arrows (S1-8). */
    sealed interface Annotation : PlayAction

    data object ToggleAnnotate : Annotation

    data class PickAnnotationColor(
        val color: AnnotationColor,
    ) : Annotation

    data object ClearAnnotations : Annotation

    /** Import, export and messages. */
    sealed interface Document : PlayAction

    /** Pasted text: a FEN starts a game from that position, anything else is read as PGN. */
    data class ImportText(
        val text: String,
    ) : Document

    data class OpenFile(
        val uri: String,
    ) : Document

    data class SaveFile(
        val uri: String,
    ) : Document

    data class PickGame(
        val index: Int,
    ) : Document

    data object DismissGameChoice : Document

    data object MessageShown : Document
}

/** One-off message shown in a snackbar. */
sealed interface PlayMessage {
    data class Imported(
        val games: Int,
        val errors: Int,
    ) : PlayMessage

    /** Nothing could be read; [error] is the first one, null for an empty text. */
    data class ImportFailed(
        val error: PgnIssue?,
    ) : PlayMessage

    data object PositionLoaded : PlayMessage

    data object Saved : PlayMessage

    data class FileError(
        val detail: String,
    ) : PlayMessage
}

/** A game offered when a pasted or opened PGN holds several. */
data class GameSummary(
    val white: String,
    val black: String,
    val date: String,
    val result: String,
)

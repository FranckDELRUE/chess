package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.PieceLetters
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.MarkColor
import com.chesscompanion.core.chess.repertoire.NodeNotes
import com.chesscompanion.core.chess.repertoire.RepertoireArrow
import com.chesscompanion.core.chess.repertoire.RepertoireSquareMark
import com.chesscompanion.core.chess.toSan
import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.Arrow
import com.chesscompanion.core.ui.board.BoardAnnotations
import com.chesscompanion.core.ui.board.BoardHighlights

private fun MarkColor.toUi() = AnnotationColor.valueOf(name)

private fun AnnotationColor.toMark() = MarkColor.valueOf(name)

/** The marks of a position as the board draws them. */
internal fun NodeNotes.toAnnotations(): BoardAnnotations =
    BoardAnnotations(
        squares = squares.associate { it.square to it.color.toUi() },
        arrows = arrows.associate { Arrow(it.from, it.to) to it.color.toUi() },
    )

/** These notes with the marks replaced by [marks]; comment and source are kept. */
internal fun NodeNotes.withMarks(marks: BoardAnnotations): NodeNotes =
    copy(
        squares = marks.squares.map { (square, color) -> RepertoireSquareMark(square, color.toMark()) },
        arrows = marks.arrows.map { (arrow, color) -> RepertoireArrow(arrow.from, arrow.to, color.toMark()) },
    )

internal fun EditorModel.toUiState(name: String): RepertoireEditorUiState {
    val selected = input.selected
    val position = position
    return RepertoireEditorUiState(
        status = EditorStatus.READY,
        name = name,
        side = repertoire.side,
        board = position.board,
        flipped = flipped,
        highlights =
            BoardHighlights(
                selected = selected,
                targets = selected?.let { input.targetsOf(it) }.orEmpty(),
                lastMove = trail.lastOrNull()?.move,
                check = if (position.isCheck) position.board.kingSquare(position.sideToMove) else null,
            ),
        promotionSide = if (input.pendingPromotion != null) position.sideToMove else null,
        lineText = lineText(),
        canGoBack = trail.isNotEmpty(),
        continuations =
            repertoire
                .movesFrom(
                    current,
                ).map { Continuation(it.move.uci, position.toSan(it.move, PieceLetters.FRENCH)) },
        comment = notes.comment,
        sourceBook = sourceDraft.book,
        sourceChapter = sourceDraft.chapter,
        sourcePage = sourceDraft.page,
        annotations = notes.toAnnotations(),
        annotating = annotating,
        annotationColor = color,
        confirmDelete = confirmDelete,
        moveCount = repertoire.edgeCount,
    )
}

/** `1. e4 e5 2. Cf3`, in French letters, from the start of the repertoire to the position shown. */
private fun EditorModel.lineText(): String {
    val start = checkNotNull(repertoire.node(repertoire.root)).position
    var number = start.fullmoveNumber
    var white = start.sideToMove == Side.WHITE
    val tokens = mutableListOf<String>()
    trail.forEachIndexed { index, edge ->
        val before = checkNotNull(repertoire.node(edge.from)).position
        if (white) {
            tokens += "$number."
        } else if (index == 0) {
            tokens += "$number..."
        }
        tokens += before.toSan(edge.move, PieceLetters.FRENCH)
        if (!white) number++
        white = !white
    }
    return tokens.joinToString(" ")
}

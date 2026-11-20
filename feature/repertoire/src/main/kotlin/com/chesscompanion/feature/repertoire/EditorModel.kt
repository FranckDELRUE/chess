package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.Move
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.repertoire.NodeNotes
import com.chesscompanion.core.chess.repertoire.PositionId
import com.chesscompanion.core.chess.repertoire.Repertoire
import com.chesscompanion.core.chess.repertoire.RepertoireEdge
import com.chesscompanion.core.chess.repertoire.Source
import com.chesscompanion.core.ui.board.AnnotationColor

/** What is typed in the three source fields; a source exists only once it has a book. */
internal data class SourceDraft(
    val book: String = "",
    val chapter: String = "",
    val page: String = "",
) {
    fun with(
        field: SourceField,
        text: String,
    ): SourceDraft =
        when (field) {
            SourceField.BOOK -> copy(book = text)
            SourceField.CHAPTER -> copy(chapter = text)
            SourceField.PAGE -> copy(page = text)
        }

    fun toSource(): Source? =
        book
            .trim()
            .takeIf {
                it.isNotEmpty()
            }?.let { Source(it, chapter.trim().ifEmpty { null }, page.trim().ifEmpty { null }) }

    companion object {
        fun of(source: Source?) = SourceDraft(source?.book.orEmpty(), source?.chapter.orEmpty(), source?.page.orEmpty())
    }
}

/**
 * The editor of a repertoire, as an immutable model: the [repertoire], the [trail] of moves from
 * its start to the position shown, and the state of the board input. [reduce] applies an action and
 * returns the next model; the repertoire instance changes only when its content does.
 */
internal data class EditorModel(
    val repertoire: Repertoire,
    val trail: List<RepertoireEdge>,
    val input: EditorInput,
    val flipped: Boolean,
    val annotating: Boolean = false,
    val color: AnnotationColor = AnnotationColor.GREEN,
    val sourceDraft: SourceDraft = SourceDraft(),
    val confirmDelete: Boolean = false,
) {
    val current: PositionId get() = trail.lastOrNull()?.to ?: repertoire.root

    val position: Position get() = checkNotNull(repertoire.node(current)).position

    val notes: NodeNotes get() = repertoire.notes(current)

    fun reduce(action: EditorAction): EditorModel =
        when (action) {
            is EditorAction.Board -> board(action)
            is EditorAction.Promote -> step(input.promote(action.type))
            EditorAction.CancelPromotion -> copy(input = input.cancelPromotion())
            EditorAction.Back -> if (trail.isEmpty()) this else show(repertoire, trail.dropLast(1))
            EditorAction.ToStart -> show(repertoire, emptyList())
            is EditorAction.Follow -> follow(action.uci)
            // A move proposed from outside (the explorer) enters only if it is legal here (rule 2).
            is EditorAction.PlayUci -> Move.parseUci(action.uci)?.takeIf(position::isLegal)?.let(::play) ?: this
            EditorAction.Flip -> copy(flipped = !flipped)
            else -> edit(action)
        }

    /** Actions that write notes and marks, or delete. */
    private fun edit(action: EditorAction): EditorModel =
        when (action) {
            EditorAction.ToggleAnnotate -> copy(annotating = !annotating, input = input.copy(selected = null))
            is EditorAction.PickAnnotationColor -> copy(annotating = true, color = action.color)
            EditorAction.ClearAnnotations -> withNotes(notes.copy(arrows = emptyList(), squares = emptyList()))
            is EditorAction.EditComment -> withNotes(notes.copy(comment = action.text))
            is EditorAction.EditSource -> editSource(action)
            EditorAction.AskDelete -> if (trail.isEmpty()) this else copy(confirmDelete = true)
            EditorAction.ConfirmDelete -> deleteLastMove()
            EditorAction.DismissDelete -> copy(confirmDelete = false)
            else -> this
        }

    private fun board(action: EditorAction.Board): EditorModel =
        when {
            annotating -> mark(action)
            action is EditorAction.SquareTap -> step(input.tap(action.square))
            action is EditorAction.DragStart -> step(input.dragStart(action.square))
            action is EditorAction.Drop -> step(input.drop(action.from, action.to))
            else -> this
        }

    /** A tap colours a square and a drag draws an arrow; a mark of the same colour is erased. */
    private fun mark(action: EditorAction.Board): EditorModel {
        val marks = notes.toAnnotations()
        return when (action) {
            is EditorAction.SquareTap -> withNotes(notes.withMarks(marks.toggleSquare(action.square, color)))
            is EditorAction.Drop -> withNotes(notes.withMarks(marks.toggleArrow(action.from, action.to, color)))
            else -> this
        }
    }

    private fun step(result: InputStep): EditorModel {
        val moved = copy(input = result.input)
        return result.played?.let(moved::play) ?: moved
    }

    /** Plays [move] from the position shown: follows it if the repertoire has it, adds it otherwise. */
    private fun play(move: Move): EditorModel {
        val added = repertoire.add(current, move)
        val next = if (added.created) added.repertoire else repertoire
        val edge = checkNotNull(next.movesFrom(current).firstOrNull { it.move == move })
        return show(next, trail + edge)
    }

    private fun follow(uci: String): EditorModel {
        val edge = Move.parseUci(uci)?.let { move -> repertoire.movesFrom(current).firstOrNull { it.move == move } }
        return if (edge == null) this else show(repertoire, trail + edge)
    }

    private fun deleteLastMove(): EditorModel {
        val last = trail.lastOrNull() ?: return copy(confirmDelete = false)
        return show(repertoire.removeMove(last.from, last.move), trail.dropLast(1))
    }

    private fun editSource(action: EditorAction.EditSource): EditorModel {
        val draft = sourceDraft.with(action.field, action.text)
        return copy(sourceDraft = draft).withNotes(notes.copy(source = draft.toSource()))
    }

    private fun withNotes(value: NodeNotes): EditorModel = copy(repertoire = repertoire.withNotes(current, value))

    /** Shows the position at the end of [newTrail] in [newRepertoire], with a fresh input. */
    internal fun show(
        newRepertoire: Repertoire,
        newTrail: List<RepertoireEdge>,
    ): EditorModel {
        val id = newTrail.lastOrNull()?.to ?: newRepertoire.root
        val node = checkNotNull(newRepertoire.node(id))
        return copy(
            repertoire = newRepertoire,
            trail = newTrail,
            input = EditorInput(node.position),
            sourceDraft = SourceDraft.of(newRepertoire.notes(id).source),
            confirmDelete = false,
        )
    }

    companion object {
        fun start(repertoire: Repertoire): EditorModel {
            val root = checkNotNull(repertoire.node(repertoire.root))
            return EditorModel(
                repertoire = repertoire,
                trail = emptyList(),
                input = EditorInput(root.position),
                flipped = repertoire.side == Side.BLACK,
            )
        }
    }
}

/** The same position shown in [imported], a repertoire that only gained moves and notes. */
internal fun EditorModel.withImported(imported: Repertoire): EditorModel = show(imported, trail)

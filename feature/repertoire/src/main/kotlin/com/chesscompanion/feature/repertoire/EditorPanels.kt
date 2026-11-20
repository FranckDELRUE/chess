package com.chesscompanion.feature.repertoire

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.BoardColors
import com.chesscompanion.core.ui.board.nameRes

const val EDITOR_BACK_TAG = "editor-back"
const val EDITOR_START_TAG = "editor-start"
const val EDITOR_FLIP_TAG = "editor-flip"
const val EDITOR_ANNOTATE_TAG = "editor-annotate"
const val EDITOR_CLEAR_MARKS_TAG = "editor-clear-marks"
const val EDITOR_COMMENT_TAG = "editor-comment"
const val EDITOR_DELETE_TAG = "editor-delete"
const val EDITOR_IMPORT_TAG = "editor-import"

fun editorContinuationTag(uci: String) = "editor-continuation-$uci"

fun editorColorTag(color: AnnotationColor) = "editor-color-${color.name.lowercase()}"

fun editorSourceTag(field: SourceField) = "editor-source-${field.name.lowercase()}"

private const val DOT_SIZE = 32
private const val SELECTED_BORDER = 3
private const val COMMENT_LINES = 3
private const val BOOK_WEIGHT = 2f

@Composable
internal fun EditorPanel(
    state: RepertoireEditorUiState,
    explorer: ExplorerUiState,
    onAction: (EditorAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Header(state)
            NavigationRow(state, onAction)
            Continuations(state, onAction)
            ExplorerSection(explorer, onAction)
            MarksBar(state, onAction)
            CommentField(state, onAction)
            SourceFields(state, onAction)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onAction(EditorAction.AskDelete) },
                    enabled = state.canGoBack,
                    modifier = Modifier.testTag(EDITOR_DELETE_TAG),
                ) { Text(stringResource(R.string.editor_delete)) }
                OutlinedButton(
                    onClick = { onAction(EditorAction.OpenImport) },
                    modifier = Modifier.testTag(EDITOR_IMPORT_TAG),
                ) { Text(stringResource(R.string.editor_import)) }
            }
        }
    }
}

@Composable
private fun Header(state: RepertoireEditorUiState) {
    val side =
        stringResource(
            if (state.side ==
                Side.WHITE
            ) {
                R.string.repertoire_side_white
            } else {
                R.string.repertoire_side_black
            },
        )
    Text(state.name, style = MaterialTheme.typography.titleLarge)
    Text(
        stringResource(
            R.string.repertoire_summary,
            side,
            pluralStringResource(R.plurals.repertoire_move_count, state.moveCount, state.moveCount),
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        state.lineText.ifEmpty { stringResource(R.string.editor_start_position) },
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.testTag(EDITOR_LINE_TAG),
    )
}

@Composable
private fun NavigationRow(
    state: RepertoireEditorUiState,
    onAction: (EditorAction) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onAction(EditorAction.ToStart) },
            enabled = state.canGoBack,
            modifier = Modifier.testTag(EDITOR_START_TAG),
        ) { Text(stringResource(R.string.editor_to_start)) }
        OutlinedButton(
            onClick = { onAction(EditorAction.Back) },
            enabled = state.canGoBack,
            modifier = Modifier.testTag(EDITOR_BACK_TAG),
        ) { Text(stringResource(R.string.editor_back)) }
        OutlinedButton(
            onClick = { onAction(EditorAction.Flip) },
            modifier = Modifier.testTag(EDITOR_FLIP_TAG),
        ) { Text(stringResource(R.string.editor_flip)) }
    }
}

@Composable
private fun Continuations(
    state: RepertoireEditorUiState,
    onAction: (EditorAction) -> Unit,
) {
    if (state.continuations.isEmpty()) {
        Text(stringResource(R.string.editor_no_continuation))
        return
    }
    Text(stringResource(R.string.editor_continuations))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.continuations.forEach { next ->
            AssistChip(
                onClick = { onAction(EditorAction.Follow(next.uci)) },
                label = { Text(next.label) },
                modifier = Modifier.testTag(editorContinuationTag(next.uci)),
            )
        }
    }
}

@Composable
private fun MarksBar(
    state: RepertoireEditorUiState,
    onAction: (EditorAction) -> Unit,
) {
    val colors = BoardColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(
            checked = state.annotating,
            onCheckedChange = { onAction(EditorAction.ToggleAnnotate) },
            modifier = Modifier.testTag(EDITOR_ANNOTATE_TAG),
        )
        Text(stringResource(R.string.editor_annotate))
        AnnotationColor.entries.forEach { color ->
            val label = stringResource(R.string.editor_color, stringResource(color.nameRes()))
            val selected = state.annotating && state.annotationColor == color
            Box(
                Modifier
                    .size(DOT_SIZE.dp)
                    .clip(CircleShape)
                    .background(colors.of(color))
                    .border(
                        width = if (selected) SELECTED_BORDER.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                    ).testTag(editorColorTag(color))
                    .semantics {
                        contentDescription = label
                        role = Role.RadioButton
                        this.selected = selected
                    }.clickable { onAction(EditorAction.PickAnnotationColor(color)) },
            )
        }
        TextButton(
            onClick = { onAction(EditorAction.ClearAnnotations) },
            enabled = !state.annotations.isEmpty,
            modifier = Modifier.testTag(EDITOR_CLEAR_MARKS_TAG),
        ) { Text(stringResource(R.string.editor_clear_marks)) }
    }
}

@Composable
private fun CommentField(
    state: RepertoireEditorUiState,
    onAction: (EditorAction) -> Unit,
) {
    OutlinedTextField(
        value = state.comment,
        onValueChange = { onAction(EditorAction.EditComment(it)) },
        label = { Text(stringResource(R.string.editor_comment)) },
        minLines = COMMENT_LINES,
        modifier = Modifier.fillMaxWidth().testTag(EDITOR_COMMENT_TAG),
    )
}

@Composable
private fun SourceFields(
    state: RepertoireEditorUiState,
    onAction: (EditorAction) -> Unit,
) {
    Text(stringResource(R.string.editor_source))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SourceField.entries.forEach { field ->
            val (value, label) =
                when (field) {
                    SourceField.BOOK -> state.sourceBook to R.string.editor_source_book
                    SourceField.CHAPTER -> state.sourceChapter to R.string.editor_source_chapter
                    SourceField.PAGE -> state.sourcePage to R.string.editor_source_page
                }
            OutlinedTextField(
                value = value,
                onValueChange = { onAction(EditorAction.EditSource(field, it)) },
                label = { Text(stringResource(label)) },
                singleLine = true,
                modifier =
                    Modifier
                        .weight(
                            if (field ==
                                SourceField.BOOK
                            ) {
                                BOOK_WEIGHT
                            } else {
                                1f
                            },
                        ).testTag(editorSourceTag(field)),
            )
        }
    }
}

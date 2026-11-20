package com.chesscompanion.feature.repertoire

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chesscompanion.core.ui.board.ChessBoard
import com.chesscompanion.core.ui.board.DragMode
import com.chesscompanion.core.ui.board.PromotionDialog

const val EDITOR_LINE_TAG = "editor-line"
const val EDITOR_NOT_FOUND_TAG = "editor-not-found"
const val EDITOR_DELETE_CONFIRM_TAG = "editor-delete-confirm"

/** The editor of one repertoire: the board on one side, the notes and moves on the other. */
@Composable
fun RepertoireEditorScreen(viewModel: RepertoireEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // The app may be killed once it is in the background: write what is pending.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.saveNow() }
    RepertoireEditorContent(state, viewModel::onAction)
}

@Composable
internal fun RepertoireEditorContent(
    state: RepertoireEditorUiState,
    onAction: (EditorAction) -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        when (state.status) {
            EditorStatus.LOADING -> Text(stringResource(R.string.repertoire_loading), Modifier.padding(16.dp))
            EditorStatus.NOT_FOUND ->
                Text(
                    stringResource(R.string.editor_not_found),
                    Modifier.padding(16.dp).testTag(EDITOR_NOT_FOUND_TAG),
                )
            EditorStatus.READY -> EditorLayout(state, onAction)
        }
    }
    state.promotionSide?.let { side ->
        PromotionDialog(side, { onAction(EditorAction.Promote(it)) }, { onAction(EditorAction.CancelPromotion) })
    }
    if (state.confirmDelete) DeleteDialog(onAction)
    ImportDialog(state.import, onAction)
}

@Composable
private fun EditorLayout(
    state: RepertoireEditorUiState,
    onAction: (EditorAction) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        val board = @Composable { modifier: Modifier -> EditorBoard(state, onAction, modifier) }
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                board(Modifier.weight(1f).fillMaxSize())
                EditorPanel(state, onAction, Modifier.weight(1f).fillMaxSize())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                board(Modifier.fillMaxWidth().weight(1f))
                EditorPanel(state, onAction, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun EditorBoard(
    state: RepertoireEditorUiState,
    onAction: (EditorAction) -> Unit,
    modifier: Modifier,
) {
    ChessBoard(
        board = state.board,
        flipped = state.flipped,
        onSquareTap = { onAction(EditorAction.SquareTap(it)) },
        onDragStart = { onAction(EditorAction.DragStart(it)) },
        onMove = { from, to -> onAction(EditorAction.Drop(from, to)) },
        modifier = modifier,
        highlights = state.highlights,
        annotations = state.annotations,
        dragMode = if (state.annotating) DragMode.ARROW else DragMode.PIECE,
        arrowColor = state.annotationColor,
    )
}

@Composable
private fun DeleteDialog(onAction: (EditorAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(EditorAction.DismissDelete) },
        title = { Text(stringResource(R.string.editor_delete_title)) },
        text = { Text(stringResource(R.string.editor_delete_message)) },
        confirmButton = {
            TextButton(
                onClick = { onAction(EditorAction.ConfirmDelete) },
                modifier = Modifier.testTag(EDITOR_DELETE_CONFIRM_TAG),
            ) { Text(stringResource(R.string.repertoire_delete_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = { onAction(EditorAction.DismissDelete) }) {
                Text(stringResource(R.string.repertoire_cancel))
            }
        },
    )
}

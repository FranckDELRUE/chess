package com.chesscompanion.feature.play

import android.content.ClipData
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.pgn.GameSession
import com.chesscompanion.core.ui.board.BoardHighlights
import com.chesscompanion.core.ui.board.ChessBoard
import com.chesscompanion.core.ui.board.DragMode
import com.chesscompanion.core.ui.board.PromotionDialog
import com.chesscompanion.core.ui.theme.ChessTheme
import kotlinx.coroutines.launch

/** Test tag of the status line under the board. */
const val PLAY_STATUS_TAG = "play-status"

/** PGN MIME types offered by the file picker (Lichess, Chess.com and generic text files). */
private val PGN_MIME_TYPES =
    arrayOf("application/x-chess-pgn", "application/vnd.chess-pgn", "text/plain", "application/octet-stream")

/** Clipboard and file actions; they need the platform, so the stateful screen provides them. */
class DocumentActions(
    val onPaste: () -> Unit = {},
    val onOpen: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onCopyPgn: () -> Unit = {},
    val onCopyFen: () -> Unit = {},
)

@Composable
fun PlayScreen(viewModel: PlayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val copiedPgn = stringResource(R.string.play_copied_pgn)
    val copiedFen = stringResource(R.string.play_copied_fen)
    val saveName = stringResource(R.string.play_save_file_name)
    val open =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.onAction(PlayAction.OpenFile(it.toString())) }
        }
    val save =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-chess-pgn")) { uri ->
            uri?.let { viewModel.onAction(PlayAction.SaveFile(it.toString())) }
        }

    fun copy(
        label: String,
        text: String,
        confirmation: String,
    ) = scope.launch {
        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(label, text)))
        snackbar.showSnackbar(confirmation)
    }

    PlayContent(
        state = state,
        onAction = viewModel::onAction,
        documents =
            DocumentActions(
                onPaste = {
                    scope.launch {
                        val text =
                            clipboard
                                .getClipEntry()
                                ?.clipData
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.text
                        viewModel.onAction(PlayAction.ImportText(text?.toString().orEmpty()))
                    }
                },
                onOpen = { open.launch(PGN_MIME_TYPES) },
                onSave = { save.launch(saveName) },
                onCopyPgn = { copy("PGN", viewModel.exportPgn(), copiedPgn) },
                onCopyFen = { copy("FEN", state.fen, copiedFen) },
            ),
        snackbar = snackbar,
    )
}

@Composable
fun PlayContent(
    state: PlayUiState,
    onAction: (PlayAction) -> Unit,
    documents: DocumentActions,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val messageText = state.message?.let { messageText(it) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            onAction(PlayAction.MessageShown)
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .safeDrawingPadding()
                .padding(16.dp),
        ) {
            val board = @Composable { modifier: Modifier ->
                ChessBoard(
                    board = state.board,
                    flipped = state.flipped,
                    onSquareTap = { onAction(PlayAction.SquareTap(it)) },
                    onDragStart = { onAction(PlayAction.DragStart(it)) },
                    onMove = { from, to -> onAction(PlayAction.Drop(from, to)) },
                    modifier = modifier,
                    highlights = state.highlights,
                    annotations = state.annotations,
                    dragMode = if (state.annotating) DragMode.ARROW else DragMode.PIECE,
                    arrowColor = state.annotationColor,
                )
            }
            if (maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    board(Modifier.weight(1f).fillMaxSize())
                    SidePanel(state, onAction, documents, Modifier.weight(1f).fillMaxSize())
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    board(Modifier.fillMaxWidth().weight(1f))
                    SidePanel(state, onAction, documents, Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
    state.promotionSide?.let { side ->
        PromotionDialog(
            side = side,
            onPick = { onAction(PlayAction.Promote(it)) },
            onDismiss = { onAction(PlayAction.CancelPromotion) },
        )
    }
    state.gameChoice?.let { games ->
        GameChoiceDialog(
            games = games,
            onPick = { onAction(PlayAction.PickGame(it)) },
            onDismiss = { onAction(PlayAction.DismissGameChoice) },
        )
    }
}

@Composable
private fun SidePanel(
    state: PlayUiState,
    onAction: (PlayAction) -> Unit,
    documents: DocumentActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(statusMessage(state.status, state.sideToMove, state.highlights.check != null)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag(PLAY_STATUS_TAG),
            )
            NavigationBar(state, onAction)
            AnnotationBar(state, onAction)
            MoveList(state.moveList, onGoTo = { onAction(PlayAction.GoTo(it)) }, modifier = Modifier.weight(1f))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(onClick = { onAction(PlayAction.NewGame) }) { Text(stringResource(R.string.play_new_game)) }
                OutlinedButton(
                    onClick = { onAction(PlayAction.Flip) },
                ) { Text(stringResource(R.string.play_flip_board)) }
                OutlinedButton(onClick = documents.onPaste) { Text(stringResource(R.string.play_paste)) }
                OutlinedButton(onClick = documents.onOpen) { Text(stringResource(R.string.play_open)) }
                OutlinedButton(onClick = documents.onCopyPgn) { Text(stringResource(R.string.play_copy_pgn)) }
                OutlinedButton(onClick = documents.onCopyFen) { Text(stringResource(R.string.play_copy_fen)) }
                OutlinedButton(onClick = documents.onSave) { Text(stringResource(R.string.play_save)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = state.showLegalMoves, onCheckedChange = { onAction(PlayAction.ShowLegalMoves(it)) })
                Text(stringResource(R.string.play_show_legal_moves))
            }
        }
    }
}

private fun previewState(status: GameStatus = GameStatus.Ongoing): PlayUiState {
    val session = GameSession.new()
    return PlayUiState(
        board = Position.START.board,
        sideToMove = Side.WHITE,
        status = status,
        highlights = BoardHighlights(),
        flipped = false,
        showLegalMoves = true,
        promotionSide = null,
        moveList = MoveListBuilder.build(session),
    )
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun PlayLandscapePreview() = ChessTheme { PlayContent(previewState(), {}, DocumentActions()) }

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun PlayPortraitDarkPreview() =
    ChessTheme(darkTheme = true) { PlayContent(previewState(GameStatus.Stalemate), {}, DocumentActions()) }

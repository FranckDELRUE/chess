package com.chesscompanion.feature.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chesscompanion.core.chess.GameStatus
import com.chesscompanion.core.chess.PieceType
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.chess.Square
import com.chesscompanion.core.ui.board.BoardHighlights
import com.chesscompanion.core.ui.board.ChessBoard
import com.chesscompanion.core.ui.board.PromotionDialog
import com.chesscompanion.core.ui.theme.ChessTheme

/** Test tag of the status line under the board. */
const val PLAY_STATUS_TAG = "play-status"

@Composable
fun PlayScreen(viewModel: PlayViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PlayContent(
        state = state,
        actions =
            PlayActions(
                onSquareTap = viewModel::onSquareTap,
                onDragStart = viewModel::onDragStart,
                onMove = viewModel::onMove,
                onPromotion = viewModel::onPromotion,
                onPromotionCancel = viewModel::onPromotionCancel,
                onNewGame = viewModel::newGame,
                onFlip = viewModel::flip,
                onShowLegalMoves = viewModel::setShowLegalMoves,
            ),
    )
}

/** Everything the screen can ask the ViewModel. */
class PlayActions(
    val onSquareTap: (Square) -> Unit = {},
    val onDragStart: (Square) -> Unit = {},
    val onMove: (Square, Square) -> Unit = { _, _ -> },
    val onPromotion: (PieceType) -> Unit = {},
    val onPromotionCancel: () -> Unit = {},
    val onNewGame: () -> Unit = {},
    val onFlip: () -> Unit = {},
    val onShowLegalMoves: (Boolean) -> Unit = {},
)

@Composable
fun PlayContent(
    state: PlayUiState,
    actions: PlayActions,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            val board = @Composable { modifier: Modifier ->
                ChessBoard(
                    board = state.board,
                    flipped = state.flipped,
                    onSquareTap = actions.onSquareTap,
                    onDragStart = actions.onDragStart,
                    onMove = actions.onMove,
                    modifier = modifier,
                    highlights = state.highlights,
                )
            }
            if (maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    board(Modifier.weight(1f).fillMaxSize())
                    SidePanel(state, actions, Modifier.weight(1f).fillMaxSize())
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    board(Modifier.fillMaxWidth().weight(1f))
                    SidePanel(state, actions, Modifier.fillMaxWidth())
                }
            }
        }
        state.promotionSide?.let { side ->
            PromotionDialog(side = side, onPick = actions.onPromotion, onDismiss = actions.onPromotionCancel)
        }
    }
}

@Composable
private fun SidePanel(
    state: PlayUiState,
    actions: PlayActions,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(statusMessage(state.status, state.sideToMove, state.highlights.check != null)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.testTag(PLAY_STATUS_TAG),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions.onNewGame) { Text(stringResource(R.string.play_new_game)) }
                OutlinedButton(onClick = actions.onFlip) { Text(stringResource(R.string.play_flip_board)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = state.showLegalMoves, onCheckedChange = actions.onShowLegalMoves)
                Text(stringResource(R.string.play_show_legal_moves))
            }
        }
    }
}

private fun previewState(
    side: Side = Side.WHITE,
    status: GameStatus = GameStatus.Ongoing,
) = PlayUiState(
    board = Position.START.board,
    sideToMove = side,
    status = status,
    highlights = BoardHighlights(),
    flipped = false,
    showLegalMoves = true,
    promotionSide = null,
)

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun PlayLandscapePreview() = ChessTheme { PlayContent(previewState(), PlayActions()) }

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun PlayPortraitDarkPreview() =
    ChessTheme(darkTheme = true) { PlayContent(previewState(status = GameStatus.Stalemate), PlayActions()) }

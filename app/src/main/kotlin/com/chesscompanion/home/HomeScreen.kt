package com.chesscompanion.home

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chesscompanion.R
import com.chesscompanion.core.ui.board.ChessBoard
import com.chesscompanion.core.ui.theme.ChessTheme

@Composable
fun HomeScreen(
    onOpenEngineDebug: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state = state, onFlip = viewModel::flipBoard, onOpenEngineDebug = onOpenEngineDebug)
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onFlip: () -> Unit,
    onOpenEngineDebug: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
            if (maxWidth > maxHeight) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChessBoard(state.fen, state.flipped, Modifier.weight(1f).fillMaxSize())
                    SidePanel(onFlip, onOpenEngineDebug, Modifier.weight(1f).fillMaxSize())
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChessBoard(state.fen, state.flipped, Modifier.fillMaxWidth().weight(1f))
                    SidePanel(onFlip, onOpenEngineDebug, Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SidePanel(
    onFlip: () -> Unit,
    onOpenEngineDebug: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onFlip) { Text(stringResource(R.string.home_flip_board)) }
            OutlinedButton(onClick = onOpenEngineDebug) { Text(stringResource(R.string.home_engine_debug)) }
        }
    }
}

private const val FEN_ENDGAME = "8/5k2/8/3K4/8/8/4P3/8 w - - 0 1"
private const val FEN_ITALIAN = "r1bqk1nr/pppp1ppp/2n5/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4"
private const val FEN_MIDDLEGAME = "r2q1rk1/pp2bppp/2n1pn2/3p4/3P4/2N1PN2/PP2BPPP/R2Q1RK1 w - - 0 10"

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun HomeLandscapePreview() = ChessTheme { HomeContent(HomeUiState(), {}, {}) }

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun HomePortraitFlippedPreview() = ChessTheme { HomeContent(HomeUiState(FEN_ITALIAN, true), {}, {}) }

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun HomeEndgamePreview() = ChessTheme { HomeContent(HomeUiState(FEN_ENDGAME), {}, {}) }

@Preview(widthDp = 800, heightDp = 1280)
@Composable
private fun HomeMiddlegamePreview() = ChessTheme { HomeContent(HomeUiState(FEN_MIDDLEGAME), {}, {}) }

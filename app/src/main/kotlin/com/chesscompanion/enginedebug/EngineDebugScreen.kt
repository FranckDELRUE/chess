package com.chesscompanion.enginedebug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chesscompanion.R
import com.chesscompanion.core.engine.EngineInfo
import com.chesscompanion.core.engine.Score

private val DEPTH_CHOICES = listOf(10, 16, 18, 20)
private val THREAD_CHOICES = listOf(1, 2, 4)

@Composable
fun EngineDebugScreen(viewModel: EngineDebugViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    EngineDebugContent(
        state = state,
        onFen = viewModel::setFen,
        onDepth = viewModel::setDepth,
        onThreads = viewModel::setThreads,
        onAnalyze = viewModel::analyze,
        onStop = viewModel::stop,
        onBatch = viewModel::runBatch,
        onThermal = viewModel::runThermal,
    )
}

@Composable
@Suppress("LongParameterList")
internal fun EngineDebugContent(
    state: EngineDebugState,
    onFen: (String) -> Unit,
    onDepth: (Int) -> Unit,
    onThreads: (Int) -> Unit,
    onAnalyze: () -> Unit,
    onStop: () -> Unit,
    onBatch: () -> Unit,
    onThermal: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.engine_debug_title), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = state.fen,
                onValueChange = onFen,
                label = { Text(stringResource(R.string.engine_debug_fen)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.busy,
            )
            ChoiceRow(R.string.engine_debug_depth, DEPTH_CHOICES, state.depth, !state.busy, onDepth)
            ChoiceRow(R.string.engine_debug_threads, THREAD_CHOICES, state.threads, !state.busy, onThreads)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAnalyze,
                    enabled = !state.busy,
                ) { Text(stringResource(R.string.engine_debug_analyze)) }
                OutlinedButton(
                    onClick = onStop,
                    enabled = state.busy,
                ) { Text(stringResource(R.string.engine_debug_stop)) }
                OutlinedButton(
                    onClick = onBatch,
                    enabled = !state.busy,
                ) { Text(stringResource(R.string.engine_debug_batch)) }
                OutlinedButton(
                    onClick = onThermal,
                    enabled = !state.busy,
                ) { Text(stringResource(R.string.engine_debug_thermal)) }
            }
            Text(stringResource(R.string.engine_debug_status, state.status.name))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            ResultBlock(state.info, state.bestMove, state.elapsedMs)
            state.log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun ChoiceRow(
    label: Int,
    choices: List<Int>,
    selected: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(label), Modifier.padding(top = 8.dp))
        choices.forEach { value ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                label = { Text(value.toString()) },
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun ResultBlock(
    info: EngineInfo?,
    bestMove: String?,
    elapsedMs: Long,
) {
    if (info == null) return
    val score =
        when (val s = info.score) {
            is Score.Centipawns -> "%+.2f".format(s.value / CP_PER_PAWN)
            is Score.Mate -> "#${s.moves}"
        }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.engine_debug_result, info.depth, score, elapsedMs))
        Text(stringResource(R.string.engine_debug_pv, info.pv.take(PV_MOVES).joinToString(" ")))
        bestMove?.let { Text(stringResource(R.string.engine_debug_best, it)) }
    }
}

private const val CP_PER_PAWN = 100f
private const val PV_MOVES = 8

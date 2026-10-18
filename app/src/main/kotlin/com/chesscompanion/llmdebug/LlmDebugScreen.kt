package com.chesscompanion.llmdebug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.chesscompanion.spike.llm.LlmModel
import com.chesscompanion.spike.llm.PromptVariant

private val THREAD_CHOICES = listOf(2, 4, 6, 8)

@Composable
fun LlmDebugScreen(viewModel: LlmDebugViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LlmDebugContent(
        state = state,
        onModel = viewModel::setModel,
        onThreads = viewModel::setThreads,
        onVariant = viewModel::setVariant,
        onGenerate = viewModel::generate,
        onStop = viewModel::stop,
        onBatch = viewModel::runBatch,
        onThermal = viewModel::runThermal,
    )
}

@Composable
@Suppress("LongParameterList")
internal fun LlmDebugContent(
    state: LlmDebugState,
    onModel: (LlmModel) -> Unit,
    onThreads: (Int) -> Unit,
    onVariant: (PromptVariant) -> Unit,
    onGenerate: () -> Unit,
    onStop: () -> Unit,
    onBatch: () -> Unit,
    onThermal: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.safeDrawingPadding().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.llm_debug_title), style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.llm_debug_model), Modifier.padding(top = 8.dp))
                LlmModel.entries.forEach { model ->
                    FilterChip(
                        selected = model == state.model,
                        onClick = { onModel(model) },
                        label = { Text(model.label) },
                        enabled = !state.busy,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.llm_debug_prompt), Modifier.padding(top = 8.dp))
                PromptVariant.entries.forEach { variant ->
                    FilterChip(
                        selected = variant == state.variant,
                        onClick = { onVariant(variant) },
                        label = { Text(variant.name) },
                        enabled = !state.busy,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.engine_debug_threads), Modifier.padding(top = 8.dp))
                THREAD_CHOICES.forEach { threads ->
                    FilterChip(
                        selected = threads == state.threads,
                        onClick = { onThreads(threads) },
                        label = { Text(threads.toString()) },
                        enabled = !state.busy,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onGenerate,
                    enabled = !state.busy,
                ) { Text(stringResource(R.string.llm_debug_generate)) }
                OutlinedButton(
                    onClick = onStop,
                    enabled = state.busy,
                ) { Text(stringResource(R.string.engine_debug_stop)) }
                OutlinedButton(
                    onClick = onBatch,
                    enabled = !state.busy,
                ) { Text(stringResource(R.string.llm_debug_batch)) }
                OutlinedButton(onClick = onThermal, enabled = !state.busy) {
                    Text(stringResource(R.string.engine_debug_thermal))
                }
            }
            Text(stringResource(R.string.engine_debug_status, state.status.name))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.output.isNotEmpty()) Text(state.output, style = MaterialTheme.typography.bodyLarge)
            state.log.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

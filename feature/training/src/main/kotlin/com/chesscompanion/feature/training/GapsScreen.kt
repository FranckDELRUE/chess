package com.chesscompanion.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chesscompanion.core.data.explorer.ExplorerProblem
import java.util.Locale

const val GAPS_NOT_FOUND_TAG = "gaps-not-found"
const val GAPS_SCAN_TAG = "gaps-scan"
const val GAPS_CANCEL_TAG = "gaps-cancel"
const val GAPS_PROGRESS_TAG = "gaps-progress"
const val GAPS_PROBLEM_TAG = "gaps-problem"
const val GAPS_NONE_TAG = "gaps-none"
const val GAPS_TRUNCATED_TAG = "gaps-truncated"

fun gapAddedTag(index: Int) = "gaps-added-$index"

fun gapRowTag(index: Int) = "gaps-row-$index"

/** The holes of one repertoire: frequent replies of the opponent that it does not answer. */
@Composable
fun GapsScreen(viewModel: GapsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GapsContent(state, viewModel::onAction)
}

@Composable
internal fun GapsContent(
    state: GapsUiState,
    onAction: (GapsAction) -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        when (state.status) {
            GapsStatus.LOADING -> Text(stringResource(R.string.drill_loading), Modifier.padding(16.dp))
            GapsStatus.NOT_FOUND ->
                Text(stringResource(R.string.drill_not_found), Modifier.padding(16.dp).testTag(GAPS_NOT_FOUND_TAG))
            else -> GapsBody(state, onAction)
        }
    }
}

@Composable
private fun GapsBody(
    state: GapsUiState,
    onAction: (GapsAction) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.gaps_title, state.name), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.gaps_explanation), style = MaterialTheme.typography.bodyMedium)
        if (state.status == GapsStatus.SCANNING) {
            Text(
                stringResource(R.string.gaps_progress, state.done, state.total),
                Modifier.testTag(GAPS_PROGRESS_TAG),
            )
            OutlinedButton(onClick = { onAction(GapsAction.Cancel) }, modifier = Modifier.testTag(GAPS_CANCEL_TAG)) {
                Text(stringResource(R.string.gaps_cancel))
            }
        } else {
            Button(onClick = { onAction(GapsAction.Scan) }, modifier = Modifier.testTag(GAPS_SCAN_TAG)) {
                Text(stringResource(if (state.status == GapsStatus.DONE) R.string.gaps_rescan else R.string.gaps_scan))
            }
        }
        if (state.status == GapsStatus.DONE) GapsResult(state, onAction)
    }
}

@Composable
private fun GapsResult(
    state: GapsUiState,
    onAction: (GapsAction) -> Unit,
) {
    state.problem?.let { Text(stringResource(problemText(it)), Modifier.testTag(GAPS_PROBLEM_TAG)) }
    if (state.gaps.isEmpty() && state.problem == null) {
        Text(stringResource(R.string.gaps_none, state.scanned), Modifier.testTag(GAPS_NONE_TAG))
    }
    if (state.truncated) {
        Text(
            stringResource(R.string.gaps_truncated, state.scanned, state.available),
            Modifier.testTag(GAPS_TRUNCATED_TAG),
        )
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.gaps.size) { index ->
            val gap = state.gaps[index]
            Card(
                onClick = { onAction(GapsAction.Add(index)) },
                modifier = Modifier.fillMaxWidth().testTag(gapRowTag(index)),
                enabled = !gap.added && !gap.adding,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(
                            R.string.drill_line,
                            gap.line.ifEmpty { stringResource(R.string.drill_line_start) },
                        ),
                    )
                    Text(
                        stringResource(R.string.gaps_missing, gap.moveSan),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(
                            R.string.gaps_frequency,
                            gap.percent,
                            String.format(Locale.FRANCE, "%,d", gap.games),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    AddStatus(index, gap)
                }
            }
        }
    }
}

internal fun problemText(problem: ExplorerProblem): Int =
    when (problem) {
        ExplorerProblem.NO_TOKEN -> R.string.gaps_problem_no_token
        ExplorerProblem.BAD_TOKEN -> R.string.gaps_problem_bad_token
        ExplorerProblem.RATE_LIMITED -> R.string.gaps_problem_rate_limited
        ExplorerProblem.OFFLINE -> R.string.gaps_problem_offline
        ExplorerProblem.SERVER -> R.string.gaps_problem_server
    }

@Composable
private fun AddStatus(
    index: Int,
    gap: GapRow,
) {
    when {
        gap.adding -> Text(stringResource(R.string.gaps_adding), Modifier.testTag(gapAddedTag(index)))
        gap.added -> {
            Text(
                pluralStringResource(R.plurals.gaps_added, gap.addedMoves, gap.addedMoves),
                Modifier.testTag(gapAddedTag(index)),
                style = MaterialTheme.typography.titleSmall,
            )
            gap.addProblem?.let {
                Text(
                    stringResource(R.string.gaps_added_incomplete, stringResource(problemText(it))),
                )
            }
        }
        else -> Text(stringResource(R.string.gaps_tap_to_add), style = MaterialTheme.typography.bodySmall)
    }
}

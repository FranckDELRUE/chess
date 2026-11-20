package com.chesscompanion.feature.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chesscompanion.core.data.AnalysisJob
import com.chesscompanion.core.data.AnalysisJobState

/** Test tags of the background analysis panel. */
const val QUEUE_ANALYSE_TAG = "queue-analyse"
const val QUEUE_CANCEL_TAG = "queue-cancel"
const val QUEUE_STATUS_TAG = "queue-status"

/**
 * "Analyser la partie" and the progress of the background analysis (S2-5): while a game is in the
 * queue, its progress and "Annuler"; afterwards, how the last analysis ended.
 */
@Composable
internal fun QueuePanel(
    job: AnalysisJob?,
    onAnalyse: () -> Unit,
    onAction: (PlayAction) -> Unit,
) {
    val active = job?.state == AnalysisJobState.PENDING || job?.state == AnalysisJobState.RUNNING
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (active && job != null) {
                Text(progressText(job), Modifier.testTag(QUEUE_STATUS_TAG), style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = { onAction(PlayAction.CancelGameAnalysis) },
                    modifier = Modifier.testTag(QUEUE_CANCEL_TAG),
                ) { Text(stringResource(R.string.play_queue_cancel)) }
            } else {
                OutlinedButton(onClick = onAnalyse, modifier = Modifier.testTag(QUEUE_ANALYSE_TAG)) {
                    Text(stringResource(R.string.play_queue_analyse))
                }
                job?.let {
                    Text(
                        endText(it),
                        Modifier.testTag(QUEUE_STATUS_TAG),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (active && job != null && job.total > 0) {
            LinearProgressIndicator(progress = { job.done.toFloat() / job.total }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun progressText(job: AnalysisJob): String =
    if (job.state == AnalysisJobState.PENDING && job.done == 0) {
        stringResource(R.string.play_queue_waiting)
    } else {
        stringResource(R.string.play_queue_progress, job.done, job.total)
    }

@Composable
private fun endText(job: AnalysisJob): String =
    when (job.state) {
        AnalysisJobState.DONE -> stringResource(R.string.play_queue_done, job.total)
        AnalysisJobState.CANCELLED -> stringResource(R.string.play_queue_cancelled)
        else -> stringResource(R.string.play_queue_failed)
    }

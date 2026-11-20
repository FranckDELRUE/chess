package com.chesscompanion.feature.repertoire

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chesscompanion.core.chess.repertoire.ImportRejection
import com.chesscompanion.core.chess.repertoire.RejectionReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

const val IMPORT_FILE_TAG = "import-file"
const val IMPORT_TEXT_TAG = "import-text"
const val IMPORT_RUN_TAG = "import-run"
const val IMPORT_PROGRESS_TAG = "import-progress"
const val IMPORT_REPORT_TAG = "import-report"
const val IMPORT_CLOSE_TAG = "import-close"

private const val MAX_FILE_BYTES = 32 * 1024 * 1024
private const val SHOWN_REJECTIONS = 10
private const val MILLIS_PER_SECOND = 1000f

/** The import dialog of the editor, whichever step it is at. */
@Composable
internal fun ImportDialog(
    state: ImportUiState,
    onAction: (EditorAction) -> Unit,
) {
    when (state) {
        ImportUiState.Closed -> Unit
        ImportUiState.Form -> ImportForm(onAction)
        is ImportUiState.Running -> ImportRunning(state)
        is ImportUiState.Done -> ImportDone(state.summary, onAction)
        ImportUiState.FileUnreadable -> ImportMessage(stringResource(R.string.import_file_unreadable), onAction)
    }
}

@Composable
private fun ImportForm(onAction: (EditorAction) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pasted by rememberSaveable { mutableStateOf("") }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                scope.launch {
                    val text = withContext(Dispatchers.IO) { readPgn(context, uri) }
                    onAction(if (text == null) EditorAction.ImportFileUnreadable else EditorAction.RunImport(text))
                }
            }
        }
    AlertDialog(
        onDismissRequest = { onAction(EditorAction.CloseImport) },
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.import_help))
                OutlinedButton(onClick = { picker.launch("*/*") }, modifier = Modifier.testTag(IMPORT_FILE_TAG)) {
                    Text(stringResource(R.string.import_choose_file))
                }
                OutlinedTextField(
                    value = pasted,
                    onValueChange = { pasted = it },
                    label = { Text(stringResource(R.string.import_paste)) },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag(IMPORT_TEXT_TAG),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(EditorAction.RunImport(pasted)) },
                enabled = pasted.isNotBlank(),
                modifier = Modifier.testTag(IMPORT_RUN_TAG),
            ) { Text(stringResource(R.string.import_run)) }
        },
        dismissButton = {
            TextButton(onClick = { onAction(EditorAction.CloseImport) }) {
                Text(stringResource(R.string.repertoire_cancel))
            }
        },
    )
}

@Composable
private fun ImportRunning(state: ImportUiState.Running) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.import_progress, state.done, state.total))
                if (state.total > 0) {
                    LinearProgressIndicator(
                        progress = { state.done.toFloat() / state.total },
                        modifier = Modifier.fillMaxWidth().testTag(IMPORT_PROGRESS_TAG),
                    )
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth().testTag(IMPORT_PROGRESS_TAG))
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun ImportDone(
    summary: ImportSummary,
    onAction: (EditorAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(EditorAction.CloseImport) },
        title = { Text(stringResource(R.string.import_report_title)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()).testTag(IMPORT_REPORT_TAG),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (summary.gamesInFile == 0) {
                    Text(stringResource(R.string.import_no_game))
                } else {
                    Text(stringResource(R.string.import_games, summary.gamesMerged, summary.gamesInFile))
                    Text(stringResource(R.string.import_added, summary.addedMoves))
                    Text(stringResource(R.string.import_existing, summary.existingMoves))
                    Text(stringResource(R.string.import_duration, summary.elapsedMs / MILLIS_PER_SECOND))
                }
                RejectionList(summary.rejected)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(EditorAction.CloseImport) },
                modifier = Modifier.testTag(IMPORT_CLOSE_TAG),
            ) { Text(stringResource(R.string.import_close)) }
        },
    )
}

@Composable
private fun RejectionList(rejected: List<ImportRejection>) {
    if (rejected.isEmpty()) return
    Text(stringResource(R.string.import_rejected, rejected.size))
    rejected.take(SHOWN_REJECTIONS).forEach { Text(rejectionText(it)) }
    if (rejected.size > SHOWN_REJECTIONS) {
        Text(stringResource(R.string.import_rejected_more, rejected.size - SHOWN_REJECTIONS))
    }
}

@Composable
private fun rejectionText(rejection: ImportRejection): String =
    when (rejection.reason) {
        RejectionReason.UNREADABLE ->
            stringResource(
                R.string.import_rejected_unreadable,
                rejection.gameNumber,
                rejection.line ?: 0,
                rejection.detail,
            )
        RejectionReason.DIFFERENT_START -> stringResource(R.string.import_rejected_start, rejection.gameNumber)
    }

@Composable
private fun ImportMessage(
    message: String,
    onAction: (EditorAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(EditorAction.CloseImport) },
        title = { Text(stringResource(R.string.import_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { onAction(EditorAction.CloseImport) }) { Text(stringResource(R.string.import_close)) }
        },
    )
}

/** The text of the file at [uri], or null if it cannot be read or is too big. */
private fun readPgn(
    context: Context,
    uri: Uri,
): String? =
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readNBytes(MAX_FILE_BYTES + 1)
            if (bytes.size > MAX_FILE_BYTES) null else bytes.toString(Charsets.UTF_8)
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

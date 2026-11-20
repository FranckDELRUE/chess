package com.chesscompanion.feature.repertoire

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.data.RepertoireSummary
import com.chesscompanion.core.ui.theme.ChessTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

const val REPERTOIRE_NEW_TAG = "repertoire-new"
const val REPERTOIRE_NAME_FIELD_TAG = "repertoire-name-field"
const val REPERTOIRE_CONFIRM_TAG = "repertoire-confirm"
const val REPERTOIRE_EMPTY_TAG = "repertoire-empty"
const val REPERTOIRE_EXPORT_RESULT_TAG = "repertoire-export-result"

/** What a row can open; a null callback hides its button. */
data class RepertoireRowActions(
    val onOpen: ((String) -> Unit)? = null,
    val onDrill: ((String) -> Unit)? = null,
    val onGaps: ((String) -> Unit)? = null,
    val onExport: ((String) -> Unit)? = null,
)

/**
 * The list of repertoires. [onOpen] opens one; while it is null the rows are not tappable. "Exporter" writes the
 * repertoire as a PGN file chosen by the player (Storage Access Framework, no storage permission).
 */
@Composable
fun RepertoireListScreen(
    onOpen: ((String) -> Unit)? = null,
    onDrill: ((String) -> Unit)? = null,
    onGaps: ((String) -> Unit)? = null,
    viewModel: RepertoireListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf<String?>(null) }
    var exported by remember { mutableStateOf<Boolean?>(null) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-chess-pgn")) { uri ->
            val id = exporting
            exporting = null
            if (uri != null && id != null) {
                scope.launch { exported = writePgn(context, uri, viewModel.exportPgn(id)) }
            }
        }
    val actions =
        RepertoireRowActions(
            onOpen,
            onDrill,
            onGaps,
            onExport = { id ->
                exported = null
                exporting = id
                picker.launch(exportFileName(state.items.firstOrNull { it.id == id }?.name))
            },
        )
    RepertoireListContent(state, viewModel::onAction, actions, exported)
}

/** `Ma Sicilienne` becomes `Ma-Sicilienne.pgn`: no character a file system dislikes. */
internal fun exportFileName(name: String?): String {
    val clean =
        name
            .orEmpty()
            .trim()
            .replace(Regex("[^\\p{L}\\p{N}]+"), "-")
            .trim('-')
    return "${clean.ifEmpty { "repertoire" }}.pgn"
}

private suspend fun writePgn(
    context: Context,
    uri: Uri,
    text: String?,
): Boolean =
    text != null &&
        try {
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    ?: throw IOException("Cannot write $uri")
            }
            true
        } catch (_: IOException) {
            false
        }

@Composable
internal fun RepertoireListContent(
    state: RepertoireListUiState,
    onAction: (RepertoireListAction) -> Unit,
    actions: RepertoireRowActions = RepertoireRowActions(),
    exported: Boolean? = null,
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.repertoire_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = { onAction(RepertoireListAction.StartCreate) },
                    modifier = Modifier.testTag(REPERTOIRE_NEW_TAG),
                ) { Text(stringResource(R.string.repertoire_new)) }
            }
            exported?.let {
                Text(
                    stringResource(if (it) R.string.repertoire_exported else R.string.repertoire_export_failed),
                    Modifier.testTag(REPERTOIRE_EXPORT_RESULT_TAG),
                )
            }
            RepertoireItems(state, onAction, actions)
        }
    }
    state.dialog?.let { RepertoireDialogs(it, onAction) }
}

@Composable
private fun RepertoireItems(
    state: RepertoireListUiState,
    onAction: (RepertoireListAction) -> Unit,
    actions: RepertoireRowActions,
) {
    when {
        !state.loaded -> Text(stringResource(R.string.repertoire_loading))
        state.items.isEmpty() ->
            Text(stringResource(R.string.repertoire_empty), Modifier.testTag(REPERTOIRE_EMPTY_TAG))
        else ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items, key = { it.id }) { item -> RepertoireRow(item, onAction, actions) }
            }
    }
}

@Composable
private fun RepertoireRow(
    item: RepertoireSummary,
    onAction: (RepertoireListAction) -> Unit,
    actions: RepertoireRowActions,
) {
    val (onOpen, onDrill, onGaps) = actions
    val onExport = actions.onExport
    val rowModifier = Modifier.fillMaxWidth().testTag(repertoireRowTag(item.id))
    val content: @Composable () -> Unit = {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.repertoire_summary, sideLabel(item.side), moveCountLabel(item.moveCount)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (onDrill != null && item.moveCount > 0) {
                Button(
                    onClick = { onDrill(item.id) },
                    modifier = Modifier.testTag(repertoireDrillTag(item.id)),
                ) { Text(stringResource(R.string.repertoire_drill)) }
            }
            if (onGaps != null && item.moveCount > 0) {
                TextButton(
                    onClick = { onGaps(item.id) },
                    modifier = Modifier.testTag(repertoireGapsTag(item.id)),
                ) { Text(stringResource(R.string.repertoire_gaps)) }
            }
            if (onExport != null && item.moveCount > 0) {
                TextButton(
                    onClick = { onExport(item.id) },
                    modifier = Modifier.testTag(repertoireExportTag(item.id)),
                ) { Text(stringResource(R.string.repertoire_export)) }
            }
            TextButton(
                onClick = { onAction(RepertoireListAction.StartRename(item.id)) },
                modifier = Modifier.testTag(repertoireRenameTag(item.id)),
            ) { Text(stringResource(R.string.repertoire_rename)) }
            TextButton(
                onClick = { onAction(RepertoireListAction.StartDelete(item.id)) },
                modifier = Modifier.testTag(repertoireDeleteTag(item.id)),
            ) { Text(stringResource(R.string.repertoire_delete)) }
        }
    }
    if (onOpen == null) Card(rowModifier) { content() } else Card({ onOpen(item.id) }, rowModifier) { content() }
}

@Composable
internal fun sideLabel(side: Side): String =
    stringResource(if (side == Side.WHITE) R.string.repertoire_side_white else R.string.repertoire_side_black)

@Composable
private fun moveCountLabel(count: Int): String = pluralStringResource(R.plurals.repertoire_move_count, count, count)

@androidx.compose.ui.tooling.preview.Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun RepertoireListPreview() =
    ChessTheme {
        RepertoireListContent(
            RepertoireListUiState(
                items =
                    listOf(
                        RepertoireSummary("1", "Italienne", Side.WHITE, PREVIEW_MOVES, 0),
                        RepertoireSummary("2", "Sicilienne Najdorf", Side.BLACK, 1, 0),
                    ),
                loaded = true,
            ),
            {},
        )
    }

private const val PREVIEW_MOVES = 42

package com.chesscompanion.feature.repertoire

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.data.RepertoireSummary
import com.chesscompanion.core.ui.theme.ChessTheme

const val REPERTOIRE_NEW_TAG = "repertoire-new"
const val REPERTOIRE_NAME_FIELD_TAG = "repertoire-name-field"
const val REPERTOIRE_CONFIRM_TAG = "repertoire-confirm"
const val REPERTOIRE_EMPTY_TAG = "repertoire-empty"

/**
 * The list of repertoires. [onOpen] opens one (the editor arrives in S7-4); while it is null the
 * rows are not tappable.
 */
@Composable
fun RepertoireListScreen(
    onOpen: ((String) -> Unit)? = null,
    onDrill: ((String) -> Unit)? = null,
    onGaps: ((String) -> Unit)? = null,
    viewModel: RepertoireListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RepertoireListContent(state, viewModel::onAction, onOpen, onDrill, onGaps)
}

@Composable
internal fun RepertoireListContent(
    state: RepertoireListUiState,
    onAction: (RepertoireListAction) -> Unit,
    onOpen: ((String) -> Unit)? = null,
    onDrill: ((String) -> Unit)? = null,
    onGaps: ((String) -> Unit)? = null,
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
            RepertoireItems(state, onAction, onOpen, onDrill, onGaps)
        }
    }
    state.dialog?.let { RepertoireDialogs(it, onAction) }
}

@Composable
private fun RepertoireItems(
    state: RepertoireListUiState,
    onAction: (RepertoireListAction) -> Unit,
    onOpen: ((String) -> Unit)?,
    onDrill: ((String) -> Unit)?,
    onGaps: ((String) -> Unit)?,
) {
    when {
        !state.loaded -> Text(stringResource(R.string.repertoire_loading))
        state.items.isEmpty() ->
            Text(stringResource(R.string.repertoire_empty), Modifier.testTag(REPERTOIRE_EMPTY_TAG))
        else ->
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.items, key = { it.id }) { item -> RepertoireRow(item, onAction, onOpen, onDrill, onGaps) }
            }
    }
}

@Composable
private fun RepertoireRow(
    item: RepertoireSummary,
    onAction: (RepertoireListAction) -> Unit,
    onOpen: ((String) -> Unit)?,
    onDrill: ((String) -> Unit)?,
    onGaps: ((String) -> Unit)?,
) {
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

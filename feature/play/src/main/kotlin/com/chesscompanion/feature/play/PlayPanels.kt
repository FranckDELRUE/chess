package com.chesscompanion.feature.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Test tag of a move of the list, e.g. `move-0-0-1`. */
fun moveTestTag(path: List<Int>): String = "move-" + path.joinToString("-")

/** Test tags of the navigation buttons. */
const val NAV_START_TAG = "nav-start"
const val NAV_BACK_TAG = "nav-back"
const val NAV_FORWARD_TAG = "nav-forward"
const val NAV_END_TAG = "nav-end"

/** Test tag of the [index]-th game offered after a multi-game import. */
fun gameChoiceTestTag(index: Int): String = "game-choice-$index"

@Composable
internal fun NavigationBar(
    state: PlayUiState,
    onAction: (PlayAction) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NavButton("⏮", R.string.play_nav_start, NAV_START_TAG, state.canGoBack) { onAction(PlayAction.ToStart) }
        NavButton("◀", R.string.play_nav_back, NAV_BACK_TAG, state.canGoBack) { onAction(PlayAction.Back) }
        NavButton("▶", R.string.play_nav_forward, NAV_FORWARD_TAG, state.canGoForward) { onAction(PlayAction.Forward) }
        NavButton("⏭", R.string.play_nav_end, NAV_END_TAG, state.canGoForward) { onAction(PlayAction.ToEnd) }
    }
}

@Composable
private fun NavButton(
    symbol: String,
    description: Int,
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(description)
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.testTag(tag).semantics { contentDescription = label },
    ) { Text(symbol) }
}

/** Moves in French notation; variations smaller and lighter, the current move highlighted. */
@Composable
internal fun MoveList(
    items: List<MoveListItem>,
    onGoTo: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEach { item ->
            val style = if (item.depth == 0) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium
            val dim = MaterialTheme.colorScheme.onSurfaceVariant
            when (item) {
                is MoveListItem.Number -> Text(item.text, style = style, color = dim)
                is MoveListItem.Move ->
                    Text(
                        text = item.text,
                        style = style,
                        fontWeight = if (item.depth == 0) FontWeight.SemiBold else FontWeight.Normal,
                        modifier =
                            Modifier
                                .testTag(moveTestTag(item.path))
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (item.current) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                ).clickable { onGoTo(item.path) }
                                .padding(horizontal = 4.dp),
                    )
                is MoveListItem.Comment -> Text(item.text, style = style, fontStyle = FontStyle.Italic, color = dim)
                is MoveListItem.VariationStart -> Text("(", style = style, color = dim)
                is MoveListItem.VariationEnd -> Text(")", style = style, color = dim)
            }
        }
    }
}

@Composable
internal fun GameChoiceDialog(
    games: List<GameSummary>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.play_choose_game)) },
        text = {
            LazyColumn {
                itemsIndexed(games) { index, game ->
                    Text(
                        text =
                            stringResource(
                                R.string.play_game_summary,
                                game.white,
                                game.black,
                                game.date,
                                game.result,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .testTag(gameChoiceTestTag(index))
                                .clickable { onPick(index) }
                                .padding(vertical = 8.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.play_cancel)) } },
    )
}

/** Snackbar text of a [PlayMessage]. */
@Composable
internal fun messageText(message: PlayMessage): String =
    when (message) {
        is PlayMessage.Imported -> {
            val imported = pluralStringResource(R.plurals.play_msg_imported, message.games, message.games)
            if (message.errors == 0) {
                imported
            } else {
                pluralStringResource(R.plurals.play_msg_import_errors, message.errors, imported, message.errors)
            }
        }
        is PlayMessage.ImportFailed ->
            message.error?.let {
                stringResource(R.string.play_msg_import_failed, it.gameNumber, it.line, it.message)
            } ?: stringResource(R.string.play_msg_nothing_to_import)
        PlayMessage.PositionLoaded -> stringResource(R.string.play_msg_position_loaded)
        PlayMessage.Saved -> stringResource(R.string.play_msg_saved)
        PlayMessage.EngineFailed -> stringResource(R.string.play_msg_engine_failed)
        is PlayMessage.FileError -> stringResource(R.string.play_msg_file_error, message.detail)
    }

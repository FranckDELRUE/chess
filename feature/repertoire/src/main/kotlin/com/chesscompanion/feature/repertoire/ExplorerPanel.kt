package com.chesscompanion.feature.repertoire

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.chesscompanion.core.data.explorer.ExplorerLevel
import com.chesscompanion.core.data.explorer.ExplorerProblem
import java.text.NumberFormat

const val EXPLORER_EVAL_TAG = "explorer-eval"
const val EXPLORER_PROBLEM_TAG = "explorer-problem"
const val EXPLORER_TOKEN_TAG = "explorer-token"
const val EXPLORER_TOKEN_SAVE_TAG = "explorer-token-save"
const val EXPLORER_TOKEN_CLEAR_TAG = "explorer-token-clear"
const val EXPLORER_CACHE_TAG = "explorer-cache"

fun explorerLevelTag(level: ExplorerLevel) = "explorer-level-${level.name.lowercase()}"

fun explorerRowTag(uci: String) = "explorer-row-$uci"

fun explorerPlayTag(uci: String) = "explorer-play-$uci"

private val WHITE_BAR = Color(0xFFEDEDED)
private val DRAW_BAR = Color(0xFF8A8A8A)
private val BLACK_BAR = Color(0xFF1F1F1F)
private const val BAR_HEIGHT = 8

/** The explorer section of the editor panel: evaluation, level, statistics of the moves, token. */
@Composable
internal fun ExplorerSection(
    state: ExplorerUiState,
    onAction: (EditorAction) -> Unit,
) {
    val explorer = { action: ExplorerAction -> onAction(EditorAction.Explorer(action)) }
    Text(stringResource(R.string.explorer_title), style = MaterialTheme.typography.titleMedium)
    state.evaluation?.let {
        Text(stringResource(R.string.explorer_evaluation, it), Modifier.testTag(EXPLORER_EVAL_TAG))
    }
    LevelRow(state.level) { explorer(ExplorerAction.SetLevel(it)) }
    when (val content = state.content) {
        ExplorerContent.Loading -> Text(stringResource(R.string.explorer_loading))
        is ExplorerContent.Loaded -> Moves(content, onAction)
        is ExplorerContent.Problem -> Problem(content.problem, explorer)
    }
    if (state.hasToken) {
        TextButton(
            onClick = { explorer(ExplorerAction.ClearToken) },
            modifier = Modifier.testTag(EXPLORER_TOKEN_CLEAR_TAG),
        ) { Text(stringResource(R.string.explorer_token_clear)) }
    }
}

@Composable
private fun LevelRow(
    level: ExplorerLevel,
    onPick: (ExplorerLevel) -> Unit,
) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ExplorerLevel.entries.forEach { entry ->
            FilterChip(
                selected = entry == level,
                onClick = { onPick(entry) },
                label = { Text(stringResource(entry.labelRes())) },
                modifier = Modifier.testTag(explorerLevelTag(entry)),
            )
        }
    }
}

@Composable
private fun Moves(
    content: ExplorerContent.Loaded,
    onAction: (EditorAction) -> Unit,
) {
    if (content.fromCache) Text(stringResource(R.string.explorer_cached), Modifier.testTag(EXPLORER_CACHE_TAG))
    if (content.rows.isEmpty()) {
        Text(stringResource(R.string.explorer_no_move))
        return
    }
    Text(stringResource(R.string.explorer_games, groupDigits(content.games)))
    content.rows.forEach { row ->
        Column(
            Modifier.fillMaxWidth().testTag(explorerRowTag(row.uci)),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(row.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.explorer_row_games, groupDigits(row.games)))
                Text(stringResource(R.string.explorer_row_shares, row.whitePercent, row.drawPercent, row.blackPercent))
                OutlinedButton(
                    onClick = { onAction(EditorAction.PlayUci(row.uci)) },
                    modifier = Modifier.testTag(explorerPlayTag(row.uci)),
                ) { Text(stringResource(if (row.known) R.string.explorer_follow else R.string.explorer_add)) }
            }
            ResultBar(row)
        }
    }
}

/** A count with the thousands separator of the language of the device (S7-8a). */
private fun groupDigits(count: Long): String = NumberFormat.getIntegerInstance().format(count)

@Composable
private fun ResultBar(row: ExplorerRow) {
    val parts = listOf(row.whitePercent to WHITE_BAR, row.drawPercent to DRAW_BAR, row.blackPercent to BLACK_BAR)
    Row(Modifier.fillMaxWidth().height(BAR_HEIGHT.dp)) {
        parts.filter { it.first > 0 }.forEach { (share, color) ->
            Box(Modifier.weight(share.toFloat()).height(BAR_HEIGHT.dp).background(color))
        }
    }
}

@Composable
private fun Problem(
    problem: ExplorerProblem,
    onAction: (ExplorerAction) -> Unit,
) {
    Text(stringResource(problem.messageRes()), Modifier.testTag(EXPLORER_PROBLEM_TAG))
    if (problem == ExplorerProblem.NO_TOKEN || problem == ExplorerProblem.BAD_TOKEN) {
        var token by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text(stringResource(R.string.explorer_token)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag(EXPLORER_TOKEN_TAG),
        )
        OutlinedButton(
            onClick = {
                onAction(ExplorerAction.SaveToken(token))
                token = ""
            },
            enabled = token.isNotBlank(),
            modifier = Modifier.testTag(EXPLORER_TOKEN_SAVE_TAG),
        ) { Text(stringResource(R.string.explorer_token_save)) }
    }
}

private fun ExplorerLevel.labelRes(): Int =
    when (this) {
        ExplorerLevel.BEGINNER -> R.string.explorer_level_beginner
        ExplorerLevel.CLUB -> R.string.explorer_level_club
        ExplorerLevel.STRONG -> R.string.explorer_level_strong
        ExplorerLevel.MASTER -> R.string.explorer_level_master
    }

private fun ExplorerProblem.messageRes(): Int =
    when (this) {
        ExplorerProblem.NO_TOKEN -> R.string.explorer_no_token
        ExplorerProblem.BAD_TOKEN -> R.string.explorer_bad_token
        ExplorerProblem.RATE_LIMITED -> R.string.explorer_rate_limited
        ExplorerProblem.OFFLINE -> R.string.explorer_offline
        ExplorerProblem.SERVER -> R.string.explorer_server
    }

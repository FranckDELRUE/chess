package com.chesscompanion.feature.training

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

@Composable
internal fun ReplyPanel(state: DrillUiState) {
    LineHeader(state)
    Text(stringResource(R.string.drill_replying))
}

@Composable
internal fun LearningPanel(state: DrillUiState) {
    Text(stringResource(R.string.drill_remaining, state.remaining), style = MaterialTheme.typography.labelLarge)
    Text(
        stringResource(R.string.drill_line, state.lineText.ifEmpty { stringResource(R.string.drill_line_start) }),
        Modifier.testTag(DRILL_LINE_TAG),
    )
    Text(
        stringResource(R.string.drill_learn_title),
        Modifier.testTag(DRILL_LEARN_TAG),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        stringResource(R.string.drill_learn_moves, state.learnMoves.joinToString(" / ")),
        Modifier.testTag(DRILL_LEARN_MOVES_TAG),
    )
    if (state.learnComment.isNotEmpty()) Text(state.learnComment)
    state.learnSource?.let {
        Text(stringResource(R.string.drill_learn_source, it), style = MaterialTheme.typography.bodySmall)
    }
    if (state.learnMiss) {
        Text(stringResource(R.string.drill_learn_miss), Modifier.testTag(DRILL_LEARN_MISS_TAG))
    }
}

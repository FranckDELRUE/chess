package com.chesscompanion.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chesscompanion.core.ui.board.ChessBoard
import com.chesscompanion.core.ui.board.PromotionDialog
import java.util.Locale

const val LEARN_NOT_FOUND_TAG = "learn-not-found"
const val LEARN_EMPTY_TAG = "learn-empty"
const val LEARN_PROGRESS_TAG = "learn-progress"
const val LEARN_LINE_TAG = "learn-line"
const val LEARN_FACTS_TAG = "learn-facts"
const val LEARN_MISS_TAG = "learn-miss"
const val LEARN_LEARNED_TAG = "learn-learned"
const val LEARN_NEXT_TAG = "learn-next"
const val LEARN_PREVIOUS_TAG = "learn-previous"
const val LEARN_QUIT_TAG = "learn-quit"

fun learnFactTag(index: Int) = "learn-fact-$index"

/** The opening lesson of one repertoire: the board on one side, the move to learn and its explanation on the other. */
@Composable
fun LearnScreen(
    onClose: () -> Unit,
    viewModel: LearnViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LearnContent(state, viewModel::onAction, onClose)
}

@Composable
internal fun LearnContent(
    state: LearnUiState,
    onAction: (LearnAction) -> Unit,
    onClose: () -> Unit = {},
) {
    Surface(Modifier.fillMaxSize()) {
        when (state.status) {
            LearnStatus.LOADING -> Text(stringResource(R.string.drill_loading), Modifier.padding(16.dp))
            LearnStatus.NOT_FOUND ->
                Text(stringResource(R.string.drill_not_found), Modifier.padding(16.dp).testTag(LEARN_NOT_FOUND_TAG))
            LearnStatus.EMPTY -> LearnEmpty(state, onClose)
            LearnStatus.LESSON -> LearnLayout(state, onAction, onClose)
        }
    }
    state.promotionSide?.let { side ->
        PromotionDialog(
            side,
            { onAction(LearnAction.Gesture(DrillAction.Promote(it))) },
            { onAction(LearnAction.Gesture(DrillAction.CancelPromotion)) },
        )
    }
}

@Composable
private fun LearnEmpty(
    state: LearnUiState,
    onClose: () -> Unit,
) {
    Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.learn_title, state.name), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.learn_empty), Modifier.testTag(LEARN_EMPTY_TAG))
        OutlinedButton(onClick = onClose, modifier = Modifier.testTag(LEARN_QUIT_TAG)) {
            Text(stringResource(R.string.learn_quit))
        }
    }
}

@Composable
private fun LearnLayout(
    state: LearnUiState,
    onAction: (LearnAction) -> Unit,
    onClose: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        val board = @Composable { modifier: Modifier ->
            ChessBoard(
                board = state.board,
                flipped = state.flipped,
                onSquareTap = { onAction(LearnAction.Gesture(DrillAction.SquareTap(it))) },
                onDragStart = { onAction(LearnAction.Gesture(DrillAction.DragStart(it))) },
                onMove = { from, to -> onAction(LearnAction.Gesture(DrillAction.Drop(from, to))) },
                modifier = modifier,
                highlights = state.highlights,
                annotations = state.annotations,
            )
        }
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                board(Modifier.weight(1f).fillMaxSize())
                LearnPanel(state, onAction, onClose, Modifier.weight(1f).fillMaxSize())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                board(Modifier.fillMaxWidth().weight(1f))
                LearnPanel(state, onAction, onClose, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun LearnPanel(
    state: LearnUiState,
    onAction: (LearnAction) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.learn_title, state.name), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.learn_progress, state.index + 1, state.total, state.learned),
            Modifier.testTag(LEARN_PROGRESS_TAG),
            style = MaterialTheme.typography.labelLarge,
        )
        LinearProgressIndicator(
            progress = { state.learned.toFloat() / state.total },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.drill_line, state.lineText.ifEmpty { stringResource(R.string.drill_line_start) }),
            Modifier.testTag(LEARN_LINE_TAG),
        )
        LearnTask(state)
        LearnFacts(state)
        if (state.comment.isNotEmpty()) Text(state.comment)
        state.source?.let {
            Text(stringResource(R.string.drill_learn_source, it), style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onAction(LearnAction.Previous) },
                enabled = state.index > 0,
                modifier = Modifier.testTag(LEARN_PREVIOUS_TAG),
            ) { Text(stringResource(R.string.learn_previous)) }
            Button(
                onClick = { onAction(LearnAction.Next) },
                enabled = state.index < state.total - 1,
                modifier = Modifier.testTag(LEARN_NEXT_TAG),
            ) { Text(stringResource(R.string.learn_next)) }
        }
        OutlinedButton(onClick = onClose, modifier = Modifier.testTag(LEARN_QUIT_TAG)) {
            Text(stringResource(R.string.learn_quit))
        }
    }
}

@Composable
private fun LearnTask(state: LearnUiState) {
    val moves = state.facts.joinToString(" / ") { it.san }
    when {
        state.justLearned ->
            Text(
                stringResource(R.string.learn_learned_now, moves),
                Modifier.testTag(LEARN_LEARNED_TAG),
                style = MaterialTheme.typography.titleMedium,
            )
        state.stepLearned ->
            Text(
                stringResource(R.string.learn_already, moves),
                Modifier.testTag(LEARN_LEARNED_TAG),
                style = MaterialTheme.typography.titleMedium,
            )
        else ->
            Text(
                stringResource(R.string.learn_prompt, moves),
                style = MaterialTheme.typography.titleMedium,
            )
    }
    if (state.miss) Text(stringResource(R.string.drill_learn_miss), Modifier.testTag(LEARN_MISS_TAG))
}

@Composable
private fun LearnFacts(state: LearnUiState) {
    Column(Modifier.fillMaxWidth().testTag(LEARN_FACTS_TAG), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.learn_facts_title), style = MaterialTheme.typography.titleSmall)
        val missing = if (state.evaluationsDone) R.string.drill_eval_unavailable else R.string.drill_eval_pending
        state.facts.forEachIndexed { i, fact ->
            Column(Modifier.testTag(learnFactTag(i))) {
                Text(fact.san, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.learn_fact_evaluation, fact.evaluation ?: stringResource(missing)))
                fact.games?.let { Text(statsText(fact, it)) }
            }
        }
        when {
            state.explorerProblem != null ->
                Text(
                    stringResource(
                        R.string.learn_stats_unavailable,
                        stringResource(problemText(state.explorerProblem)),
                    ),
                )
            !state.explorerLoaded -> Text(stringResource(R.string.learn_stats_pending))
        }
        Text(stringResource(R.string.learn_facts_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun statsText(
    fact: MoveFact,
    games: Long,
): String =
    if (games == 0L) {
        stringResource(R.string.learn_stats_never)
    } else {
        stringResource(
            R.string.learn_stats,
            fact.percent ?: 0,
            String.format(Locale.FRANCE, "%,d", games),
            fact.whitePercent ?: 0,
            fact.drawPercent ?: 0,
            fact.blackPercent ?: 0,
        )
    }

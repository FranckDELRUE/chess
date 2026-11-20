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
import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.ui.board.ChessBoard
import com.chesscompanion.core.ui.board.PromotionDialog

const val DRILL_NOT_FOUND_TAG = "drill-not-found"
const val DRILL_QUESTION_TAG = "drill-question"
const val DRILL_LEARN_TAG = "drill-learn"
const val DRILL_LEARN_MOVES_TAG = "drill-learn-moves"
const val DRILL_LEARN_MISS_TAG = "drill-learn-miss"
const val DRILL_LEARNED_TAG = "drill-learned"
const val DRILL_HINT_BUTTON_TAG = "drill-hint-button"
const val DRILL_HINT_TAG = "drill-hint"
const val DRILL_FEEDBACK_TAG = "drill-feedback"
const val DRILL_LINE_TAG = "drill-line"
const val DRILL_EVAL_PLAYED_TAG = "drill-eval-played"
const val DRILL_EVAL_EXPECTED_TAG = "drill-eval-expected"
const val DRILL_CONTINUE_TAG = "drill-continue"
const val DRILL_FINISHED_TAG = "drill-finished"
const val DRILL_SUMMARY_TAG = "drill-summary"
const val DRILL_LEARNED_SUMMARY_TAG = "drill-learned-summary"

/** The drill of one repertoire: the board on one side, the question and the feedback on the other. */
@Composable
fun DrillScreen(viewModel: DrillViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DrillContent(state, viewModel::onAction)
}

@Composable
internal fun DrillContent(
    state: DrillUiState,
    onAction: (DrillAction) -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        when (state.status) {
            DrillStatus.LOADING -> Text(stringResource(R.string.drill_loading), Modifier.padding(16.dp))
            DrillStatus.NOT_FOUND ->
                Text(stringResource(R.string.drill_not_found), Modifier.padding(16.dp).testTag(DRILL_NOT_FOUND_TAG))
            else -> DrillLayout(state, onAction)
        }
    }
    state.promotionSide?.let { side ->
        PromotionDialog(side, { onAction(DrillAction.Promote(it)) }, { onAction(DrillAction.CancelPromotion) })
    }
}

@Composable
private fun DrillLayout(
    state: DrillUiState,
    onAction: (DrillAction) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        val board = @Composable { modifier: Modifier -> DrillBoard(state, onAction, modifier) }
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                board(Modifier.weight(1f).fillMaxSize())
                DrillPanel(state, onAction, Modifier.weight(1f).fillMaxSize())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                board(Modifier.fillMaxWidth().weight(1f))
                DrillPanel(state, onAction, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun DrillBoard(
    state: DrillUiState,
    onAction: (DrillAction) -> Unit,
    modifier: Modifier,
) {
    ChessBoard(
        board = state.board,
        flipped = state.flipped,
        onSquareTap = { onAction(DrillAction.SquareTap(it)) },
        onDragStart = { onAction(DrillAction.DragStart(it)) },
        onMove = { from, to -> onAction(DrillAction.Drop(from, to)) },
        modifier = modifier,
        highlights = state.highlights,
        annotations = state.annotations,
    )
}

@Composable
private fun DrillPanel(
    state: DrillUiState,
    onAction: (DrillAction) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.drill_title, state.name), style = MaterialTheme.typography.headlineSmall)
        when (state.status) {
            DrillStatus.LEARNING -> LearningPanel(state)
            DrillStatus.ASKING -> AskingPanel(state, onAction)
            DrillStatus.FEEDBACK -> FeedbackPanel(state, onAction)
            else -> FinishedPanel(state)
        }
    }
}

@Composable
private fun LineHeader(state: DrillUiState) {
    Text(stringResource(R.string.drill_remaining, state.remaining), style = MaterialTheme.typography.labelLarge)
    if (state.isNewPosition) Text(stringResource(R.string.drill_new_position))
    if (state.isRetry) Text(stringResource(R.string.drill_retry))
    Text(
        stringResource(
            R.string.drill_line,
            state.lineText.ifEmpty { stringResource(R.string.drill_line_start) },
        ),
        Modifier.testTag(DRILL_LINE_TAG),
    )
}

@Composable
private fun LearningPanel(state: DrillUiState) {
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

@Composable
private fun AskingPanel(
    state: DrillUiState,
    onAction: (DrillAction) -> Unit,
) {
    LineHeader(state)
    Text(
        stringResource(if (state.toMove == Side.WHITE) R.string.drill_ask_white else R.string.drill_ask_black),
        Modifier.testTag(DRILL_QUESTION_TAG),
        style = MaterialTheme.typography.titleMedium,
    )
    val hint = state.hint
    if (hint == null) {
        OutlinedButton(
            onClick = { onAction(DrillAction.ShowHint) },
            modifier = Modifier.testTag(DRILL_HINT_BUTTON_TAG),
        ) { Text(stringResource(R.string.drill_hint)) }
    } else {
        Text(stringResource(R.string.drill_hint_shown, hint), Modifier.testTag(DRILL_HINT_TAG))
    }
}

@Composable
private fun FeedbackPanel(
    state: DrillUiState,
    onAction: (DrillAction) -> Unit,
) {
    val feedback = state.feedback ?: return
    Column(Modifier.fillMaxWidth().testTag(DRILL_FEEDBACK_TAG), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (feedback.learned) {
            Text(
                stringResource(R.string.drill_learned, feedback.playedSan),
                Modifier.testTag(DRILL_LEARNED_TAG),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.drill_back_tomorrow))
        } else if (feedback.correct) {
            Text(
                stringResource(R.string.drill_correct, feedback.playedSan),
                style = MaterialTheme.typography.titleMedium,
            )
        } else {
            Text(
                stringResource(R.string.drill_wrong, feedback.playedSan, feedback.expectedSans.joinToString(" / ")),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(stringResource(R.string.drill_repertoire_line, feedback.line), Modifier.testTag(DRILL_LINE_TAG))
            EvaluationRows(feedback)
            if (!feedback.retry) Text(stringResource(R.string.drill_back_tomorrow))
        }
    }
    Button(onClick = { onAction(DrillAction.Continue) }, modifier = Modifier.testTag(DRILL_CONTINUE_TAG)) {
        Text(stringResource(R.string.drill_continue))
    }
}

@Composable
private fun EvaluationRows(feedback: DrillFeedback) {
    val pending = stringResource(R.string.drill_eval_pending)
    val unavailable = stringResource(R.string.drill_eval_unavailable)

    fun text(value: String?) = value ?: if (feedback.evaluationDone) unavailable else pending
    Text(
        stringResource(R.string.drill_eval_played, feedback.playedSan, text(feedback.playedEvaluation)),
        Modifier.testTag(DRILL_EVAL_PLAYED_TAG),
    )
    Text(
        stringResource(
            R.string.drill_eval_expected,
            feedback.expectedSans.first(),
            text(feedback.expectedEvaluation),
        ),
        Modifier.testTag(DRILL_EVAL_EXPECTED_TAG),
    )
    Text(stringResource(R.string.drill_eval_note), style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun FinishedPanel(state: DrillUiState) {
    Column(Modifier.testTag(DRILL_FINISHED_TAG), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.summary.learned > 0) {
            Text(
                stringResource(R.string.drill_summary_learned, state.summary.learned),
                Modifier.testTag(DRILL_LEARNED_SUMMARY_TAG),
            )
        }
        if (state.nothingToReview) {
            Text(stringResource(R.string.drill_nothing_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.drill_nothing_message))
        } else {
            Text(stringResource(R.string.drill_finished_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(
                    R.string.drill_summary,
                    state.summary.graded,
                    state.summary.correct,
                    state.summary.missed,
                ),
                Modifier.testTag(DRILL_SUMMARY_TAG),
            )
        }
        if (state.leftDue > 0 || state.leftNew > 0) {
            Text(stringResource(R.string.drill_left, state.leftDue, state.leftNew))
        }
    }
}

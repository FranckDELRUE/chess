package com.chesscompanion.feature.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Test tags of the analysis widgets. */
const val ANALYSIS_SWITCH_TAG = "analysis-switch"
const val EVAL_BAR_TAG = "eval-bar"

/** Test tag of the [rank]-th analysis line (1 = best). */
fun analysisLineTestTag(rank: Int): String = "analysis-line-$rank"

private val EVAL_BAR_WIDTH = 28.dp
private val EVAL_BAR_GAP = 8.dp
private const val MIN_SHARE = 0.02f
private const val HALF = 0.5f
private val WHITE_PART = Color.White
private val BLACK_PART = Color(0xFF262626)

/**
 * The board, with the evaluation bar on its left when the analysis is on. The bar is as tall as
 * the (square) board, so both are sized together from the space available.
 */
@Composable
internal fun BoardWithEvalBar(
    analysis: AnalysisState,
    flipped: Boolean,
    modifier: Modifier = Modifier,
    board: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val extra = if (analysis.enabled) EVAL_BAR_WIDTH + EVAL_BAR_GAP else 0.dp
        val side = minOf(maxWidth - extra, maxHeight)
        Row(Modifier.size(width = side + extra, height = side)) {
            if (analysis.enabled) {
                EvalBar(analysis, flipped, Modifier.width(EVAL_BAR_WIDTH).fillMaxHeight())
                Spacer(Modifier.width(EVAL_BAR_GAP))
            }
            board(Modifier.size(side))
        }
    }
}

/** White's part grows from the White side of the board; the score is written on the larger part. */
@Composable
internal fun EvalBar(
    analysis: AnalysisState,
    flipped: Boolean,
    modifier: Modifier = Modifier,
) {
    val share = analysis.whiteShare.coerceIn(MIN_SHARE, 1f - MIN_SHARE)
    val label = analysis.best?.let { formatScore(it.score) }
    val description =
        if (label == null) {
            stringResource(R.string.play_analysis_eval_bar_waiting)
        } else {
            stringResource(R.string.play_analysis_eval_bar, label)
        }
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier
            .testTag(EVAL_BAR_TAG)
            .semantics { contentDescription = description }
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
    ) {
        val whitePart = @Composable {
            Box(Modifier.weight(share).fillMaxWidth().background(WHITE_PART), contentAlignment = Alignment.Center) {
                if (label != null && share >= HALF) EvalLabel(label, BLACK_PART)
            }
        }
        val blackPart = @Composable {
            Box(
                Modifier.weight(1f - share).fillMaxWidth().background(BLACK_PART),
                contentAlignment = Alignment.Center,
            ) {
                if (label != null && share < HALF) EvalLabel(label, WHITE_PART)
            }
        }
        if (flipped) {
            whitePart()
            blackPart()
        } else {
            blackPart()
            whitePart()
        }
    }
}

@Composable
private fun EvalLabel(
    text: String,
    color: Color,
) = Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)

/** Switch and the best lines (up to three); a line is tapped to play its first move. */
@Composable
internal fun AnalysisPanel(
    analysis: AnalysisState,
    onAction: (PlayAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Switch(
                checked = analysis.enabled,
                onCheckedChange = { onAction(PlayAction.ToggleAnalysis) },
                modifier = Modifier.testTag(ANALYSIS_SWITCH_TAG),
            )
            Text(stringResource(R.string.play_analysis))
            if (analysis.enabled && analysis.depth > 0) {
                Text(
                    stringResource(R.string.play_analysis_depth, analysis.depth),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        if (analysis.enabled) AnalysisBody(analysis, onAction)
    }
}

@Composable
private fun AnalysisBody(
    analysis: AnalysisState,
    onAction: (PlayAction) -> Unit,
) {
    when {
        analysis.gameOver -> InfoText(stringResource(R.string.play_analysis_game_over))
        analysis.failed -> InfoText(stringResource(R.string.play_analysis_failed))
        analysis.lines.isEmpty() -> InfoText(stringResource(R.string.play_analysis_waiting))
        else -> analysis.lines.forEach { line -> AnalysisLineRow(line, onAction) }
    }
}

@Composable
private fun InfoText(text: String) = Text(text, style = MaterialTheme.typography.bodyMedium)

@Composable
private fun AnalysisLineRow(
    line: AnalysisLine,
    onAction: (PlayAction) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(analysisLineTestTag(line.rank))
            .clip(RoundedCornerShape(4.dp))
            .clickable { onAction(PlayAction.PlayAnalysisMove(line.firstMove)) }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(formatScore(line.score), fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp))
        Text(line.text(), style = MaterialTheme.typography.bodyMedium, maxLines = 2)
    }
}

package com.chesscompanion.feature.play

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chesscompanion.core.engine.EngineStrength
import java.util.Locale
import kotlin.math.roundToInt

/** Test tags of the game against Stockfish. */
const val VS_TAKE_BACK_TAG = "vs-take-back"
const val VS_RESIGN_TAG = "vs-resign"
const val SETUP_START_TAG = "setup-start"
const val SETUP_ELO_TAG = "setup-strength-elo"
const val SETUP_SKILL_TAG = "setup-strength-skill"
const val SETUP_SLIDER_TAG = "setup-slider"

fun setupColorTestTag(color: PlayerColor): String = "setup-color-${color.name.lowercase()}"

fun setupTimeTestTag(millis: Long): String = "setup-time-$millis"

private const val ELO_STEP = 10

/** "Reprendre un coup" and "Abandonner", shown while a game against Stockfish is on screen. */
@Composable
internal fun OpponentControls(
    opponent: OpponentUi,
    onAction: (PlayAction) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(
            onClick = { onAction(PlayAction.TakeBack) },
            modifier = Modifier.testTag(VS_TAKE_BACK_TAG),
        ) { Text(stringResource(R.string.play_vs_take_back)) }
        OutlinedButton(
            onClick = { onAction(PlayAction.Resign) },
            enabled = !opponent.over,
            modifier = Modifier.testTag(VS_RESIGN_TAG),
        ) { Text(stringResource(R.string.play_vs_resign)) }
    }
}

/** Colour, strength (ELO limit or skill level) and time per move; "Jouer" starts a new game. */
@Composable
internal fun OpponentSetupDialog(
    initial: OpponentSettings,
    onStart: (OpponentSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var color by remember { mutableStateOf(initial.color) }
    var byElo by remember { mutableStateOf(initial.strength is EngineStrength.Elo) }
    var elo by remember {
        mutableFloatStateOf(
            (
                (initial.strength as? EngineStrength.Elo)?.elo
                    ?: OpponentSettings.DEFAULT_ELO
            ).toFloat(),
        )
    }
    var skill by remember {
        mutableFloatStateOf(((initial.strength as? EngineStrength.Skill)?.level ?: DEFAULT_SKILL).toFloat())
    }
    var time by remember { mutableStateOf(initial.moveTimeMs) }
    val strength: EngineStrength =
        if (byElo) {
            EngineStrength.Elo(snapElo(elo))
        } else {
            EngineStrength.Skill(skill.roundToInt().coerceIn(EngineStrength.MIN_SKILL, EngineStrength.MAX_SKILL))
        }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.play_vs_setup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SetupLabel(R.string.play_vs_color)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlayerColor.entries.forEach { option ->
                        SetupChip(colorLabel(option), color == option, setupColorTestTag(option)) { color = option }
                    }
                }
                SetupLabel(R.string.play_vs_strength)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SetupChip(stringResource(R.string.play_vs_strength_elo), byElo, SETUP_ELO_TAG) { byElo = true }
                    SetupChip(
                        stringResource(R.string.play_vs_strength_skill),
                        !byElo,
                        SETUP_SKILL_TAG,
                    ) { byElo = false }
                }
                if (byElo) {
                    Text(stringResource(R.string.play_vs_elo_value, snapElo(elo)))
                    Slider(
                        value = elo,
                        onValueChange = { elo = it },
                        valueRange = EngineStrength.MIN_ELO.toFloat()..EngineStrength.MAX_ELO.toFloat(),
                        modifier = Modifier.testTag(SETUP_SLIDER_TAG),
                    )
                    Text(stringResource(R.string.play_vs_elo_note), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(stringResource(R.string.play_vs_skill_value, skill.roundToInt()))
                    Slider(
                        value = skill,
                        onValueChange = { skill = it },
                        valueRange = EngineStrength.MIN_SKILL.toFloat()..EngineStrength.MAX_SKILL.toFloat(),
                        modifier = Modifier.testTag(SETUP_SLIDER_TAG),
                    )
                }
                SetupLabel(R.string.play_vs_time)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OpponentSettings.MOVE_TIMES_MS.forEach { option ->
                        SetupChip(secondsLabel(option), time == option, setupTimeTestTag(option)) { time = option }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onStart(OpponentSettings(color, strength, time)) },
                modifier = Modifier.testTag(SETUP_START_TAG),
            ) { Text(stringResource(R.string.play_vs_start)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.play_vs_cancel)) } },
    )
}

@Composable
private fun SetupLabel(label: Int) = Text(stringResource(label), style = MaterialTheme.typography.titleSmall)

@Composable
private fun SetupChip(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
) = FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = Modifier.testTag(tag))

@Composable
private fun colorLabel(color: PlayerColor): String =
    stringResource(
        when (color) {
            PlayerColor.WHITE -> R.string.play_vs_color_white
            PlayerColor.BLACK -> R.string.play_vs_color_black
            PlayerColor.RANDOM -> R.string.play_vs_color_random
        },
    )

/** "0,5 s", "1 s", "3 s". */
@Composable
private fun secondsLabel(millis: Long): String {
    val seconds =
        if (millis % MS_PER_S ==
            0L
        ) {
            (millis / MS_PER_S).toString()
        } else {
            String.format(Locale.FRANCE, "%.1f", millis / MS_PER_S.toDouble())
        }
    return stringResource(R.string.play_vs_time_value, seconds)
}

/** The slider value rounded to a multiple of 10 inside the range Stockfish accepts. */
private fun snapElo(value: Float): Int =
    ((value / ELO_STEP).roundToInt() * ELO_STEP).coerceIn(EngineStrength.MIN_ELO, EngineStrength.MAX_ELO)

private const val DEFAULT_SKILL = 5
private const val MS_PER_S = 1_000L

package com.chesscompanion.feature.play

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chesscompanion.core.ui.board.AnnotationColor
import com.chesscompanion.core.ui.board.BoardColors
import com.chesscompanion.core.ui.board.nameRes

/** Test tag of the "Annoter" switch. */
const val ANNOTATE_SWITCH_TAG = "annotate-switch"

/** Test tag of a colour of the annotation bar, e.g. `annotation-color-red`. */
fun annotationColorTestTag(color: AnnotationColor): String = "annotation-color-${color.name.lowercase()}"

private const val DOT_SIZE = 32
private const val SELECTED_BORDER = 3

/** "Annoter" switch, the three colours and the eraser. */
@Composable
internal fun AnnotationBar(
    state: PlayUiState,
    onAction: (PlayAction) -> Unit,
) {
    val colors = BoardColors()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Switch(
            checked = state.annotating,
            onCheckedChange = { onAction(PlayAction.ToggleAnnotate) },
            modifier = Modifier.testTag(ANNOTATE_SWITCH_TAG),
        )
        Text(stringResource(R.string.play_annotate))
        AnnotationColor.entries.forEach { color ->
            val label = stringResource(R.string.play_annotation_color, stringResource(color.nameRes()))
            val selected = state.annotating && state.annotationColor == color
            Box(
                Modifier
                    .size(DOT_SIZE.dp)
                    .clip(CircleShape)
                    .background(colors.of(color))
                    .border(
                        width = if (selected) SELECTED_BORDER.dp else 0.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = CircleShape,
                    ).testTag(annotationColorTestTag(color))
                    .semantics {
                        contentDescription = label
                        role = Role.RadioButton
                        this.selected = selected
                    }.clickable { onAction(PlayAction.PickAnnotationColor(color)) },
            )
        }
        TextButton(onClick = { onAction(PlayAction.ClearAnnotations) }, enabled = !state.annotations.isEmpty) {
            Text(stringResource(R.string.play_annotation_clear))
        }
    }
}

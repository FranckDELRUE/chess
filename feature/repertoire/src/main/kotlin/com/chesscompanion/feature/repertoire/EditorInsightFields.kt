package com.chesscompanion.feature.repertoire

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource

fun editorInsightTag(field: InsightField) = "editor-${field.name.lowercase()}"

/** The idea, plan and trap prompts: what the move is for, what comes next, what to watch out for. */
@Composable
internal fun InsightFields(
    state: RepertoireEditorUiState,
    onAction: (EditorAction) -> Unit,
) {
    InsightField.entries.forEach { field ->
        val (value, label) =
            when (field) {
                InsightField.IDEA -> state.idea to R.string.editor_idea
                InsightField.PLAN -> state.plan to R.string.editor_plan
                InsightField.TRAP -> state.trap to R.string.editor_trap
            }
        OutlinedTextField(
            value = value,
            onValueChange = { onAction(EditorAction.EditInsight(field, it)) },
            label = { Text(stringResource(label)) },
            modifier = Modifier.fillMaxWidth().testTag(editorInsightTag(field)),
        )
    }
}

package com.chesscompanion.feature.repertoire

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chesscompanion.core.chess.Side

@Composable
internal fun RepertoireDialogs(
    dialog: RepertoireDialog,
    onAction: (RepertoireListAction) -> Unit,
) {
    val title: String
    val confirm: String
    when (dialog) {
        is RepertoireDialog.Create -> {
            title = stringResource(R.string.repertoire_create_title)
            confirm = stringResource(R.string.repertoire_create_confirm)
        }
        is RepertoireDialog.Rename -> {
            title = stringResource(R.string.repertoire_rename_title)
            confirm = stringResource(R.string.repertoire_rename_confirm)
        }
        is RepertoireDialog.Delete -> {
            title = stringResource(R.string.repertoire_delete_title, dialog.name)
            confirm = stringResource(R.string.repertoire_delete_confirm)
        }
    }
    AlertDialog(
        onDismissRequest = { onAction(RepertoireListAction.Dismiss) },
        title = { Text(title) },
        text = { DialogBody(dialog, onAction) },
        confirmButton = {
            TextButton(
                onClick = { onAction(RepertoireListAction.Confirm) },
                enabled = dialog.canConfirm,
                modifier = Modifier.testTag(REPERTOIRE_CONFIRM_TAG),
            ) { Text(confirm) }
        },
        dismissButton = {
            TextButton(onClick = { onAction(RepertoireListAction.Dismiss) }) {
                Text(stringResource(R.string.repertoire_cancel))
            }
        },
    )
}

@Composable
private fun DialogBody(
    dialog: RepertoireDialog,
    onAction: (RepertoireListAction) -> Unit,
) {
    when (dialog) {
        is RepertoireDialog.Delete -> Text(stringResource(R.string.repertoire_delete_message))
        is RepertoireDialog.Create -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NameField(dialog.name, onAction)
                Text(stringResource(R.string.repertoire_create_side))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Side.entries.forEach { side ->
                        FilterChip(
                            selected = dialog.side == side,
                            onClick = { onAction(RepertoireListAction.ChooseSide(side)) },
                            label = { Text(sideLabel(side)) },
                            modifier = Modifier.testTag(repertoireSideTag(side)),
                        )
                    }
                }
            }
        }
        is RepertoireDialog.Rename -> NameField(dialog.name, onAction)
    }
}

@Composable
private fun NameField(
    name: String,
    onAction: (RepertoireListAction) -> Unit,
) {
    OutlinedTextField(
        value = name,
        onValueChange = { onAction(RepertoireListAction.EditName(it)) },
        label = { Text(stringResource(R.string.repertoire_name_label)) },
        singleLine = true,
        modifier = Modifier.testTag(REPERTOIRE_NAME_FIELD_TAG),
    )
}

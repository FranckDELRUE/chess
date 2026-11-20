package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.Side
import com.chesscompanion.core.data.RepertoireSummary

/** A repertoire name is cut at this length. */
const val MAX_NAME_LENGTH = 60

/** Name to store: trimmed, null when blank. */
fun cleanName(raw: String): String? = raw.trim().takeIf { it.isNotEmpty() }

/** The dialog open over the list, if any. */
sealed interface RepertoireDialog {
    /** Creation: a name and the side the repertoire is played from. */
    data class Create(
        val name: String = "",
        val side: Side = Side.WHITE,
    ) : RepertoireDialog

    data class Rename(
        val id: String,
        val name: String,
    ) : RepertoireDialog

    data class Delete(
        val id: String,
        val name: String,
    ) : RepertoireDialog
}

/** True when the dialog can be confirmed (a delete always can). */
val RepertoireDialog.canConfirm: Boolean
    get() =
        when (this) {
            is RepertoireDialog.Create -> cleanName(name) != null
            is RepertoireDialog.Rename -> cleanName(name) != null
            is RepertoireDialog.Delete -> true
        }

data class RepertoireListUiState(
    val items: List<RepertoireSummary> = emptyList(),
    /** False until the store has answered once, to tell "empty" from "loading". */
    val loaded: Boolean = false,
    val dialog: RepertoireDialog? = null,
)

sealed interface RepertoireListAction {
    data object StartCreate : RepertoireListAction

    data class StartRename(
        val id: String,
    ) : RepertoireListAction

    data class StartDelete(
        val id: String,
    ) : RepertoireListAction

    data class EditName(
        val name: String,
    ) : RepertoireListAction

    data class ChooseSide(
        val side: Side,
    ) : RepertoireListAction

    data object Confirm : RepertoireListAction

    data object Dismiss : RepertoireListAction
}

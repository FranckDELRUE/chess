package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.repertoire.ImportRejection
import com.chesscompanion.core.chess.repertoire.ImportReport

/** What an import did, for the report shown to the user. */
data class ImportSummary(
    val gamesInFile: Int,
    val gamesMerged: Int,
    val addedMoves: Int,
    val existingMoves: Int,
    val rejected: List<ImportRejection>,
    /** From the start of the import to the end of the save. */
    val elapsedMs: Long,
)

internal fun ImportReport.toSummary(elapsedMs: Long) =
    ImportSummary(gamesInFile, gamesMerged, addedMoves, existingMoves, rejected, elapsedMs)

/** State of the PGN import dialog of the editor. */
sealed interface ImportUiState {
    data object Closed : ImportUiState

    /** Choosing a file or pasting text. */
    data object Form : ImportUiState

    data class Running(
        val done: Int,
        val total: Int,
    ) : ImportUiState

    data class Done(
        val summary: ImportSummary,
    ) : ImportUiState

    data object FileUnreadable : ImportUiState
}

package com.chesscompanion.feature.repertoire

import com.chesscompanion.core.chess.repertoire.ImportRejection
import com.chesscompanion.core.chess.repertoire.ImportReport
import com.chesscompanion.core.data.study.StudyResult

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

    /** Waiting for Lichess to answer. */
    data object Fetching : ImportUiState

    data class StudyFailed(
        val problem: StudyProblem,
    ) : ImportUiState
}

/** Why a study could not be fetched. */
enum class StudyProblem { INVALID_REFERENCE, NOT_FOUND, PRIVATE, RATE_LIMITED, OFFLINE, SERVER, TOO_LARGE }

internal fun StudyResult.toProblem(): StudyProblem =
    when (this) {
        is StudyResult.Found -> error("A study that was found is not a problem")
        StudyResult.NotFound -> StudyProblem.NOT_FOUND
        StudyResult.Private -> StudyProblem.PRIVATE
        StudyResult.RateLimited -> StudyProblem.RATE_LIMITED
        StudyResult.Offline -> StudyProblem.OFFLINE
        is StudyResult.ServerError -> StudyProblem.SERVER
        StudyResult.TooLarge -> StudyProblem.TOO_LARGE
    }

package com.chesscompanion.core.chess.repertoire

import com.chesscompanion.core.chess.pgn.PgnReader

/** Why a game of a PGN file was not merged. */
enum class RejectionReason {
    /** The PGN text of the game is faulty (illegal move, bad syntax): see the detail and the line. */
    UNREADABLE,

    /** The game does not start from the repertoire's start position. */
    DIFFERENT_START,
}

/** A game left out of an import; [gameNumber] counts the games of the file from 1. */
data class ImportRejection(
    val gameNumber: Int,
    val reason: RejectionReason,
    /** 1-based line in the file, when the reason is [RejectionReason.UNREADABLE]. */
    val line: Int? = null,
    val detail: String = "",
)

/** What an import did to a repertoire. */
data class ImportReport(
    val repertoire: Repertoire,
    val gamesInFile: Int,
    val gamesMerged: Int,
    val addedMoves: Int,
    val existingMoves: Int,
    val addedPositions: Int,
    val notedPositions: Int,
    val rejected: List<ImportRejection>,
) {
    /** True if the repertoire changed. */
    val changed: Boolean get() = addedMoves > 0 || notedPositions > 0
}

/**
 * Merges every game of the PGN [text] into this repertoire. Each game is read by [PgnReader], so every
 * move was checked by the rules before it can be stored (rule 2); a faulty game is rejected with its
 * reason and the others go on. [onProgress] gets (games handled, games in the file).
 */
fun Repertoire.importPgn(
    text: String,
    onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
): ImportReport {
    val read = PgnReader.readAll(text)
    val total = read.games.size + read.errors.size
    val broken = read.errors.map { it.gameNumber }.toSet()
    val numbers = (1..total).filter { it !in broken }
    val unreadable =
        read.errors.map {
            ImportRejection(it.gameNumber, RejectionReason.UNREADABLE, it.line, it.message)
        }
    val batch = mergeAll(read.games) { onProgress(it + read.errors.size, total) }
    val wrongStart = batch.mismatched.map { ImportRejection(numbers[it], RejectionReason.DIFFERENT_START) }
    val merged = batch.merged
    return ImportReport(
        repertoire = merged.repertoire,
        gamesInFile = total,
        gamesMerged = read.games.size - wrongStart.size,
        addedMoves = merged.addedMoves,
        existingMoves = merged.existingMoves,
        addedPositions = merged.addedPositions,
        notedPositions = merged.notedPositions,
        rejected = (unreadable + wrongStart).sortedBy { it.gameNumber },
    )
}

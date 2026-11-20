package com.chesscompanion.core.data

import kotlinx.coroutines.flow.Flow

/**
 * Stockfish's analysis of one position (S2-5), kept to be shown again without searching. The score is
 * seen from White: centipawns, or a mate in [mateIn] moves (negative when White is mated).
 */
data class StoredAnalysis(
    /** FEN of the position; only its first four fields (board, side, castling, en passant) count. */
    val fen: String,
    val depth: Int,
    val scoreCp: Int?,
    val mateIn: Int?,
    /** Best move in UCI, checked by the rules before it was stored (rule 2). */
    val bestMove: String,
    /** Principal variation in UCI, starting with [bestMove], cut at the first illegal move. */
    val pv: List<String>,
) {
    init {
        require((scoreCp == null) != (mateIn == null)) { "Exactly one of scoreCp and mateIn must be set" }
    }
}

enum class AnalysisJobState { PENDING, RUNNING, DONE, CANCELLED, FAILED }

/** One game waiting in, or gone through, the background analysis queue. */
data class AnalysisJob(
    val id: String,
    val pgn: String,
    val state: AnalysisJobState,
    /** Positions handled so far: the analysis resumes from there. */
    val done: Int,
    val total: Int,
)

/** Stored analyses and the queue of games to analyse; Room in the app, a fake in tests. */
interface AnalysisStore {
    /** The analysis of [fen] (identified by its Zobrist [key]), or null if there is none for that position. */
    suspend fun position(
        key: Long,
        fen: String,
    ): StoredAnalysis?

    /** Keeps [analysis] unless a deeper one of the same position is already stored. */
    suspend fun savePosition(
        key: Long,
        analysis: StoredAnalysis,
    )

    /** Adds a game to the queue and returns the id of its job. */
    suspend fun newJob(
        pgn: String,
        total: Int,
    ): String

    suspend fun job(id: String): AnalysisJob?

    suspend fun updateJob(
        id: String,
        state: AnalysisJobState,
        done: Int,
    )

    /** Marks every job still waiting or running as cancelled (the whole queue was cancelled). */
    suspend fun cancelUnfinished()

    /** The most recent job, as it changes; null before the first one. */
    fun latestJob(): Flow<AnalysisJob?>
}

/** The FEN fields that identify a position: the move counters do not. */
fun positionFen(fen: String): String =
    fen
        .trim()
        .split(' ')
        .take(POSITION_FIELDS)
        .joinToString(" ")

private const val POSITION_FIELDS = 4

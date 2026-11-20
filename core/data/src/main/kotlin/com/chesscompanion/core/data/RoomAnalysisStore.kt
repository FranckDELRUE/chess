package com.chesscompanion.core.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** [AnalysisStore] on Room. [clock] gives the time of each write (tests pass a fixed one). */
class RoomAnalysisStore(
    private val database: ChessDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) : AnalysisStore {
    private val positions = database.positionAnalysisDao()
    private val jobs = database.analysisJobDao()

    override suspend fun position(
        key: Long,
        fen: String,
    ): StoredAnalysis? =
        positions
            .get(key)
            // Two positions may share a 64-bit key: the stored FEN must be the one asked for.
            ?.takeIf { it.fen == positionFen(fen) }
            ?.let {
                StoredAnalysis(
                    fen = it.fen,
                    depth = it.depth,
                    scoreCp = it.scoreCp,
                    mateIn = it.mateIn,
                    bestMove = it.bestMove,
                    pv = it.pv.split(' ').filter(String::isNotEmpty),
                )
            }

    override suspend fun savePosition(
        key: Long,
        analysis: StoredAnalysis,
    ) {
        val fen = positionFen(analysis.fen)
        val current = positions.get(key)
        if (current != null && current.fen == fen && current.depth > analysis.depth) return
        positions.upsert(
            PositionAnalysisEntity(
                key = key,
                fen = fen,
                depth = analysis.depth,
                scoreCp = analysis.scoreCp,
                mateIn = analysis.mateIn,
                bestMove = analysis.bestMove,
                pv = analysis.pv.joinToString(" "),
                analysedAt = clock(),
            ),
        )
    }

    override suspend fun newJob(
        pgn: String,
        total: Int,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = clock()
        jobs.upsert(
            AnalysisJobEntity(
                id,
                pgn,
                AnalysisJobState.PENDING.name,
                done = 0,
                total = total,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    override suspend fun job(id: String): AnalysisJob? = jobs.get(id)?.toJob()

    override suspend fun updateJob(
        id: String,
        state: AnalysisJobState,
        done: Int,
    ) = jobs.update(id, state.name, done, clock())

    override suspend fun cancelUnfinished() =
        jobs.cancelAll(
            cancelled = AnalysisJobState.CANCELLED.name,
            unfinished = listOf(AnalysisJobState.PENDING.name, AnalysisJobState.RUNNING.name),
            updatedAt = clock(),
        )

    override fun latestJob(): Flow<AnalysisJob?> = jobs.latest().map { it?.toJob() }

    private fun AnalysisJobEntity.toJob() =
        AnalysisJob(
            id = id,
            pgn = pgn,
            state = AnalysisJobState.entries.firstOrNull { it.name == state } ?: AnalysisJobState.FAILED,
            done = done,
            total = total,
        )
}

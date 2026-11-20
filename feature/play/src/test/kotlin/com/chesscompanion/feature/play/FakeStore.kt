package com.chesscompanion.feature.play

import com.chesscompanion.core.data.AnalysisJob
import com.chesscompanion.core.data.AnalysisJobState
import com.chesscompanion.core.data.AnalysisStore
import com.chesscompanion.core.data.StoredAnalysis
import com.chesscompanion.core.data.positionFen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** [AnalysisStore] in memory, with the same rules as the Room one (the deepest analysis wins). */
class FakeStore : AnalysisStore {
    val positions = mutableMapOf<Long, StoredAnalysis>()
    val jobs = linkedMapOf<String, AnalysisJob>()
    private val latest = MutableStateFlow<AnalysisJob?>(null)

    override suspend fun position(
        key: Long,
        fen: String,
    ): StoredAnalysis? = positions[key]?.takeIf { positionFen(it.fen) == positionFen(fen) }

    override suspend fun savePosition(
        key: Long,
        analysis: StoredAnalysis,
    ) {
        val current = positions[key]
        if (current != null &&
            positionFen(current.fen) == positionFen(analysis.fen) &&
            current.depth > analysis.depth
        ) {
            return
        }
        positions[key] = analysis
    }

    override suspend fun newJob(
        pgn: String,
        total: Int,
    ): String {
        val id = "job-${jobs.size + 1}"
        put(AnalysisJob(id, pgn, AnalysisJobState.PENDING, 0, total))
        return id
    }

    override suspend fun job(id: String): AnalysisJob? = jobs[id]

    override suspend fun updateJob(
        id: String,
        state: AnalysisJobState,
        done: Int,
    ) {
        jobs[id]?.let { put(it.copy(state = state, done = done)) }
    }

    override suspend fun cancelUnfinished() {
        jobs.values
            .filter { it.state == AnalysisJobState.PENDING || it.state == AnalysisJobState.RUNNING }
            .forEach { put(it.copy(state = AnalysisJobState.CANCELLED)) }
    }

    override fun latestJob(): Flow<AnalysisJob?> = latest

    fun put(job: AnalysisJob) {
        jobs[job.id] = job
        latest.value = jobs.values.last()
    }
}

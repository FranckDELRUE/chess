package com.chesscompanion.feature.play

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chesscompanion.core.chess.Position
import com.chesscompanion.core.chess.pgn.PgnReader
import com.chesscompanion.core.data.AnalysisJob
import com.chesscompanion.core.data.AnalysisStore
import com.chesscompanion.core.data.StoredAnalysis
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The background analysis seen from the game screen; WorkManager and Room in the app, a fake in tests. */
interface GameAnalysisQueue {
    /** The most recent game sent to the queue, with its progress. */
    val latest: Flow<AnalysisJob?>

    /** Adds the game [pgn] to the queue (rule 2: it is read by the rules first). */
    suspend fun enqueue(pgn: String)

    /** Cancels the analysis in progress and the games waiting after it. */
    suspend fun cancelAll()

    /** What the queue has stored for [position], if anything. */
    suspend fun stored(position: Position): StoredAnalysis?
}

/** One unique chain of [GameAnalysisWorker]s: each game is appended after the ones waiting. */
class WorkManagerAnalysisQueue
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val store: AnalysisStore,
    ) : GameAnalysisQueue {
        override val latest: Flow<AnalysisJob?> = store.latestJob()

        override suspend fun enqueue(pgn: String) {
            val game = runCatching { PgnReader.read(pgn) }.getOrNull() ?: return
            val id = store.newJob(pgn, GameAnalyzer.positionCount(game))
            val request =
                OneTimeWorkRequestBuilder<GameAnalysisWorker>()
                    .setInputData(workDataOf(GameAnalysisWorker.KEY_JOB to id))
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .addTag(id)
                    .build()
            WorkManager
                .getInstance(
                    context,
                ).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        override suspend fun cancelAll() {
            store.cancelUnfinished()
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        }

        override suspend fun stored(position: Position): StoredAnalysis? =
            store.position(position.zobristKey, position.toFen())

        private companion object {
            const val UNIQUE_WORK = "game-analysis"
        }
    }

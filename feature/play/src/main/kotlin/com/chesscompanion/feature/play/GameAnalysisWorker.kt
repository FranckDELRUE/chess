package com.chesscompanion.feature.play

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.chesscompanion.core.data.AnalysisJobState
import com.chesscompanion.core.data.AnalysisStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Analyses one game of the queue in the foreground (S2-5): a notification shows the progress and
 * offers to cancel; the analysis goes on with the screen off. Cancelled by the user, the whole queue
 * is cancelled (WorkManager cancels the games chained after this one); stopped by the system, the job
 * goes back to waiting and resumes where it stopped when WorkManager runs it again.
 */
@HiltWorker
class GameAnalysisWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val analyzer: GameAnalyzer,
        private val store: AnalysisStore,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val id = inputData.getString(KEY_JOB) ?: return Result.failure()
            val job = store.job(id) ?: return Result.failure()
            showProgress(job.done, job.total)
            return try {
                val state =
                    analyzer.run(id) { done, total ->
                        setProgress(workDataOf(KEY_DONE to done, KEY_TOTAL to total))
                        showProgress(done, total)
                    }
                if (state == AnalysisJobState.FAILED) Result.failure() else Result.success()
            } catch (e: CancellationException) {
                withContext(NonCancellable) {
                    if (stopReason == WorkInfo.STOP_REASON_CANCELLED_BY_APP) {
                        store.cancelUnfinished()
                    } else {
                        store.job(id)?.takeIf { it.state == AnalysisJobState.RUNNING }?.let {
                            store.updateJob(id, AnalysisJobState.PENDING, it.done)
                        }
                    }
                }
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                store.job(id)?.let { store.updateJob(id, AnalysisJobState.FAILED, it.done) }
                Result.failure(workDataOf(KEY_ERROR to (e.message ?: e.toString())))
            }
        }

        override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(0, 0)

        /** Runs in the foreground with the progress; without the right (Android 12+ limits) it still analyses. */
        private suspend fun showProgress(
            done: Int,
            total: Int,
        ) {
            runCatching { setForeground(foregroundInfo(done, total)) }
        }

        private fun foregroundInfo(
            done: Int,
            total: Int,
        ): ForegroundInfo {
            val notification = notification(done, total)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                ForegroundInfo(NOTIFICATION_ID, notification)
            }
        }

        private fun notification(
            done: Int,
            total: Int,
        ): Notification {
            val context = applicationContext
            NotificationManagerCompat.from(context).createNotificationChannel(
                NotificationChannelCompat
                    .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                    .setName(context.getString(R.string.play_queue_channel))
                    .build(),
            )
            val cancel = WorkManager.getInstance(context).createCancelPendingIntent(id)
            return NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle(context.getString(R.string.play_queue_notification_title))
                .setContentText(context.getString(R.string.play_queue_progress, done, total))
                .setProgress(total, done, total == 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.play_queue_cancel),
                    cancel,
                ).build()
        }

        companion object {
            const val KEY_JOB = "job"
            const val KEY_DONE = "done"
            const val KEY_TOTAL = "total"
            const val KEY_ERROR = "error"
            private const val CHANNEL_ID = "game-analysis"
            private const val NOTIFICATION_ID = 2_005
        }
    }

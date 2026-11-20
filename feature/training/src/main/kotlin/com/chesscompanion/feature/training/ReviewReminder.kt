package com.chesscompanion.feature.training

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.chesscompanion.core.data.RepertoireStore
import com.chesscompanion.core.data.ReviewCardStore
import com.chesscompanion.core.srs.endOfDay
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the daily reminder is on; off until the player turns it on. */
interface ReminderSettings {
    var enabled: Boolean
}

/** [ReminderSettings] in the app's private preferences. */
class SharedPreferencesReminderSettings(
    context: Context,
) : ReminderSettings {
    private val preferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_ENABLED, value).apply()

    private companion object {
        const val FILE = "review-reminder"
        const val KEY_ENABLED = "enabled"
    }
}

/** Hour of the day (local time) at which the reminder is shown. */
internal const val REMINDER_HOUR = 18

/** Time to wait from [now] until the next [hour] o'clock in [zone] (a full day if it is exactly that time). */
internal fun delayUntilNext(
    now: Instant,
    zone: ZoneId,
    hour: Int = REMINDER_HOUR,
): Duration {
    val local = now.atZone(zone)
    var next = local.toLocalDate().atTime(hour, 0).atZone(zone)
    if (!next.toInstant().isAfter(now)) next = next.plusDays(1)
    return Duration.between(now, next.toInstant())
}

/** Counts the positions due today across all repertoires (reviewed positions only: new ones never nag). */
class DueCounter(
    private val repertoires: RepertoireStore,
    private val cards: ReviewCardStore,
    private val clock: Clock,
) {
    suspend fun dueToday(): Int {
        val limit = endOfDay(Instant.now(clock), clock.zone)
        return repertoires.repertoires().first().sumOf { cards.dueCount(it.id, limit) }
    }
}

/**
 * The daily reminder (S8-6): once a day it says how many positions are due, if any. It reads the local database
 * only (no network) and shows nothing when nothing is due or when notifications are not allowed.
 */
@HiltWorker
class ReviewReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val repertoires: RepertoireStore,
        private val cards: ReviewCardStore,
        private val clock: Clock,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val due = DueCounter(repertoires, cards, clock).dueToday()
            val notifications = NotificationManagerCompat.from(applicationContext)
            if (due > 0 && notifications.areNotificationsEnabled()) notify(notifications, due)
            return Result.success()
        }

        @android.annotation.SuppressLint("MissingPermission")
        private fun notify(
            notifications: NotificationManagerCompat,
            due: Int,
        ) {
            val context = applicationContext
            notifications.createNotificationChannel(
                NotificationChannelCompat
                    .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(context.getString(R.string.reminder_channel))
                    .build(),
            )
            val open =
                context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
                    PendingIntent.getActivity(
                        context,
                        0,
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle(context.getString(R.string.reminder_title))
                    .setContentText(context.resources.getQuantityString(R.plurals.reminder_text, due, due))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .build()
            notifications.notify(NOTIFICATION_ID, notification)
        }

        companion object {
            const val CHANNEL_ID = "review-reminder"
            const val NOTIFICATION_ID = 8_006
        }
    }

/** Turns the daily reminder on and off: a unique periodic job, and the choice remembered. */
@Singleton
class ReviewReminders
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val settings: ReminderSettings,
        private val clock: Clock,
    ) {
        val isEnabled: Boolean get() = settings.enabled

        fun setEnabled(enabled: Boolean) {
            settings.enabled = enabled
            val work = WorkManager.getInstance(context)
            if (enabled) {
                val delay = delayUntilNext(Instant.now(clock), clock.zone)
                val request =
                    PeriodicWorkRequestBuilder<ReviewReminderWorker>(1, TimeUnit.DAYS)
                        .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                        .build()
                work.enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
            } else {
                work.cancelUniqueWork(UNIQUE_NAME)
            }
        }

        private companion object {
            const val UNIQUE_NAME = "review-reminder"
        }
    }

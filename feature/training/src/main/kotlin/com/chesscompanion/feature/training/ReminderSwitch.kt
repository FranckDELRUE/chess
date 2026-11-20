package com.chesscompanion.feature.training

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

const val REMINDER_SWITCH_TAG = "reminder-switch"
const val REMINDER_DENIED_TAG = "reminder-denied"

@Module
@InstallIn(SingletonComponent::class)
object TrainingModule {
    @Provides
    @Singleton
    fun provideReminderSettings(
        @ApplicationContext context: Context,
    ): ReminderSettings = SharedPreferencesReminderSettings(context)
}

@HiltViewModel
class ReminderViewModel
    @Inject
    constructor(
        private val reminders: ReviewReminders,
    ) : ViewModel() {
        val enabled: Boolean get() = reminders.isEnabled

        fun setEnabled(value: Boolean) = reminders.setEnabled(value)
    }

/**
 * The switch of the optional daily reminder. Turning it on asks for the notification permission (Android 13+);
 * refused, the reminder stays off and the switch says why.
 */
@Composable
fun ReminderSwitch(
    modifier: Modifier = Modifier,
    viewModel: ReminderViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(viewModel.enabled) }
    var denied by remember { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResults.requestPermission) { granted ->
            denied = !granted
            enabled = granted
            viewModel.setEnabled(granted)
        }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.reminder_switch),
                Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = enabled,
                onCheckedChange = { wanted ->
                    denied = false
                    if (wanted && needsPermission(context)) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        enabled = wanted
                        viewModel.setEnabled(wanted)
                    }
                },
                modifier = Modifier.testTag(REMINDER_SWITCH_TAG),
            )
        }
        if (denied) Text(stringResource(R.string.reminder_denied), Modifier.testTag(REMINDER_DENIED_TAG))
    }
}

private object ActivityResults {
    val requestPermission = ActivityResultContracts.RequestPermission()
}

private fun needsPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED

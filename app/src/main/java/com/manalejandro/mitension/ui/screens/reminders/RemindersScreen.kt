package com.manalejandro.mitension.ui.screens.reminders

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manalejandro.mitension.R
import com.manalejandro.mitension.data.db.Reminder
import com.manalejandro.mitension.notifications.NotificationHelper
import com.manalejandro.mitension.notifications.ReminderScheduler
import com.manalejandro.mitension.ui.MainViewModel
import com.manalejandro.mitension.ui.components.AppSectionCard
import com.manalejandro.mitension.ui.components.BigOutlinedButton
import com.manalejandro.mitension.ui.components.BigSwitchRow
import com.manalejandro.mitension.ui.str
import com.manalejandro.mitension.util.ReminderKinds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val reminders by vm.reminders.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingToggleKind by remember { mutableStateOf<String?>(null) }
    var pendingSoundKind by remember { mutableStateOf<String?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val kind = pendingToggleKind
        if (granted && kind != null) {
            vm.setReminderEnabled(kind, true)
        }
        pendingToggleKind = null
    }

    val soundPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val kind = pendingSoundKind ?: return@rememberLauncherForActivityResult
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            vm.setReminderSound(kind, uri?.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = str(R.string.reminders_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = str(R.string.reminders_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        reminders.forEach { reminder ->
            ReminderCard(
                reminder = reminder,
                onEnabledChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !NotificationHelper.notificationsEnabled(context)
                    ) {
                        pendingToggleKind = reminder.kind
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        vm.setReminderEnabled(reminder.kind, enabled)
                    }
                },
                onTimeChange = { hour, minute ->
                    vm.setReminderTime(reminder.kind, hour, minute)
                },
                onPickSound = {
                    pendingSoundKind = reminder.kind
                    soundPickerLauncher.launch(buildRingtonePickerIntent(context, reminder.soundUri))
                }
            )
        }

        if (!ReminderScheduler.canScheduleExact(context)) {
            AppSectionCard(title = str(R.string.reminder_exact_alarm)) {
                Column {
                    Text(
                        text = str(R.string.reminder_exact_alarm_desc),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    BigOutlinedButton(
                        text = str(R.string.reminder_open_settings),
                        icon = Icons.Filled.Alarm,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                })
                            }
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderCard(
    reminder: Reminder,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onPickSound: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val icon: ImageVector = when (reminder.kind) {
        ReminderKinds.MORNING -> Icons.Filled.WbSunny
        ReminderKinds.MIDDAY -> Icons.Filled.LightMode
        else -> Icons.Filled.NightsStay
    }

    val timeLabel = String.format("%02d:%02d", reminder.hour, reminder.minute)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BigSwitchRow(
            label = str(ReminderKinds.labelRes(reminder.kind)),
            icon = icon,
            checked = reminder.enabled,
            onCheckedChange = onEnabledChange,
            supportingText = if (reminder.enabled) str(R.string.reminder_enabled) else str(R.string.reminder_disabled)
        )

        if (reminder.enabled) {
            AppSectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            str(R.string.reminder_time),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { showTimePicker = true }) {
                            Text(
                                timeLabel,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            str(R.string.reminder_sound),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onPickSound) {
                            Text(
                                soundLabel(context, reminder.soundUri),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(
            initialHour = reminder.hour,
            initialMinute = reminder.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(state.hour, state.minute)
                        showTimePicker = false
                    }
                ) {
                    Text(str(R.string.users_save), style = MaterialTheme.typography.titleMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(str(R.string.users_cancel), style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                TimePicker(state = state)
            }
        )
    }
}

private fun soundLabel(context: Context, soundUri: String?): String {
    if (soundUri == null || soundUri == NotificationHelper.SILENT_SOUND) {
        return context.getString(R.string.reminder_sound_default)
    }
    val uri = Uri.parse(soundUri)
    return RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        ?: context.getString(R.string.reminder_sound_default)
}

private fun buildRingtonePickerIntent(context: Context, soundUri: String?): Intent {
    val existing = soundUri?.takeIf { it.isNotBlank() && it != NotificationHelper.SILENT_SOUND }
        ?.let { Uri.parse(it) }
    return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.reminder_sound))
        if (existing != null) {
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
        }
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
    }
}
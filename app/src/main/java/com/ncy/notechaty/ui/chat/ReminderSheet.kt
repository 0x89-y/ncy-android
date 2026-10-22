package com.ncy.notechaty.ui.chat

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ncy.notechaty.R
import com.ncy.notechaty.ui.desktoptheme.NcyAlertDialog
import com.ncy.notechaty.ui.desktoptheme.NcySheetActionRow
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal data class ReminderPreset(@StringRes val label: Int, val at: Long, val icon: ImageVector)

/**
 * The quick choices, as moments rather than delays. "This evening" only while there
 * is still an evening to come; "Next week" is the coming Monday morning.
 */
internal fun reminderPresets(now: Long, zone: TimeZone = TimeZone.getDefault()): List<ReminderPreset> {
    fun at(daysAhead: Int, hour: Int): Long = Calendar.getInstance(zone).apply {
        timeInMillis = now
        add(Calendar.DAY_OF_YEAR, daysAhead)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val today = Calendar.getInstance(zone).apply { timeInMillis = now }
    val daysToMonday = ((Calendar.MONDAY - today.get(Calendar.DAY_OF_WEEK) + 7) % 7).let { if (it == 0) 7 else it }
    return buildList {
        add(ReminderPreset(R.string.in_15_minutes, now + 15 * 60_000L, Icons.Default.Alarm))
        add(ReminderPreset(R.string.in_1_hour, now + 60 * 60_000L, Icons.Default.AccessTime))
        add(ReminderPreset(R.string.in_3_hours, now + 3 * 60 * 60_000L, Icons.Default.AccessTime))
        if (today.get(Calendar.HOUR_OF_DAY) < EVENING_HOUR - 1) {
            add(ReminderPreset(R.string.this_evening, at(0, EVENING_HOUR), Icons.Default.NightsStay))
        }
        add(ReminderPreset(R.string.tomorrow_morning, at(1, MORNING_HOUR), Icons.Default.WbSunny))
        add(ReminderPreset(R.string.next_week, at(daysToMonday, MORNING_HOUR), Icons.Default.DateRange))
    }
}

private const val MORNING_HOUR = 9
private const val EVENING_HOUR = 18

/** "18:00" today, "Thu 09:00" within a week, otherwise "Mon, Oct 27, 09:00", in the device's style. */
internal fun formatReminderTime(at: Long, now: Long, locale: Locale, is24Hour: Boolean): String {
    val then = Calendar.getInstance().apply { timeInMillis = at }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val sameDay = then.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        then.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    val hours = if (is24Hour) "Hm" else "hm"
    val skeleton = when {
        sameDay -> hours
        at - now < 6 * 24 * 60 * 60_000L -> "EEE$hours"
        then.get(Calendar.YEAR) == today.get(Calendar.YEAR) -> "EEEMMMd$hours"
        else -> "yMMMd$hours"
    }
    return DateFormat.format(DateFormat.getBestDateTimePattern(locale, skeleton), Date(at)).toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
    /** The reminder already set on the message, if any. */
    current: Long?,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val locale = Locale.getDefault()
    val is24Hour = DateFormat.is24HourFormat(context)
    val now = remember { System.currentTimeMillis() }
    val presets = remember(now) { reminderPresets(now) }
    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    var exactAlarmsAllowed by remember { mutableStateOf(alarmManager.canScheduleExactAlarms()) }
    // Checked again on return from the system setting the hint links to.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        exactAlarmsAllowed = alarmManager.canScheduleExactAlarms()
    }
    var pickingDate by remember { mutableStateOf(false) }
    var pickedDay by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Text(
                stringResource(if (current != null) R.string.edit_reminder else R.string.set_reminder),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            if (current != null) {
                Text(
                    stringResource(R.string.reminder_set_for, formatReminderTime(current, now, locale, is24Hour)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp),
                )
                NcySheetActionRow(Icons.Default.AlarmOff, stringResource(R.string.remove_reminder), destructive = true) {
                    onRemove()
                }
            }
            if (!exactAlarmsAllowed) {
                // Without "Alarms & reminders" Android may hold a reminder back by
                // up to an hour. Say so here, where it matters, with the way out.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                ) {
                    Text(
                        stringResource(R.string.reminders_may_be_late),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                        )
                    }) { Text(stringResource(R.string.allow)) }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            presets.forEach { preset ->
                ReminderOptionRow(
                    icon = preset.icon,
                    label = stringResource(preset.label),
                    time = formatReminderTime(preset.at, now, locale, is24Hour),
                ) { onPick(preset.at) }
            }
            ReminderOptionRow(
                icon = Icons.Default.CalendarMonth,
                label = stringResource(R.string.pick_date_and_time),
                time = null,
            ) { pickingDate = true }
        }
    }

    if (pickingDate) {
        val todayUtc = remember(now) { startOfLocalDayAsUtc(now) }
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = todayUtc,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickedDay = dateState.selectedDateMillis
                        pickingDate = false
                    },
                    enabled = dateState.selectedDateMillis != null,
                ) { Text(stringResource(R.string.next)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) {
            DatePicker(state = dateState)
        }
    }

    pickedDay?.let { dayUtc ->
        val timeState = rememberTimePickerState(initialHour = MORNING_HOUR, initialMinute = 0, is24Hour = is24Hour)
        val at = combineDayAndTime(dayUtc, timeState.hour, timeState.minute)
        val inFuture = at > System.currentTimeMillis()
        NcyAlertDialog(
            onDismissRequest = { pickedDay = null },
            title = { Text(stringResource(R.string.pick_a_time)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timeState)
                    if (!inFuture) {
                        Text(
                            stringResource(R.string.reminder_time_in_past),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickedDay = null; onPick(at) }, enabled = inFuture) {
                    Text(stringResource(R.string.set_reminder))
                }
            },
            dismissButton = { TextButton(onClick = { pickedDay = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun ReminderOptionRow(icon: ImageVector, label: String, time: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (time != null) {
            Text(time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The date picker works in UTC midnights; this is today's, as the picker counts days. */
internal fun startOfLocalDayAsUtc(now: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = now }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** The local moment for a picked day (a UTC midnight) at [hour]:[minute]. */
internal fun combineDayAndTime(dayUtc: Long, hour: Int, minute: Int, zone: TimeZone = TimeZone.getDefault()): Long {
    val day = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dayUtc }
    return Calendar.getInstance(zone).apply {
        clear()
        set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH), hour, minute)
    }.timeInMillis
}

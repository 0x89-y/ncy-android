package com.ncy.notechaty.ui.chat

import androidx.compose.ui.res.stringResource
import com.ncy.notechaty.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/** A day in the chat: the date between two hairlines. */
@Composable
fun DateHeader(timestamp: Long) {
    val line = com.ncy.notechaty.ui.theme.NcyTheme.colors.lineStrong
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = line)
        Text(
            text = formatDateHeader(timestamp),
            style = com.ncy.notechaty.ui.theme.NcyTheme.type.quote,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(Modifier.weight(1f), thickness = 1.dp, color = line)
    }
}

@Composable
private fun formatDateHeader(timestamp: Long): String {
    val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val todayCalendar = Calendar.getInstance()

    val sameDay = messageCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
            messageCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)

    if (sameDay) return stringResource(R.string.today)

    todayCalendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = messageCalendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) &&
            messageCalendar.get(Calendar.DAY_OF_YEAR) == todayCalendar.get(Calendar.DAY_OF_YEAR)

    if (yesterday) return stringResource(R.string.yesterday)

    val sameYear = messageCalendar.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
    // The locale picks the order and punctuation; "MMM d" reads wrong in most of the world.
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(
        Locale.getDefault(), if (sameYear) "MMMd" else "yMMMd"
    )
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}

fun getDateKey(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
}

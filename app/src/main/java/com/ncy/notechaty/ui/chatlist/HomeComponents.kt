package com.ncy.notechaty.ui.chatlist

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.ncy.notechaty.R
import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.MessageType
import com.ncy.notechaty.data.model.Profile
import com.ncy.notechaty.ui.theme.NcyAvatarShape
import com.ncy.notechaty.ui.theme.NcyGutter
import com.ncy.notechaty.ui.theme.NcyInitialTile
import com.ncy.notechaty.ui.theme.NcyInkTile
import com.ncy.notechaty.ui.theme.NcyPageTitle
import com.ncy.notechaty.ui.theme.NcyPill
import com.ncy.notechaty.ui.theme.NcyRowDivider
import com.ncy.notechaty.ui.theme.NcySectionLabel
import com.ncy.notechaty.ui.theme.NcyTheme
import com.ncy.notechaty.ui.theme.ncyAvatarColor
import com.ncy.notechaty.ui.theme.ncyChatHue
import com.ncy.notechaty.util.gibberishFor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
//  Header
// ---------------------------------------------------------------------------

/** Mono profile label at the top left; opens the profile sheet. */
@Composable
internal fun HomeProfileLabel(activeProfile: Profile?, showAll: Boolean, onClick: () -> Unit) {
    val name = if (showAll) stringResource(R.string.all_chats) else activeProfile?.name ?: stringResource(R.string.ncy)
    val c = NcyTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = stringResource(R.string.switch_profile), role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(c.ink),
            contentAlignment = Alignment.Center,
        ) {
            Text(name.take(1).uppercase(), style = NcyTheme.type.time, color = c.onInk)
        }
        Text(
            name.uppercase(),
            style = NcyTheme.type.label.copy(fontSize = NcyTheme.type.label.fontSize * 1.1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp),
        )
        Icon(Icons.Outlined.KeyboardArrowDown, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun HomeHeader(
    title: String,
    activeProfile: Profile?,
    showAllProfiles: Boolean,
    syncStatus: HomeSyncStatus?,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
        ) {
            HomeProfileLabel(activeProfile, showAllProfiles, onProfileClick)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.search))
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Tune, contentDescription = stringResource(R.string.settings))
            }
        }
        NcyPageTitle(title)
        if (syncStatus != null) HomeSyncLine(syncStatus, onSyncClick)
        else Spacer(Modifier.size(14.dp))
    }
}

@Composable
private fun HomeSyncLine(status: HomeSyncStatus, onClick: () -> Unit) {
    val online = status.connectedNames.isNotEmpty()
    val ago = status.lastSyncAt?.let {
        DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString().lowercase()
    }
    val names: @Composable (List<String>) -> String = { list ->
        when (list.size) {
            0 -> ""
            1 -> list[0]
            else -> stringResource(R.string.home_peers_and_more, list[0], list.size - 1)
        }
    }
    val text = when {
        online -> stringResource(R.string.home_synced_with, names(status.connectedNames), ago ?: "")
        // Offline, no one device is named: with several paired, naming the most
        // recent one suggested it was the one that mattered. Devices lists them all.
        ago != null -> stringResource(R.string.home_peer_away, ago)
        else -> stringResource(R.string.home_paired_not_synced)
    }.trimEnd(' ', '·')
    val dot = if (online) NcyTheme.colors.online else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = NcyGutter),
    ) {
        Box(
            Modifier.size(7.dp).clip(CircleShape)
                .then(if (online) Modifier.background(dot) else Modifier.border(1.5.dp, dot, CircleShape))
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal data class HomeChip(val label: String, val icon: ImageVector?, val count: Int?, val selected: Boolean, val onClick: () -> Unit)

@Composable
internal fun HomeChips(chips: List<HomeChip>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = NcyGutter)
            .padding(bottom = 12.dp),
    ) {
        chips.forEach { NcyPill(selected = it.selected, onClick = it.onClick, label = it.label, icon = it.icon, count = it.count) }
    }
}

/** Search mode header: back, the field, and a clear button. */
@Composable
internal fun HomeSearchBar(query: String, onQuery: (String) -> Unit, onClose: () -> Unit, focusRequester: FocusRequester) {
    val c = NcyTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close_search))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.raised)
                .border(1.dp, c.lineStrong, RoundedCornerShape(14.dp))
                .padding(start = 12.dp),
        ) {
            Icon(Icons.Outlined.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(stringResource(R.string.home_search_notes), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.home_clear_search), Modifier.size(18.dp))
                }
            } else Spacer(Modifier.width(12.dp))
        }
    }
}

// ---------------------------------------------------------------------------
//  Rows
// ---------------------------------------------------------------------------

/** A chat's tile: its picture, a lock, or its initial on its colour. */
@Composable
internal fun HomeChatTile(chat: Chat, baseDir: File?, locked: Boolean, size: androidx.compose.ui.unit.Dp = 44.dp) {
    val avatarFile = baseDir?.let { dir -> chat.avatarFile?.let { File(File(dir, "chat_${chat.id}"), it) } }
    when {
        locked -> NcyInkTile(size = size) { Icon(Icons.Outlined.Lock, null, Modifier.size(size * 0.42f)) }
        avatarFile != null && avatarFile.exists() -> AsyncImage(
            model = avatarFile,
            contentDescription = null,
            modifier = Modifier.size(size).clip(NcyAvatarShape),
            contentScale = ContentScale.Crop,
        )
        else -> NcyInitialTile(chat.name, ncyChatHue(chat), size = size)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeChatRow(
    chat: Chat,
    baseDir: File,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    profileTag: String? = null,
) {
    val view = LocalView.current
    val locked = chat.isProtected && !isUnlocked
    val name = if (locked && chat.hideName) gibberishFor(chat.id) else chat.name
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                },
            )
            .heightIn(min = 68.dp)
            .padding(horizontal = NcyGutter, vertical = 10.dp),
    ) {
        HomeChatTile(chat, baseDir, locked)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(name, style = NcyTheme.type.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (chat.isProtected && !locked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.Lock, stringResource(R.string.unlocked), Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                chat.lastMessageAt?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(homeTime(it), style = NcyTheme.type.time, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val preview = if (locked) stringResource(R.string.protected_chat) else chat.lastMessagePreview.orEmpty()
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (profileTag != null) {
                    Spacer(Modifier.width(8.dp))
                    HueTag(profileTag, ncyAvatarColor(profileTag))
                }
            }
        }
    }
}

/** A coloured dot and a name: which chat (or profile) something is in. */
@Composable
internal fun HueTag(name: String, hue: Color, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(hue))
        Spacer(Modifier.width(6.dp))
        Text(
            name,
            style = NcyTheme.type.quote.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Accent-soft card for the next reminder, above the chats. */
@Composable
internal fun HomeUpNextCard(item: com.ncy.notechaty.ui.starred.StarredMessage, more: Int, onClick: () -> Unit) {
    val c = NcyTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.accentSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Outlined.NotificationsNone, null, tint = c.onAccentSoft)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val at = item.message.reminderAt ?: item.message.createdAt
            Text(
                "${stringResource(R.string.home_up_next)} · ${reminderTime(at)}".uppercase(),
                style = NcyTheme.type.label,
                color = c.onAccentSoft,
            )
            Text(
                item.message.plainTextPreview().ifBlank { typeLabel(item.message) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(R.string.home_in_chat, item.chatName), style = MaterialTheme.typography.bodySmall, color = c.onAccentSoft)
        }
        if (more > 0) {
            Spacer(Modifier.width(8.dp))
            Text("+$more", style = NcyTheme.type.time, color = c.onAccentSoft)
        }
    }
}

/** A note found by search or listed under Starred: where it is, when, and what it says. */
@Composable
internal fun HomeNoteRow(
    chatName: String,
    hue: Color,
    message: Message,
    query: String = "",
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = NcyGutter, vertical = 11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HueTag(chatName, hue, Modifier.weight(1f))
            if (message.isStarred) {
                Icon(Icons.Filled.Star, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
            }
            Text(homeTime(message.createdAt), style = NcyTheme.type.time, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.size(3.dp))
        Row(verticalAlignment = Alignment.Top) {
            typeIcon(message)?.let {
                Icon(it, null, Modifier.padding(top = 3.dp, end = 6.dp).size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                highlight(message.plainTextPreview().ifBlank { typeLabel(message) }, query),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * 0.94f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** One reminder in the timeline, with a ring to mark it done. */
@Composable
internal fun HomeReminderRow(
    item: com.ncy.notechaty.ui.starred.StarredMessage,
    hue: Color,
    showDay: Boolean,
    onClick: () -> Unit,
    onDone: () -> Unit,
) {
    val at = item.message.reminderAt ?: return
    val view = LocalView.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = NcyGutter, end = 6.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Column(Modifier.width(56.dp)) {
            Text(
                if (showDay) SimpleDateFormat("EEE", Locale.getDefault()).format(Date(at)) else clock(at),
                style = NcyTheme.type.time.copy(fontSize = NcyTheme.type.time.fontSize * 1.18f),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (showDay) Text(clock(at), style = NcyTheme.type.time, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(
                item.message.plainTextPreview().ifBlank { typeLabel(item.message) },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            HueTag(item.chatName, hue)
        }
        val doneLabel = stringResource(R.string.home_mark_done)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    onDone()
                }
                .semantics { contentDescription = doneLabel },
        ) {
            Box(Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

/** "Reminders may ring late" with the way to fix it, shown only while it is true. */
@Composable
internal fun HomeExactAlarmWarning() {
    val context = LocalContext.current
    val alarmManager = remember { context.getSystemService(AlarmManager::class.java) }
    var allowed by remember { mutableStateOf(alarmManager.canScheduleExactAlarms()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { allowed = alarmManager.canScheduleExactAlarms() }
    if (allowed) return
    val c = NcyTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, c.lineStrong, RoundedCornerShape(14.dp))
            .padding(12.dp),
    ) {
        Icon(Icons.Outlined.Alarm, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(stringResource(R.string.home_exact_alarms_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.home_exact_alarms_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.home_allow_exact_alarms),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .heightIn(min = 40.dp)
                    .clickable(role = Role.Button) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                    }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

/** Icon, title and hint for an empty list. */
@Composable
internal fun HomeEmpty(icon: ImageVector, title: String, hint: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(horizontal = 40.dp)) {
        Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(14.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.size(6.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** A section label for the lists on the home screen. */
@Composable
internal fun HomeLabel(text: String) {
    NcySectionLabel(text, modifier = Modifier.padding(start = NcyGutter, top = 16.dp, bottom = 4.dp))
}

/** Hairline between chat rows, indented past the tile. */
@Composable
internal fun HomeRowDivider() = NcyRowDivider(inset = 78.dp)

// ---------------------------------------------------------------------------
//  Text helpers
// ---------------------------------------------------------------------------

private fun clock(millis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

private fun sameDay(a: Calendar, b: Calendar) =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

/** Today → 14:02, yesterday → Yesterday, this week → Mon, this year → 12 Nov, older → 12 Nov 2024. */
@Composable
internal fun homeTime(millis: Long): String {
    val now = Calendar.getInstance()
    val t = Calendar.getInstance().apply { timeInMillis = millis }
    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    val locale = Locale.getDefault()
    return when {
        sameDay(now, t) -> clock(millis)
        sameDay(yesterday, t) -> stringResource(R.string.yesterday)
        now.timeInMillis - millis < 6L * 24 * 3600 * 1000 && millis < now.timeInMillis -> SimpleDateFormat("EEE", locale).format(Date(millis))
        now.get(Calendar.YEAR) == t.get(Calendar.YEAR) -> SimpleDateFormat("d MMM", locale).format(Date(millis))
        else -> SimpleDateFormat("d MMM yyyy", locale).format(Date(millis))
    }
}

/** "16:00" today, "Tomorrow 08:15", otherwise "Fri 18:00". */
@Composable
private fun reminderTime(millis: Long): String {
    val now = Calendar.getInstance()
    val t = Calendar.getInstance().apply { timeInMillis = millis }
    val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    return when {
        sameDay(now, t) -> clock(millis)
        sameDay(tomorrow, t) -> "${stringResource(R.string.home_tomorrow)} ${clock(millis)}"
        else -> SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(millis))
    }
}

internal enum class ReminderDay { TODAY, TOMORROW, LATER }

internal fun reminderDay(millis: Long): ReminderDay {
    val now = Calendar.getInstance()
    val t = Calendar.getInstance().apply { timeInMillis = millis }
    val tomorrow = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    return when {
        sameDay(now, t) || millis < now.timeInMillis -> ReminderDay.TODAY
        sameDay(tomorrow, t) -> ReminderDay.TOMORROW
        else -> ReminderDay.LATER
    }
}

private fun typeIcon(m: Message): ImageVector? = when (m.type) {
    MessageType.VOICE -> Icons.Outlined.Mic
    MessageType.CHECKLIST -> Icons.Outlined.FactCheck
    else -> null
}

@Composable
private fun typeLabel(m: Message): String = when (m.type) {
    MessageType.VOICE -> stringResource(R.string.home_filter_voice)
    MessageType.IMAGE -> stringResource(R.string.home_photo)
    MessageType.CHECKLIST -> stringResource(R.string.checklist)
    MessageType.FILE -> stringResource(R.string.file)
    MessageType.TEXT -> ""
}

/** [text] with each case-insensitive match of [query] set on the soft accent. */
@Composable
internal fun highlight(text: String, query: String): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty()) return AnnotatedString(text)
    val style = SpanStyle(background = NcyTheme.colors.accentSoft, color = NcyTheme.colors.onAccentSoft)
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            val hit = text.indexOf(q, i, ignoreCase = true)
            if (hit < 0) { append(text.substring(i)); break }
            append(text.substring(i, hit))
            withStyle(style) { append(text.substring(hit, hit + q.length)) }
            i = hit + q.length
        }
    }
}

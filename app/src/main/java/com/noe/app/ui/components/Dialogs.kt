package com.noe.app.ui.components

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FormatColorReset
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.noe.app.data.Repeat
import com.noe.app.ui.theme.NoteColors
import com.noe.app.ui.theme.noteContainerColor
import java.util.Calendar
import java.util.TimeZone

// ---------- Note color ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val dark = isDarkSurface()
    val scheme = MaterialTheme.colorScheme
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NoteColors.forEachIndexed { index, color ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(noteContainerColor(index, dark, scheme.surfaceContainerHighest))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) scheme.primary else scheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(index) }
                    .semantics { contentDescription = color.name },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isSelected -> Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = scheme.primary,
                    )
                    index == 0 -> Icon(
                        Icons.Outlined.FormatColorReset,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note color") },
        text = { ColorPicker(selected = selected, onSelect = onSelect) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

// ---------- Labels ----------

@Composable
fun LabelDialog(
    allLabels: List<String>,
    checked: Set<String>,
    onToggle: (label: String, on: Boolean) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newLabel by remember { mutableStateOf("") }

    fun create() {
        val clean = newLabel.trim()
        if (clean.isNotEmpty()) {
            onCreate(clean)
            newLabel = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Labels") },
        text = {
            Column {
                OutlinedTextField(
                    value = newLabel,
                    onValueChange = { newLabel = it },
                    singleLine = true,
                    placeholder = { Text("Create a new label") },
                    trailingIcon = {
                        if (newLabel.isNotBlank()) {
                            TextButton(onClick = { create() }) { Text("Add") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(top = 8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(allLabels, key = { it }) { label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(label, label !in checked) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MorphCheck(
                                checked = label in checked,
                                onCheckedChange = { onToggle(label, it) },
                                tint = MaterialTheme.colorScheme.primary,
                                checkColor = MaterialTheme.colorScheme.onPrimary,
                                size = 22.dp,
                                touch = 48.dp,
                            )
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

// ---------- Reminders ----------

private fun toPickerDate(ms: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = ms }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun fromPicker(dateUtc: Long, hour: Int, minute: Int): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateUtc }
    return Calendar.getInstance().apply {
        clear()
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            hour,
            minute,
            0,
        )
    }.timeInMillis
}

private fun atTime(daysAhead: Int, hour: Int, minute: Int = 0): Long =
    Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysAhead)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

@Composable
private fun ReminderOption(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderDialog(
    current: Long?,
    currentRepeat: Repeat,
    onSet: (Long, Repeat) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var pickedDate by remember { mutableStateOf<Long?>(null) }
    var repeat by remember { mutableStateOf(currentRepeat) }
    val now = System.currentTimeMillis()

    when (step) {
        0 -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Remind me") },
            text = {
                Column {
                    Text(
                        text = "Repeat",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                    FlowRow(
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Repeat.entries.forEach { option ->
                            FilterChip(
                                selected = repeat == option,
                                onClick = { repeat = option },
                                label = { Text(option.label) },
                                shape = CircleShape,
                            )
                        }
                    }
                    ReminderOption(Icons.Outlined.Schedule, "In 1 hour") { onSet(now + 60L * 60L * 1000L, repeat) }
                    val tonight = atTime(0, 20)
                    if (tonight > now + 30L * 60L * 1000L) {
                        ReminderOption(Icons.Outlined.WbTwilight, "This evening, 20:00") { onSet(tonight, repeat) }
                    }
                    ReminderOption(Icons.Outlined.Alarm, "Tomorrow morning, 08:00") { onSet(atTime(1, 8), repeat) }
                    ReminderOption(Icons.Outlined.CalendarMonth, "Pick date and time") { step = 1 }
                    if (current != null) {
                        ReminderOption(Icons.Outlined.NotificationsOff, "Remove reminder") { onClear() }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        )

        1 -> {
            val state = rememberDatePickerState(
                initialSelectedDateMillis = toPickerDate(current ?: now),
            )
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(
                        onClick = {
                            pickedDate = state.selectedDateMillis
                            if (pickedDate != null) step = 2
                        },
                    ) { Text("Next") }
                },
                dismissButton = { TextButton(onClick = { step = 0 }) { Text("Back") } },
            ) {
                DatePicker(state = state)
            }
        }

        else -> {
            val initial = Calendar.getInstance().apply { timeInMillis = current ?: now }
            val timeState = rememberTimePickerState(
                initialHour = if (current != null) initial.get(Calendar.HOUR_OF_DAY) else 9,
                initialMinute = if (current != null) initial.get(Calendar.MINUTE) else 0,
                is24Hour = DateFormat.is24HourFormat(context),
            )
            AlertDialog(
                onDismissRequest = onDismiss,
                text = { TimePicker(state = timeState) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val date = pickedDate
                            if (date != null) {
                                val at = fromPicker(date, timeState.hour, timeState.minute)
                                if (at <= System.currentTimeMillis()) {
                                    Toast.makeText(context, "Pick a time in the future", Toast.LENGTH_SHORT).show()
                                } else {
                                    onSet(at, repeat)
                                }
                            }
                        },
                    ) { Text("Set reminder") }
                },
                dismissButton = { TextButton(onClick = { step = 1 }) { Text("Back") } },
            )
        }
    }
}

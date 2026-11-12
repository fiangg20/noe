package com.noe.app.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AddAlert
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileCopy
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.TextFormat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.noe.app.data.ChecklistItem
import com.noe.app.data.Repeat
import com.noe.app.data.Reminders
import com.noe.app.ui.EditorState
import com.noe.app.ui.NotesViewModel
import com.noe.app.ui.components.ColorPickerDialog
import com.noe.app.ui.components.FormatBar
import com.noe.app.ui.components.Fmt
import com.noe.app.ui.components.LabelDialog
import com.noe.app.ui.components.MorphCheck
import com.noe.app.ui.components.ReminderDialog
import com.noe.app.ui.components.SmallPill
import com.noe.app.ui.components.SpanTransformation
import com.noe.app.ui.components.WavyProgress
import com.noe.app.ui.components.isDarkSurface
import com.noe.app.ui.components.shareText
import com.noe.app.ui.theme.noteContainerColor
import com.noe.app.ui.theme.noteContentColor

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(vm: NotesViewModel, editor: EditorState) {
    val context = LocalContext.current
    val dark = isDarkSurface()
    val scheme = MaterialTheme.colorScheme

    val container by animateColorAsState(
        targetValue = noteContainerColor(editor.colorIndex, dark, scheme.surface),
        label = "editorBackground",
    )
    val content = noteContentColor(editor.colorIndex, dark, scheme.onSurface)

    var showColor by remember { mutableStateOf(false) }
    var showLabels by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showFormat by rememberSaveable { mutableStateOf(false) }

    val titleFocus = remember { FocusRequester() }
    val existing = vm.notes.firstOrNull { it.id == editor.id }

    BackHandler { vm.closeEditor() }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "Notifications are turned off, so the reminder won't show.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        val blank = editor.title.isBlank() && editor.body.isBlank() &&
            editor.items.all { it.text.isBlank() }
        if (blank) runCatching { titleFocus.requestFocus() }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = container,
        contentColor = content,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { vm.closeEditor() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { editor.pinned = !editor.pinned }) {
                        Icon(
                            imageVector = if (editor.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (editor.pinned) "Unpin" else "Pin",
                        )
                    }
                    IconButton(onClick = { showReminder = true }) {
                        Icon(
                            imageVector = if (editor.reminderAt != null) Icons.Outlined.NotificationsActive
                            else Icons.Outlined.AddAlert,
                            contentDescription = "Reminder",
                        )
                    }
                    IconButton(onClick = { vm.archiveFromEditor() }) {
                        Icon(
                            imageVector = if (editor.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                            contentDescription = if (editor.archived) "Unarchive" else "Archive",
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Copy to clipboard") },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                onClick = {
                                    showMenu = false
                                    val text = editor.toNote().shareText()
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Noe", text))
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                leadingIcon = { Icon(Icons.Outlined.FileCopy, null) },
                                onClick = {
                                    showMenu = false
                                    vm.duplicateFromEditor()
                                    Toast.makeText(context, "Note duplicated", Toast.LENGTH_SHORT).show()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                onClick = {
                                    showMenu = false
                                    shareText(context, editor.toNote().shareText())
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                onClick = {
                                    showMenu = false
                                    vm.trashFromEditor()
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                    navigationIconContentColor = content,
                    actionIconContentColor = content,
                ),
            )
        },
        bottomBar = {
            Column {
            AnimatedVisibility(
                visible = showFormat && !editor.isChecklist,
                enter = expandVertically() + androidx.compose.animation.fadeIn(),
                exit = shrinkVertically() + androidx.compose.animation.fadeOut(),
            ) {
                FormatBar(
                    format = editor.currentFormat(),
                    tint = content,
                    onToggle = { editor.toggleFormat(it) },
                    onSize = { editor.setTextSize(it) },
                    onClear = { editor.clearFormatting() },
                )
            }
            BottomAppBar(
                containerColor = Color.Transparent,
                contentColor = content,
                actions = {
                    IconButton(onClick = { showColor = true }) {
                        Icon(Icons.Outlined.Palette, contentDescription = "Color")
                    }
                    IconButton(onClick = { showLabels = true }) {
                        Icon(Icons.Outlined.Sell, contentDescription = "Labels")
                    }
                    if (!editor.isChecklist) {
                        IconButton(onClick = { showFormat = !showFormat }) {
                            Icon(
                                imageVector = Icons.Outlined.TextFormat,
                                contentDescription = "Text formatting",
                                tint = if (showFormat) scheme.primary else content,
                            )
                        }
                    }
                    IconButton(onClick = { editor.toggleChecklist() }) {
                        Icon(
                            imageVector = if (editor.isChecklist) Icons.Outlined.Description
                            else Icons.Outlined.Checklist,
                            contentDescription = if (editor.isChecklist) "Switch to text" else "Switch to checklist",
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = run {
                            val words = editor.wordCount()
                            val wordText = if (words == 1) "1 word" else "$words words"
                            wordText + " · " +
                                if (existing != null) "Edited ${Fmt.relative(existing.updatedAt)}" else "New note"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = content.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
            )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            BasicTextField(
                value = editor.title,
                onValueChange = { editor.title = it },
                textStyle = MaterialTheme.typography.headlineSmall.copy(color = content),
                cursorBrush = SolidColor(content),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocus),
                decorationBox = { inner ->
                    Box {
                        if (editor.title.isEmpty()) {
                            Text(
                                text = "Title",
                                style = MaterialTheme.typography.headlineSmall,
                                color = content.copy(alpha = 0.5f),
                            )
                        }
                        inner()
                    }
                },
            )

            val reminder = editor.reminderAt
            if (reminder != null || editor.labels.isNotEmpty()) {
                Spacer(Modifier.size(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (reminder != null) {
                        SmallPill(
                            text = Fmt.dateTime(reminder) +
                                if (editor.repeat != Repeat.None) " · ${editor.repeat.label}" else "",
                            color = content,
                            icon = Icons.Outlined.Alarm,
                            strike = reminder < System.currentTimeMillis() && editor.repeat == Repeat.None,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showReminder = true },
                        )
                    }
                    editor.labels.forEach { label ->
                        SmallPill(
                            text = label,
                            color = content,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showLabels = true },
                        )
                    }
                }
            }

            Spacer(Modifier.size(12.dp))

            if (editor.isChecklist) {
                ChecklistEditor(editor = editor, content = content, container = container)
            } else {
                val bodyTransformation = remember(editor.spans) { SpanTransformation(editor.spans) }
                BasicTextField(
                    value = editor.bodyValue,
                    onValueChange = { editor.onBodyChange(it) },
                    visualTransformation = bodyTransformation,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = content,
                        lineHeight = TextUnit.Unspecified,
                    ),
                    cursorBrush = SolidColor(content),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 320.dp),
                    decorationBox = { inner ->
                        Box {
                            if (editor.body.isEmpty()) {
                                Text(
                                    text = "Note",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = content.copy(alpha = 0.5f),
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }
    }

    if (showColor) {
        ColorPickerDialog(
            selected = editor.colorIndex,
            onSelect = { editor.colorIndex = it },
            onDismiss = { showColor = false },
        )
    }

    if (showLabels) {
        LabelDialog(
            allLabels = (vm.labels + editor.labels).distinct().sortedBy { it.lowercase() },
            checked = editor.labels.toSet(),
            onToggle = { label, on ->
                if (on) {
                    if (label !in editor.labels) editor.labels.add(label)
                } else {
                    editor.labels.remove(label)
                }
            },
            onCreate = { if (it !in editor.labels) editor.labels.add(it) },
            onDismiss = { showLabels = false },
        )
    }

    if (showReminder) {
        ReminderDialog(
            current = editor.reminderAt,
            currentRepeat = editor.repeat,
            onSet = { at, repeat ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.canNotify(context)) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                editor.reminderAt = at
                editor.repeat = repeat
                showReminder = false
            },
            onClear = {
                editor.reminderAt = null
                editor.repeat = Repeat.None
                showReminder = false
            },
            onDismiss = { showReminder = false },
        )
    }
}

@Composable
private fun ChecklistEditor(editor: EditorState, content: Color, container: Color) {
    var focusId by remember { mutableStateOf<String?>(null) }
    var showDone by rememberSaveable { mutableStateOf(true) }

    val active = editor.items.filter { !it.checked }
    val done = editor.items.filter { it.checked }

    val (doneCount, totalCount) = editor.progress()

    Column {
        if (totalCount > 0) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WavyProgress(
                    progress = doneCount.toFloat() / totalCount,
                    color = content,
                    animate = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "$doneCount of $totalCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = content.copy(alpha = 0.8f),
                )
            }
        }

        active.forEach { item ->
            key(item.id) {
                ChecklistRow(
                    editor = editor,
                    item = item,
                    content = content,
                    container = container,
                    focusId = focusId,
                    onFocusHandled = { focusId = null },
                    onFocusRequest = { focusId = it },
                )
            }
        }

        TextButton(
            onClick = {
                val fresh = ChecklistItem()
                val at = editor.items.indexOfLast { !it.checked } + 1
                editor.items.add(at, fresh)
                focusId = fresh.id
            },
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = content)
            Spacer(Modifier.width(8.dp))
            Text("Add item", color = content)
        }

        if (done.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .clickable { showDone = !showDone }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (showDone) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = content,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${done.size} completed",
                    style = MaterialTheme.typography.labelLarge,
                    color = content.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        editor.items.removeAll { it.checked }
                        if (editor.items.isEmpty()) editor.items.add(ChecklistItem())
                    },
                ) {
                    Text("Clear", color = content)
                }
            }
            if (showDone) {
                done.forEach { item ->
                    key(item.id) {
                        ChecklistRow(
                            editor = editor,
                            item = item,
                            content = content,
                            container = container,
                            focusId = focusId,
                            onFocusHandled = { focusId = null },
                            onFocusRequest = { focusId = it },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    editor: EditorState,
    item: ChecklistItem,
    content: Color,
    container: Color,
    focusId: String?,
    onFocusHandled: () -> Unit,
    onFocusRequest: (String) -> Unit,
) {
    val requester = remember { FocusRequester() }

    LaunchedEffect(focusId) {
        if (focusId == item.id) {
            runCatching { requester.requestFocus() }
            onFocusHandled()
        }
    }

    fun indexOfItem(): Int = editor.items.indexOfFirst { it.id == item.id }

    fun removeItem() {
        val i = indexOfItem()
        if (i >= 0 && editor.items.size > 1) editor.items.removeAt(i)
        else if (i >= 0) editor.items[i] = editor.items[i].copy(text = "", checked = false)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = (-8).dp),
        verticalAlignment = Alignment.Top,
    ) {
        MorphCheck(
            checked = item.checked,
            onCheckedChange = { checked ->
                val i = indexOfItem()
                if (i >= 0) editor.items[i] = editor.items[i].copy(checked = checked)
            },
            tint = content,
            checkColor = container,
            modifier = Modifier.padding(top = 4.dp),
        )
        BasicTextField(
            value = item.text,
            onValueChange = { value ->
                val i = indexOfItem()
                if (i >= 0) {
                    if ('\n' in value) {
                        // Enter splits the item and moves to a new one.
                        val cut = value.indexOf('\n')
                        val after = value.substring(cut + 1).replace("\n", " ")
                        editor.items[i] = editor.items[i].copy(text = value.substring(0, cut))
                        val fresh = ChecklistItem(text = after)
                        editor.items.add(i + 1, fresh)
                        onFocusRequest(fresh.id)
                    } else {
                        editor.items[i] = editor.items[i].copy(text = value)
                    }
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (item.checked) content.copy(alpha = 0.55f) else content,
                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
            ),
            cursorBrush = SolidColor(content),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
                .focusRequester(requester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && item.text.isEmpty()) {
                        removeItem()
                        true
                    } else {
                        false
                    }
                },
            decorationBox = { inner ->
                Box {
                    if (item.text.isEmpty()) {
                        Text(
                            text = "List item",
                            style = MaterialTheme.typography.bodyLarge,
                            color = content.copy(alpha = 0.45f),
                        )
                    }
                    inner()
                }
            },
        )
        IconButton(onClick = { removeItem() }) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "Remove item",
                modifier = Modifier.size(18.dp),
                tint = content.copy(alpha = 0.6f),
            )
        }
    }
}

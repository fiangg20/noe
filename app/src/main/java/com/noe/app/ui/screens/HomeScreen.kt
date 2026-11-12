package com.noe.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noe.app.data.Note
import com.noe.app.ui.NotesViewModel
import com.noe.app.ui.Section
import com.noe.app.ui.components.ColorPickerDialog
import com.noe.app.ui.components.EmptyState
import com.noe.app.ui.components.LabelDialog
import com.noe.app.ui.components.NoteCard
import com.noe.app.ui.components.shareText
import kotlinx.coroutines.launch

private fun Section.title(): String = when (this) {
    Section.Notes -> "Notes"
    Section.Reminders -> "Reminders"
    Section.Archive -> "Archive"
    Section.Trash -> "Trash"
    is Section.Label -> name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: NotesViewModel, gridState: LazyStaggeredGridState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = androidx.compose.material3.rememberDrawerState(DrawerValue.Closed)
    val snackbar = remember { SnackbarHostState() }

    val selected = remember { mutableStateListOf<String>() }
    var fabExpanded by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var confirmEmptyTrash by remember { mutableStateOf(false) }
    var confirmDeleteForever by remember { mutableStateOf(false) }
    var showRenameLabel by remember { mutableStateOf(false) }
    var showDeleteLabel by remember { mutableStateOf(false) }

    val section = vm.section
    val notes = vm.visibleNotes
    val inSelection = selected.isNotEmpty()
    val inTrash = section == Section.Trash

    LaunchedEffect(section) { selected.clear() }

    // Show an Undo snackbar whenever the ViewModel posts one.
    val undoKey = vm.undo?.key
    LaunchedEffect(undoKey) {
        val action = vm.undo ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = action.message,
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.performUndo() else vm.consumeUndo()
    }

    fun toggleSelect(id: String) {
        if (id in selected) selected.remove(id) else selected.add(id)
    }

    fun goTo(target: Section) {
        scope.launch { drawerState.close() }
        vm.openSection(target)
    }

    BackHandler(enabled = !inSelection && vm.query.isEmpty() && section != Section.Notes) {
        vm.openSection(Section.Notes)
    }
    BackHandler(enabled = !inSelection && vm.query.isNotEmpty()) { vm.query = "" }
    BackHandler(enabled = inSelection) { selected.clear() }
    BackHandler(enabled = fabExpanded) { fabExpanded = false }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Noe",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                    )
                    DrawerItem("Notes", Icons.Outlined.Lightbulb, section == Section.Notes) { goTo(Section.Notes) }
                    DrawerItem("Reminders", Icons.Outlined.Notifications, section == Section.Reminders) {
                        goTo(Section.Reminders)
                    }
                    if (vm.labels.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
                        Text(
                            text = "Labels",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                        )
                        vm.labels.forEach { label ->
                            DrawerItem(label, Icons.Outlined.Sell, section == Section.Label(label)) {
                                goTo(Section.Label(label))
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
                    DrawerItem("Archive", Icons.Outlined.Archive, section == Section.Archive) { goTo(Section.Archive) }
                    DrawerItem("Trash", Icons.Outlined.Delete, section == Section.Trash) { goTo(Section.Trash) }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
                    DrawerItem("Settings", Icons.Outlined.Settings, false) {
                        scope.launch { drawerState.close() }
                        vm.showSettings = true
                    }
                }
            }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val showFab = !inTrash && section != Section.Archive && !inSelection

            Scaffold(
                containerColor = MaterialTheme.colorScheme.surface,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbar,
                        modifier = Modifier.padding(bottom = if (showFab) 72.dp else 0.dp),
                    )
                },
                topBar = {
                    if (inSelection) {
                        val ids = selected.toList()
                        val chosen = vm.notes.filter { it.id in ids }
                        val allPinned = chosen.isNotEmpty() && chosen.all { it.pinned }
                        TopAppBar(
                            title = { Text("${selected.size} selected") },
                            navigationIcon = {
                                IconButton(onClick = { selected.clear() }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear selection")
                                }
                            },
                            actions = {
                                if (inTrash) {
                                    IconButton(onClick = { vm.restore(ids); selected.clear() }) {
                                        Icon(Icons.Outlined.RestoreFromTrash, contentDescription = "Restore")
                                    }
                                    IconButton(onClick = { confirmDeleteForever = true }) {
                                        Icon(Icons.Outlined.DeleteForever, contentDescription = "Delete forever")
                                    }
                                } else {
                                    if (section != Section.Archive) {
                                        IconButton(onClick = { vm.setPinned(ids, !allPinned); selected.clear() }) {
                                            Icon(
                                                imageVector = if (allPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                                contentDescription = if (allPinned) "Unpin" else "Pin",
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { vm.setArchived(ids, section != Section.Archive); selected.clear() },
                                    ) {
                                        Icon(
                                            imageVector = if (section == Section.Archive) Icons.Outlined.Unarchive
                                            else Icons.Outlined.Archive,
                                            contentDescription = "Archive",
                                        )
                                    }
                                    IconButton(onClick = { vm.trash(ids); selected.clear() }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Move to trash")
                                    }
                                    Box {
                                        IconButton(onClick = { showMenu = true }) {
                                            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                                        }
                                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                            DropdownMenuItem(
                                                text = { Text("Change color") },
                                                leadingIcon = { Icon(Icons.Outlined.Palette, null) },
                                                onClick = { showMenu = false; showColorDialog = true },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Labels") },
                                                leadingIcon = { Icon(Icons.Outlined.Sell, null) },
                                                onClick = { showMenu = false; showLabelDialog = true },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Duplicate") },
                                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                                onClick = {
                                                    showMenu = false
                                                    vm.duplicate(ids)
                                                    selected.clear()
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Share") },
                                                leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                                onClick = {
                                                    showMenu = false
                                                    shareText(
                                                        context,
                                                        chosen.joinToString("\n\n---\n\n") { it.shareText() },
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    } else {
                        SearchPill(
                            query = vm.query,
                            onQueryChange = { vm.query = it },
                            gridView = vm.settings.gridView,
                            onMenu = { scope.launch { drawerState.open() } },
                            onToggleLayout = { vm.updateSettings { it.copy(gridView = !it.gridView) } },
                        )
                    }
                },
            ) { padding ->
                val pinned = notes.filter { it.pinned }
                val others = notes.filter { !it.pinned }

                val header: @Composable () -> Unit = {
                    SectionHeader(
                        title = section.title(),
                        section = section,
                        hasNotes = notes.isNotEmpty(),
                        onEmptyTrash = { confirmEmptyTrash = true },
                        onRename = { showRenameLabel = true },
                        onDeleteLabel = { showDeleteLabel = true },
                    )
                }

                if (notes.isEmpty()) {
                    Column(modifier = Modifier.padding(padding)) {
                        header()
                        Box(modifier = Modifier.weight(1f)) {
                            EmptyHome(section = section, searching = vm.query.isNotBlank())
                        }
                    }
                } else {
                    val clickNote: (Note) -> Unit = { note ->
                        if (inSelection || inTrash) toggleSelect(note.id) else vm.openNote(note.id)
                    }

                    LazyVerticalStaggeredGrid(
                        columns = if (vm.settings.gridView) StaggeredGridCells.Adaptive(170.dp)
                        else StaggeredGridCells.Fixed(1),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding() + 120.dp,
                        ),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "header", span = StaggeredGridItemSpan.FullLine) { header() }

                        if (pinned.isNotEmpty()) {
                            item(key = "pinned_label", span = StaggeredGridItemSpan.FullLine) {
                                GroupLabel("Pinned")
                            }
                            items(pinned, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    selected = note.id in selected,
                                    onClick = { clickNote(note) },
                                    onLongClick = { toggleSelect(note.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                            if (others.isNotEmpty()) {
                                item(key = "others_label", span = StaggeredGridItemSpan.FullLine) {
                                    GroupLabel("Others")
                                }
                            }
                        }
                        items(others, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                selected = note.id in selected,
                                onClick = { clickNote(note) },
                                onLongClick = { toggleSelect(note.id) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }

            if (showFab) {
                if (fabExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { fabExpanded = false },
                    )
                }
                NewNoteFab(
                    expanded = fabExpanded,
                    onToggle = { fabExpanded = !fabExpanded },
                    onChecklist = {
                        fabExpanded = false
                        vm.openNew(checklist = true)
                    },
                    onNote = {
                        fabExpanded = false
                        vm.openNew(checklist = false)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                )
            }
        }
    }

    // ---------- Dialogs ----------

    if (showColorDialog) {
        val chosen = vm.notes.filter { it.id in selected }
        ColorPickerDialog(
            selected = chosen.map { it.colorIndex }.distinct().singleOrNull() ?: -1,
            onSelect = { vm.setColor(selected.toList(), it) },
            onDismiss = { showColorDialog = false },
        )
    }

    if (showLabelDialog) {
        val chosen = vm.notes.filter { it.id in selected }
        val common: Set<String> =
            if (chosen.isEmpty()) emptySet()
            else chosen.map { it.labels.toSet() }.reduce { a, b -> a intersect b }
        LabelDialog(
            allLabels = vm.labels,
            checked = common,
            onToggle = { label, on ->
                vm.update(selected.toList()) { n ->
                    when {
                        on && label !in n.labels -> n.copy(labels = n.labels + label)
                        !on -> n.copy(labels = n.labels - label)
                        else -> n
                    }
                }
            },
            onCreate = { vm.addLabel(selected.toList(), it) },
            onDismiss = { showLabelDialog = false },
        )
    }

    if (confirmDeleteForever) {
        AlertDialog(
            onDismissRequest = { confirmDeleteForever = false },
            title = { Text("Delete forever?") },
            text = { Text("The selected notes will be permanently deleted. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteForever(selected.toList())
                        selected.clear()
                        confirmDeleteForever = false
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteForever = false }) { Text("Cancel") } },
        )
    }

    if (confirmEmptyTrash) {
        AlertDialog(
            onDismissRequest = { confirmEmptyTrash = false },
            title = { Text("Empty trash?") },
            text = { Text("All notes in the trash will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { vm.emptyTrash(); confirmEmptyTrash = false }) { Text("Empty trash") }
            },
            dismissButton = { TextButton(onClick = { confirmEmptyTrash = false }) { Text("Cancel") } },
        )
    }

    val labelSection = section as? Section.Label
    if (showRenameLabel && labelSection != null) {
        var text by remember(labelSection.name) { mutableStateOf(labelSection.name) }
        AlertDialog(
            onDismissRequest = { showRenameLabel = false },
            title = { Text("Rename label") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.renameLabel(labelSection.name, text)
                        showRenameLabel = false
                    },
                ) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { showRenameLabel = false }) { Text("Cancel") } },
        )
    }

    if (showDeleteLabel && labelSection != null) {
        AlertDialog(
            onDismissRequest = { showDeleteLabel = false },
            title = { Text("Delete label?") },
            text = { Text("The label \"${labelSection.name}\" will be removed from all notes. The notes themselves are kept.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteLabel(labelSection.name)
                        showDeleteLabel = false
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteLabel = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DrawerItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}

@Composable
private fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    gridView: Boolean,
    onMenu: () -> Unit,
    onToggleLayout: () -> Unit,
) {
    Box(
        modifier = Modifier
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search your notes",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                    }
                } else {
                    IconButton(onClick = onToggleLayout) {
                        Icon(
                            imageVector = if (gridView) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                            contentDescription = if (gridView) "List view" else "Grid view",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    section: Section,
    hasNotes: Boolean,
    onEmptyTrash: () -> Unit,
    onRename: () -> Unit,
    onDeleteLabel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (section is Section.Label) {
                IconButton(onClick = onRename) { Icon(Icons.Outlined.Edit, contentDescription = "Rename label") }
                IconButton(onClick = onDeleteLabel) { Icon(Icons.Outlined.Delete, contentDescription = "Delete label") }
            }
            if (section == Section.Trash && hasNotes) {
                TextButton(onClick = onEmptyTrash) { Text("Empty trash") }
            }
        }
        if (section == Section.Trash) {
            Text(
                text = "Notes in the trash are deleted after 30 days. Tap a note to select it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
    )
}

@Composable
private fun EmptyHome(section: Section, searching: Boolean) {
    when {
        searching -> EmptyState(
            icon = Icons.Outlined.Search,
            title = "No matches",
            subtitle = "Nothing here matches your search.",
        )
        else -> when (section) {
            Section.Notes -> EmptyState(
                icon = Icons.Outlined.Lightbulb,
                title = "Nothing here yet",
                subtitle = "Tap the + button to write your first note or checklist.",
            )
            Section.Reminders -> EmptyState(
                icon = Icons.Outlined.Notifications,
                title = "No reminders",
                subtitle = "Notes with a reminder show up here.",
                alternateShape = true,
            )
            Section.Archive -> EmptyState(
                icon = Icons.Outlined.Archive,
                title = "Archive is empty",
                subtitle = "Archive notes you want to keep but don't need every day.",
                alternateShape = true,
            )
            Section.Trash -> EmptyState(
                icon = Icons.Outlined.Delete,
                title = "Trash is empty",
                subtitle = "Deleted notes stay here for 30 days.",
            )
            is Section.Label -> EmptyState(
                icon = Icons.Outlined.Sell,
                title = "No notes with this label",
                subtitle = "Add this label to a note from the editor.",
                alternateShape = true,
            )
        }
    }
}


/** Expandable "new note" button. Springs, a rotating plus and a morphing corner give it the Expressive feel. */
@Composable
private fun NewNoteFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onChecklist: () -> Unit,
    onNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 135f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fabRotation",
    )
    val corner by animateDpAsState(
        targetValue = if (expanded) 30.dp else 18.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fabCorner",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() +
                scaleIn(initialScale = 0.6f, transformOrigin = TransformOrigin(1f, 1f)) +
                slideInVertically { it / 2 },
            exit = fadeOut() +
                scaleOut(targetScale = 0.6f, transformOrigin = TransformOrigin(1f, 1f)) +
                slideOutVertically { it / 2 },
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MenuFab("Checklist", Icons.Outlined.Checklist, onChecklist)
                MenuFab("Note", Icons.Outlined.Description, onNote)
            }
        }
        FloatingActionButton(
            onClick = onToggle,
            shape = RoundedCornerShape(corner),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = if (expanded) "Close menu" else "New note",
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

@Composable
private fun MenuFab(label: String, icon: ImageVector, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

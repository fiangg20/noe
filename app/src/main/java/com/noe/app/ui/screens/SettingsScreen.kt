package com.noe.app.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.noe.app.data.SettingChoices
import com.noe.app.ui.components.ColorPicker
import com.noe.app.ui.components.CookieShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.noe.app.data.SortOrder
import com.noe.app.data.ThemeMode
import com.noe.app.ui.NotesViewModel
import com.noe.app.ui.theme.SeedPalettes
import com.noe.app.ui.theme.hsl

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: NotesViewModel) {
    val context = LocalContext.current
    val settings = vm.settings
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    BackHandler { vm.showSettings = false }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
            }.isSuccess
            Toast.makeText(
                context,
                if (ok) "Backup saved" else "Couldn't save the backup",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: error("empty file")
                vm.importJson(text)
            }.onSuccess { count ->
                Toast.makeText(
                    context,
                    if (count == 0) "Nothing new to import" else "Imported $count notes",
                    Toast.LENGTH_SHORT,
                ).show()
            }.onFailure {
                Toast.makeText(context, "That file isn't a Noe backup", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { vm.showSettings = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsGroup("Appearance", Icons.Outlined.Palette) {
                Text("Theme", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val modes = ThemeMode.entries
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { vm.updateSettings { it.copy(themeMode = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                            label = { Text(mode.name) },
                        )
                    }
                }

                if (dynamicAvailable) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dynamic color", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Use colors from your wallpaper",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = { on -> vm.updateSettings { it.copy(dynamicColor = on) } },
                        )
                    }
                }

                if (!dynamicAvailable || !settings.dynamicColor) {
                    Spacer(Modifier.height(16.dp))
                    Text("Accent color", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SeedPalettes.forEachIndexed { index, palette ->
                            val isSelected = settings.seedIndex == index
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(hsl(palette.hue, 0.55f, 0.45f))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { vm.updateSettings { it.copy(seedIndex = index) } }
                                    .semantics { contentDescription = palette.name },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = androidx.compose.ui.graphics.Color.White,
                                    )
                                }
                            }
                        }
                    }
                }

                SwitchRow(
                    title = "Pure black dark mode",
                    subtitle = "Black backgrounds, easier on OLED screens",
                    checked = settings.amoled,
                    onChange = { on -> vm.updateSettings { it.copy(amoled = on) } },
                )

                Spacer(Modifier.height(16.dp))
                Text("Text size", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ChoiceRow(SettingChoices.textSizeLabels, settings.textSizeIndex) { i ->
                    vm.updateSettings { it.copy(textSizeIndex = i) }
                }

                Spacer(Modifier.height(16.dp))
                Text("Corner style", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ChoiceRow(SettingChoices.cornerLabels, settings.cornerIndex) { i ->
                    vm.updateSettings { it.copy(cornerIndex = i) }
                }
                Spacer(Modifier.height(12.dp))
                val previewCorner by animateDpAsState(
                    targetValue = SettingChoices.cornerDp.getOrElse(settings.cornerIndex) { 28 }.dp,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "cornerPreview",
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(previewCorner),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("This is how note cards look", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }

            SettingsGroup("Notes", Icons.Outlined.Description) {
                Text("Layout", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = settings.gridView,
                        onClick = { vm.updateSettings { it.copy(gridView = true) } },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        label = { Text("Grid") },
                    )
                    SegmentedButton(
                        selected = !settings.gridView,
                        onClick = { vm.updateSettings { it.copy(gridView = false) } },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        label = { Text("List") },
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("Sort by", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                val orders = SortOrder.entries
                val names = mapOf(
                    SortOrder.Edited to "Edited",
                    SortOrder.Created to "Created",
                    SortOrder.Title to "Title",
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    orders.forEachIndexed { index, order ->
                        SegmentedButton(
                            selected = settings.sortOrder == order,
                            onClick = { vm.updateSettings { it.copy(sortOrder = order) } },
                            shape = SegmentedButtonDefaults.itemShape(index, orders.size),
                            label = { Text(names.getValue(order)) },
                        )
                    }
                }

                if (settings.gridView) {
                    Spacer(Modifier.height(16.dp))
                    Text("Columns", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    ChoiceRow(SettingChoices.columnLabels, settings.columnsIndex) { i ->
                        vm.updateSettings { it.copy(columnsIndex = i) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Preview length", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ChoiceRow(SettingChoices.previewLabels, settings.previewIndex) { i ->
                    vm.updateSettings { it.copy(previewIndex = i) }
                }

                Spacer(Modifier.height(16.dp))
                SwitchRow(
                    title = "Show summary",
                    subtitle = "Note, task and reminder counts under the title",
                    checked = settings.showStats,
                    onChange = { on -> vm.updateSettings { it.copy(showStats = on) } },
                )

                Spacer(Modifier.height(16.dp))
                Text("Color for new notes", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                ColorPicker(
                    selected = settings.defaultColor,
                    onSelect = { i -> vm.updateSettings { it.copy(defaultColor = i) } },
                )
            }

            SettingsGroup("Backup", Icons.Outlined.Backup) {
                Text(
                    "Save all your notes to a file, or restore them from one. Importing merges with your current notes and keeps the newest version of each.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { exportLauncher.launch("noe-backup.json") }) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export")
                    }
                    FilledTonalButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import")
                    }
                }
            }

            SettingsGroup("About", Icons.Outlined.Info) {
                val active = vm.notes.count { !it.trashed && !it.archived }
                val archived = vm.notes.count { !it.trashed && it.archived }
                val trashed = vm.notes.count { it.trashed }
                Text("Noe 1.1", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$active notes, $archived archived, $trashed in trash, ${vm.labels.size} labels",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your notes are stored only on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsGroup(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    val badge = remember { CookieShape(7, 0.09f) }
    Column {
        Row(
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(badge)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(36.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(22.dp)) { content() }
        }
    }
}

@Composable
private fun ChoiceRow(labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index, labels.size),
                label = { Text(label, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

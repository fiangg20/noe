package com.noe.app.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.OutlinedTextField
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
            SettingsGroup("Profile") {
                Text(
                    "Your name shows up in the greeting on the home screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = settings.userName,
                    onValueChange = { v -> vm.updateSettings { it.copy(userName = v.take(24)) } },
                    singleLine = true,
                    label = { Text("Your name") },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SettingsGroup("Appearance") {
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
            }

            SettingsGroup("Notes") {
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
            }

            SettingsGroup("Backup") {
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

            SettingsGroup("About") {
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
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(20.dp)) { content() }
        }
    }
}

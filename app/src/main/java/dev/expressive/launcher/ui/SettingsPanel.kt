package dev.expressive.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.State
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay

@Composable
fun SettingsPanel(
    wallpaperId: String,
    photo: ImageBitmap?,
    dim: Int,
    accentIndex: Int,
    onWallpaper: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onDim: (Int) -> Unit,
    onAccent: (Int) -> Unit,
    onSystem: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LaunchedEffect(Unit) {
            repeat(10) {
                delay(100)
                if (runCatching { firstFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).widthIn(max = 820.dp).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(44.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 28.dp)
            ) {
                Text(
                    text = "Make it yours",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Wallpaper, color and system shortcuts",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SectionTitle("Wallpaper")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 14.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    WallpaperPresets.forEachIndexed { index, preset ->
                        Swatch(
                            name = preset.name,
                            selected = wallpaperId == preset.id,
                            focusRequester = if (index == 0) firstFocus else null,
                            onClick = { onWallpaper(preset.id) }
                        ) {
                            PresetWallpaper(preset, Modifier.fillMaxSize(), animated = false)
                        }
                    }
                    Swatch(
                        name = "Photo",
                        selected = wallpaperId == "photo",
                        focusRequester = null,
                        onClick = { if (photo != null) onWallpaper("photo") else onPickPhoto() }
                    ) {
                        if (photo != null) {
                            Image(
                                bitmap = photo,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = "Choose a photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center).size(34.dp)
                            )
                        }
                    }
                }
                FilledTonalButton(onClick = onPickPhoto) {
                    Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(if (photo == null) "Choose a photo" else "Choose another photo")
                }

                SectionTitle("Wallpaper dimming")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Off", "Soft", "Strong").forEachIndexed { index, label ->
                        Pill(
                            label = label,
                            selected = dim == index,
                            modifier = Modifier.weight(1f),
                            onClick = { onDim(index) }
                        )
                    }
                }

                SectionTitle("Accent color")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 14.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Accents.forEachIndexed { index, accent ->
                        val selected = index == accentIndex
                        val cookie = remember { cookieMorph(9, 0.07f, 0.11f, 1.1f) }
                        val round = remember { roundedMorph(32.dp, 20.dp) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FocusSurface(
                                modifier = Modifier.size(64.dp),
                                shapeAt = if (selected) cookie else round,
                                containerColor = accent.primary,
                                ringColor = MaterialTheme.colorScheme.onSurface,
                                focusedScale = 1.12f,
                                onClick = { onAccent(index) }
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = accent.onPrimary,
                                        modifier = Modifier.align(Alignment.Center).size(28.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = accent.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                SectionTitle("System")
                Column(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SystemRow("Wi-Fi and network", Icons.Rounded.Wifi) {
                        onSystem(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    }
                    SystemRow("Display", Icons.Rounded.Tv) {
                        onSystem(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
                    }
                    SystemRow("Sound", Icons.Rounded.VolumeUp) {
                        onSystem(android.provider.Settings.ACTION_SOUND_SETTINGS)
                    }
                    SystemRow("Bluetooth", Icons.Rounded.Bluetooth) {
                        onSystem(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                    }
                    SystemRow("All system settings", Icons.Rounded.Settings) {
                        onSystem(android.provider.Settings.ACTION_SETTINGS)
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 26.dp, bottom = 2.dp)
    )
}

@Composable
private fun Swatch(
    name: String,
    selected: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val shapeAt = remember { roundedMorph(22.dp, 32.dp) }
    Column(modifier = Modifier.width(144.dp)) {
        FocusSurface(
            modifier = Modifier.fillMaxWidth().height(88.dp),
            shapeAt = shapeAt,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ringColor = MaterialTheme.colorScheme.primary,
            focusRequester = focusRequester,
            onClick = onClick
        ) {
            content()
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val idle: State<androidx.compose.ui.unit.Dp> = animateDpAsState(
        targetValue = if (selected) 28.dp else 14.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "pill"
    )
    val shapeAt = remember {
        { progress: Float, density: androidx.compose.ui.unit.Density ->
            with(density) {
                RoundedCornerShape(lerp(idle.value.toPx(), 28.dp.toPx(), progress).coerceAtLeast(0f))
            }
        }
    }
    FocusSurface(
        modifier = modifier.height(56.dp),
        shapeAt = shapeAt,
        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ringColor = MaterialTheme.colorScheme.primary,
        focusedScale = 1.04f,
        onClick = onClick
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SystemRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    val shapeAt = remember { roundedMorph(20.dp, 30.dp) }
    FocusSurface(
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shapeAt = shapeAt,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ringColor = MaterialTheme.colorScheme.primary,
        focusedScale = 1.02f,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

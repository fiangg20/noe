package dev.expressive.launcher.ui

import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.expressive.launcher.AppEntry
import dev.expressive.launcher.LauncherViewModel
import dev.expressive.launcher.launchApp
import dev.expressive.launcher.openAppInfo
import dev.expressive.launcher.openSystemSettings
import dev.expressive.launcher.toast
import dev.expressive.launcher.uninstallApp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
private data class ClockText(
    val time: String,
    val period: String,
    val date: String,
    val greeting: String
)

@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recents by viewModel.recents.collectAsStateWithLifecycle()
    val homeSignal by viewModel.homeSignal.collectAsStateWithLifecycle()
    val wallpaper by viewModel.wallpaper.collectAsStateWithLifecycle()
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val dim by viewModel.dim.collectAsStateWithLifecycle()
    val accentIndex by viewModel.accent.collectAsStateWithLifecycle()

    val isTv = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val byPackage = remember(apps) { apps.associateBy { it.packageName } }
    val favoriteApps = remember(byPackage, favorites) { favorites.mapNotNull { byPackage[it] } }
    val recentApps = remember(byPackage, recents) { recents.mapNotNull { byPackage[it] } }
    val favoriteSet = remember(favorites) { favorites.toSet() }

    val focused = remember { mutableStateOf<AppEntry?>(null) }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val initialFocus = remember { FocusRequester() }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.importPhoto(uri) { ok -> if (!ok) context.toast("Could not use that photo") }
        }
    }

    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000L - value % 60_000L)
        }
    }
    val is24 = remember { DateFormat.is24HourFormat(context) }
    val clock = remember(now) {
        val date = Date(now)
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        ClockText(
            time = SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.getDefault()).format(date),
            period = if (is24) "" else SimpleDateFormat("a", Locale.getDefault()).format(date),
            date = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(date),
            greeting = when {
                hour < 5 -> "Late night"
                hour < 12 -> "Good morning"
                hour < 18 -> "Good afternoon"
                else -> "Good evening"
            }
        )
    }

    val open: (AppEntry) -> Unit = { app ->
        viewModel.markLaunched(app.packageName)
        context.launchApp(app)
    }

    LaunchedEffect(homeSignal) {
        if (homeSignal > 0) {
            showSettings = false
            menuFor = null
            listState.animateScrollToItem(0)
        }
    }

    LaunchedEffect(apps.isNotEmpty()) {
        if (isTv && apps.isNotEmpty()) {
            repeat(10) {
                delay(80)
                if (runCatching { initialFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            }
        }
    }

    BackHandler {
        scope.launch { listState.animateScrollToItem(0) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0E15))
    ) {
        val compact = maxWidth < 600.dp
        val pad = if (compact) 20.dp else 56.dp
        val cardWidth = if (compact) 156.dp else maxWidth * 0.17f

        WallpaperLayer(
            wallpaperId = wallpaper,
            photo = photo,
            dim = dim,
            modifier = Modifier.fillMaxSize()
        )
        FocusGlow(focused, Modifier.fillMaxSize())

        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Header(
                clock = clock,
                compact = compact,
                pad = pad,
                focused = focused,
                favorites = favoriteSet,
                hint = if (isTv) "Hold OK for options" else "Long press for options",
                onSettingsFocused = { focused.value = null },
                onSettings = { showSettings = true }
            )

            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val firstSection = when {
                    favoriteApps.isNotEmpty() -> "favorites"
                    recentApps.isNotEmpty() -> "recent"
                    else -> "all"
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (favoriteApps.isNotEmpty()) {
                        item(key = "favorites") {
                            AppRow(
                                title = "Favorites",
                                apps = favoriteApps,
                                cardWidth = cardWidth,
                                pad = pad,
                                favorites = favoriteSet,
                                firstRequester = initialFocus.takeIf { firstSection == "favorites" },
                                onFocused = { focused.value = it },
                                onOpen = open,
                                onMenu = { menuFor = it }
                            )
                        }
                    }
                    if (recentApps.isNotEmpty()) {
                        item(key = "recent") {
                            AppRow(
                                title = "Recent",
                                apps = recentApps,
                                cardWidth = cardWidth,
                                pad = pad,
                                favorites = favoriteSet,
                                firstRequester = initialFocus.takeIf { firstSection == "recent" },
                                onFocused = { focused.value = it },
                                onOpen = open,
                                onMenu = { menuFor = it }
                            )
                        }
                    }
                    item(key = "all") {
                        AppRow(
                            title = "All apps",
                            apps = apps,
                            cardWidth = cardWidth,
                            pad = pad,
                            favorites = favoriteSet,
                            firstRequester = initialFocus.takeIf { firstSection == "all" },
                            onFocused = { focused.value = it },
                            onOpen = open,
                            onMenu = { menuFor = it }
                        )
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsPanel(
            wallpaperId = wallpaper,
            photo = photo,
            dim = dim,
            accentIndex = accentIndex,
            onWallpaper = { viewModel.setWallpaper(it) },
            onPickPhoto = {
                runCatching { photoPicker.launch("image/*") }
                    .onFailure { context.toast("No photo picker on this device") }
            },
            onDim = { viewModel.setDim(it) },
            onAccent = { viewModel.setAccent(it) },
            onSystem = { context.openSystemSettings(it) },
            onDismiss = { showSettings = false }
        )
    }

    menuFor?.let { app ->
        val firstAction = remember { FocusRequester() }
        LaunchedEffect(app) {
            delay(120)
            runCatching { firstAction.requestFocus() }
        }
        AlertDialog(
            onDismissRequest = { menuFor = null },
            icon = {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp)
                )
            },
            title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            menuFor = null
                            open(app)
                        },
                        modifier = Modifier.fillMaxWidth().focusRequester(firstAction)
                    ) { Text("Open") }
                    FilledTonalButton(
                        onClick = {
                            viewModel.toggleFavorite(app.packageName)
                            menuFor = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (app.packageName in favoriteSet) "Remove from favorites" else "Add to favorites")
                    }
                    FilledTonalButton(
                        onClick = {
                            menuFor = null
                            context.openAppInfo(app.packageName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("App info") }
                    if (!app.isSystem) {
                        FilledTonalButton(
                            onClick = {
                                menuFor = null
                                context.uninstallApp(app.packageName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) { Text("Uninstall") }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun FocusGlow(focused: State<AppEntry?>, modifier: Modifier) {
    val fallback = MaterialTheme.colorScheme.primaryContainer
    val color = animateColorAsState(
        targetValue = focused.value?.tint ?: fallback,
        animationSpec = tween(500),
        label = "glow"
    )
    Box(
        modifier = modifier.drawBehind {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(color.value.copy(alpha = 0.34f), Color.Transparent),
                    center = Offset(size.width * 0.14f, size.height * 0.04f),
                    radius = size.maxDimension * 0.6f
                )
            )
        }
    )
}

@Composable
private fun Header(
    clock: ClockText,
    compact: Boolean,
    pad: Dp,
    focused: State<AppEntry?>,
    favorites: Set<String>,
    hint: String,
    onSettingsFocused: () -> Unit,
    onSettings: () -> Unit
) {
    if (compact) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = pad, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Clock(clock, 72.sp)
                Spacer(Modifier.weight(1f))
                SettingsButton(onSettingsFocused, onSettings)
            }
            Spotlight(
                focused = focused,
                favorites = favorites,
                hint = hint,
                compact = true,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = pad, vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Clock(clock, 112.sp)
            Spacer(Modifier.width(48.dp))
            Spotlight(
                focused = focused,
                favorites = favorites,
                hint = hint,
                compact = false,
                modifier = Modifier.weight(1f).padding(top = 30.dp)
            )
            SettingsButton(onSettingsFocused, onSettings)
        }
    }
}

@Composable
private fun Clock(clock: ClockText, size: TextUnit) {
    Column {
        Text(
            text = clock.greeting,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.tertiary
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = clock.time,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = size,
                    lineHeight = size * 0.95f,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            if (clock.period.isNotEmpty()) {
                Text(
                    text = clock.period,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 10.dp, bottom = 12.dp)
                )
            }
        }
        Text(
            text = clock.date,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsButton(onFocused: () -> Unit, onClick: () -> Unit) {
    val shapeAt = remember { cookieMorph(8, 0.07f, 0.12f, 1.4f) }
    FocusSurface(
        modifier = Modifier.size(64.dp),
        shapeAt = shapeAt,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        ringColor = MaterialTheme.colorScheme.primary,
        focusedScale = 1.16f,
        onFocusChange = { if (it) onFocused() },
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "Settings",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.align(Alignment.Center).size(30.dp)
        )
    }
}

@Composable
private fun Spotlight(
    focused: State<AppEntry?>,
    favorites: Set<String>,
    hint: String,
    compact: Boolean,
    modifier: Modifier
) {
    val app = focused.value
    val spatial = spring<IntOffset>(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
    val iconSize = if (compact) 64.dp else 92.dp
    val nameSize = if (compact) 30.sp else 46.sp
    val base = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier = modifier.height(if (compact) 84.dp else 118.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedContent(
            targetState = app,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(spatial) { it / 3 }) togetherWith fadeOut(tween(120))
            },
            label = "spotlight"
        ) { target ->
            if (target == null) {
                Text(
                    text = "Pick something to start",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .clip(CookieShape(9, 0.07f))
                            .background(lerp(base, target.tint, 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = target.icon,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.62f)
                        )
                    }
                    Column {
                        Text(
                            text = target.label,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontSize = nameSize,
                                lineHeight = nameSize * 1.05f
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Chip(
                                text = if (target.isTv) "Made for TV" else "Phone app",
                                container = MaterialTheme.colorScheme.secondaryContainer,
                                content = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (target.packageName in favorites) {
                                Chip(
                                    text = "Favorite",
                                    container = MaterialTheme.colorScheme.tertiaryContainer,
                                    content = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            if (!compact) {
                                Text(
                                    text = hint,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
private fun AppRow(
    title: String,
    apps: List<AppEntry>,
    cardWidth: Dp,
    pad: Dp,
    favorites: Set<String>,
    firstRequester: FocusRequester?,
    onFocused: (AppEntry) -> Unit,
    onOpen: (AppEntry) -> Unit,
    onMenu: (AppEntry) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.padding(start = pad, top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Chip(
                text = apps.size.toString(),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = pad, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                AppCard(
                    app = app,
                    width = cardWidth,
                    isFavorite = app.packageName in favorites,
                    focusRequester = if (index == 0) firstRequester else null,
                    onFocused = { onFocused(app) },
                    onClick = { onOpen(app) },
                    onLongClick = { onMenu(app) }
                )
            }
        }
    }
}

@Composable
private fun AppCard(
    app: AppEntry,
    width: Dp,
    isFavorite: Boolean,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val labelAlpha = animateFloatAsState(
        targetValue = if (focused) 1f else 0.62f,
        animationSpec = tween(160),
        label = "label"
    )
    val shapeAt = remember { roundedMorph(24.dp, 38.dp) }
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val container = remember(app.tint, base) { lerp(base, app.tint, 0.5f) }
    val banner = app.banner
    val letter = remember(app.label) { app.label.firstOrNull()?.uppercase() ?: "" }
    val letterSize = (width.value * 0.95f).sp

    Column(modifier = Modifier.width(width)) {
        FocusSurface(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            shapeAt = shapeAt,
            containerColor = container,
            ringColor = MaterialTheme.colorScheme.primary,
            focusRequester = focusRequester,
            onFocusChange = {
                focused = it
                if (it) onFocused()
            },
            onLongClick = onLongClick,
            onClick = onClick
        ) {
            if (banner != null) {
                Image(
                    bitmap = banner,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = letterSize, lineHeight = letterSize),
                    color = Color.White.copy(alpha = 0.12f),
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 12.dp, y = 28.dp)
                )
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 18.dp)
                        .fillMaxHeight(0.54f)
                        .aspectRatio(1f)
                )
            }
            if (isFavorite) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = "Favorite",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .graphicsLayer { alpha = labelAlpha.value }
        )
    }
}

package dev.expressive.launcher

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color as AndroidColor

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _favorites = MutableStateFlow(readList("favorites"))
    val favorites: StateFlow<List<String>> = _favorites.asStateFlow()

    private val _recents = MutableStateFlow(readList("recents"))
    val recents: StateFlow<List<String>> = _recents.asStateFlow()

    private val _homeSignal = MutableStateFlow(0)
    val homeSignal: StateFlow<Int> = _homeSignal.asStateFlow()

    private val _wallpaper = MutableStateFlow(prefs.getString("wallpaper", "aurora").orEmpty())
    val wallpaper: StateFlow<String> = _wallpaper.asStateFlow()

    private val _dim = MutableStateFlow(prefs.getInt("dim", 1))
    val dim: StateFlow<Int> = _dim.asStateFlow()

    private val _accent = MutableStateFlow(prefs.getInt("accent", 0))
    val accent: StateFlow<Int> = _accent.asStateFlow()

    private val _photo = MutableStateFlow<ImageBitmap?>(null)
    val photo: StateFlow<ImageBitmap?> = _photo.asStateFlow()

    private var loadJob: Job? = null

    init {
        if (_wallpaper.value == "photo") loadPhoto()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _apps.value = withContext(Dispatchers.Default) { queryApps() }
        }
    }

    fun signalHome() {
        _homeSignal.update { it + 1 }
    }

    fun toggleFavorite(packageName: String) {
        val current = _favorites.value
        val next = if (packageName in current) current - packageName else current + packageName
        _favorites.value = next
        writeList("favorites", next)
    }

    fun markLaunched(packageName: String) {
        val next = (listOf(packageName) + _recents.value.filter { it != packageName }).take(12)
        _recents.value = next
        writeList("recents", next)
    }

    fun setWallpaper(id: String) {
        _wallpaper.value = id
        prefs.edit().putString("wallpaper", id).apply()
        if (id == "photo" && _photo.value == null) loadPhoto()
    }

    fun setDim(level: Int) {
        _dim.value = level
        prefs.edit().putInt("dim", level).apply()
    }

    fun setAccent(index: Int) {
        _accent.value = index
        prefs.edit().putInt("accent", index).apply()
    }

    fun importPhoto(uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) { runCatching { storePhoto(uri) }.getOrDefault(false) }
            if (stored) {
                _photo.value = withContext(Dispatchers.IO) { decodePhoto() }
                setWallpaper("photo")
            }
            onResult(stored)
        }
    }

    private fun photoFile(): File = File(getApplication<Application>().filesDir, "wallpaper.jpg")

    private fun loadPhoto() {
        viewModelScope.launch {
            _photo.value = withContext(Dispatchers.IO) { decodePhoto() }
        }
    }

    private fun decodePhoto(): ImageBitmap? {
        val file = photoFile()
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.path, options)?.asImageBitmap()
    }

    private fun storePhoto(uri: Uri): Boolean {
        val resolver = getApplication<Application>().contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > 2400) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return false
        val degrees = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrNull() ?: 0f
        val bitmap = if (degrees != 0f) {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(degrees) }, true)
        } else {
            decoded
        }
        photoFile().outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return true
    }

    private fun readList(key: String): List<String> =
        prefs.getString(key, "").orEmpty().split(",").filter { it.isNotBlank() }

    private fun writeList(key: String, value: List<String>) {
        prefs.edit().putString(key, value.joinToString(",")).apply()
    }

    @Suppress("DEPRECATION")
    private fun resolve(pm: PackageManager, category: String): List<ResolveInfo> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)

    private fun tintOf(bitmap: Bitmap): Color {
        val pixel = Bitmap.createScaledBitmap(bitmap, 1, 1, true).getPixel(0, 0)
        if (AndroidColor.alpha(pixel) < 40) return Color(0xFF6B4EFF)
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(pixel, hsv)
        hsv[1] = (hsv[1] * 1.35f).coerceAtMost(1f)
        hsv[2] = hsv[2].coerceIn(0.5f, 0.92f)
        return Color(AndroidColor.HSVToColor(hsv))
    }

    private fun queryApps(): List<AppEntry> {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val own = context.packageName

        val tvInfos = resolve(pm, Intent.CATEGORY_LEANBACK_LAUNCHER)
        val tvPackages = tvInfos.map { it.activityInfo.packageName }.toSet()
        val phoneInfos = resolve(pm, Intent.CATEGORY_LAUNCHER)
            .filter { it.activityInfo.packageName !in tvPackages }

        return (tvInfos.map { it to true } + phoneInfos.map { it to false })
            .filter { it.first.activityInfo.packageName != own }
            .distinctBy { it.first.activityInfo.packageName }
            .mapNotNull { (info, isTv) ->
                runCatching {
                    val activity = info.activityInfo
                    val iconBitmap = info.loadIcon(pm).toBitmap(128, 128)
                    AppEntry(
                        packageName = activity.packageName,
                        activityName = activity.name,
                        label = info.loadLabel(pm).toString(),
                        icon = iconBitmap.asImageBitmap(),
                        banner = if (isTv) {
                            runCatching { activity.loadBanner(pm)?.toBitmap(320, 180)?.asImageBitmap() }.getOrNull()
                        } else {
                            null
                        },
                        tint = tintOf(iconBitmap),
                        isTv = isTv,
                        isSystem = (activity.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                }.getOrNull()
            }
            .sortedBy { it.label.lowercase() }
    }
}

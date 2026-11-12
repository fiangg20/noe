package com.noe.app.ui

import android.app.Application
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noe.app.data.AppSettings
import com.noe.app.data.Note
import com.noe.app.data.NoteStore
import com.noe.app.data.Reminders
import com.noe.app.data.SettingsStore
import com.noe.app.data.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

sealed interface Section {
    data object Notes : Section
    data object Reminders : Section
    data object Archive : Section
    data object Trash : Section
    data class Label(val name: String) : Section
}

/** Quick filter chips shown above the notes grid. */
enum class NoteFilter(val label: String) {
    All("All"),
    Notes("Notes"),
    Checklists("Checklists"),
    WithReminder("Reminders"),
    Colored("Colored"),
}

class UndoAction(val message: String, val revert: () -> Unit) {
    val key: Long = System.nanoTime()
}

private const val TRASH_DAYS = 30L

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val store = NoteStore(app)
    private val settingsStore = SettingsStore(app)
    private val saveChannel = Channel<List<Note>>(Channel.CONFLATED)

    var notes by mutableStateOf<List<Note>>(emptyList())
        private set

    var settings by mutableStateOf(settingsStore.load())
        private set

    var section by mutableStateOf<Section>(Section.Notes)
        private set

    var query by mutableStateOf("")

    var noteFilter by mutableStateOf(NoteFilter.All)
        private set

    var editor by mutableStateOf<EditorState?>(null)
        private set

    var showSettings by mutableStateOf(false)

    var undo by mutableStateOf<UndoAction?>(null)
        private set

    val labels: List<String> by derivedStateOf {
        notes.asSequence()
            .filter { !it.trashed }
            .flatMap { it.labels.asSequence() }
            .distinct()
            .sortedBy { it.lowercase() }
            .toList()
    }

    val visibleNotes: List<Note> by derivedStateOf { computeVisible() }

    init {
        Reminders.ensureChannel(app)

        viewModelScope.launch(Dispatchers.IO) {
            for (list in saveChannel) store.save(list)
        }

        val cutoff = System.currentTimeMillis() - TRASH_DAYS * 24L * 60L * 60L * 1000L
        val loaded = store.load()
        val (expired, kept) = loaded.partition { it.trashed && (it.trashedAt ?: Long.MAX_VALUE) < cutoff }
        notes = kept
        expired.forEach { Reminders.cancel(app, it.id) }
        if (expired.isNotEmpty()) saveChannel.trySend(kept)
    }

    // ---------- Listing ----------

    private fun computeVisible(): List<Note> {
        val s = section
        val base = when (s) {
            Section.Notes -> notes.filter { !it.trashed && !it.archived }
            Section.Reminders -> notes.filter { !it.trashed && it.reminderAt != null }
            Section.Archive -> notes.filter { !it.trashed && it.archived }
            Section.Trash -> notes.filter { it.trashed }
            is Section.Label -> notes.filter { !it.trashed && s.name in it.labels }
        }

        val chip = noteFilter
        val chipped = if (chip == NoteFilter.All || (s != Section.Notes && s !is Section.Label)) base else base.filter { n ->
            when (chip) {
                NoteFilter.All -> true
                NoteFilter.Notes -> !n.isChecklist
                NoteFilter.Checklists -> n.isChecklist
                NoteFilter.WithReminder -> n.reminderAt != null
                NoteFilter.Colored -> n.colorIndex != 0
            }
        }

        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) chipped else chipped.filter { n ->
            n.title.lowercase().contains(q) ||
                n.contentText().lowercase().contains(q) ||
                n.labels.any { it.lowercase().contains(q) }
        }

        return when (s) {
            Section.Reminders -> filtered.sortedBy { it.reminderAt ?: Long.MAX_VALUE }
            Section.Trash -> filtered.sortedByDescending { it.trashedAt ?: 0L }
            else -> {
                val order: Comparator<Note> = when (settings.sortOrder) {
                    SortOrder.Edited -> compareByDescending<Note> { it.updatedAt }
                    SortOrder.Created -> compareByDescending<Note> { it.createdAt }
                    SortOrder.Title -> compareBy<Note> { it.title.lowercase() }
                }
                filtered.sortedWith(compareByDescending<Note> { it.pinned }.then(order))
            }
        }
    }

    fun openSection(newSection: Section) {
        section = newSection
        query = ""
        noteFilter = NoteFilter.All
    }

    fun setFilter(filter: NoteFilter) {
        noteFilter = filter
    }

    /** Ticks a checklist item straight from the notes grid. */
    fun toggleItem(noteId: String, itemId: String) {
        update(listOf(noteId)) { n ->
            n.copy(
                items = n.items.map { if (it.id == itemId) it.copy(checked = !it.checked) else it },
                updatedAt = System.currentTimeMillis(),
            )
        }
    }

    // ---------- Persistence helpers ----------

    private fun commit(newList: List<Note>, changed: Collection<Note> = emptyList()) {
        notes = newList
        changed.forEach {
            Reminders.sync(app, it)
            if (it.trashed) Reminders.cancelNotification(app, it.id)
        }
        saveChannel.trySend(newList)
    }

    fun update(ids: Collection<String>, transform: (Note) -> Note) {
        val set = ids.toSet()
        if (set.isEmpty()) return
        val changed = ArrayList<Note>()
        val newList = notes.map { n ->
            if (n.id in set) transform(n).also { changed.add(it) } else n
        }
        commit(newList, changed)
    }

    // ---------- Actions on notes ----------

    fun setPinned(ids: Collection<String>, pinned: Boolean) =
        update(ids) { it.copy(pinned = pinned, archived = if (pinned) false else it.archived) }

    fun setArchived(ids: Collection<String>, archived: Boolean) {
        val list = ids.toList()
        update(list) { it.copy(archived = archived, pinned = if (archived) false else it.pinned) }
        showUndo(if (archived) "Archived" else "Moved to notes") { setArchivedQuiet(list, !archived) }
    }

    private fun setArchivedQuiet(ids: Collection<String>, archived: Boolean) =
        update(ids) { it.copy(archived = archived, pinned = if (archived) false else it.pinned) }

    fun trash(ids: Collection<String>) {
        val list = ids.toList()
        if (list.isEmpty()) return
        val now = System.currentTimeMillis()
        update(list) { it.copy(trashed = true, trashedAt = now, pinned = false) }
        showUndo(if (list.size == 1) "Moved to trash" else "${list.size} notes moved to trash") {
            restore(list)
        }
    }

    fun restore(ids: Collection<String>) =
        update(ids) { it.copy(trashed = false, trashedAt = null) }

    fun deleteForever(ids: Collection<String>) {
        val set = ids.toSet()
        if (set.isEmpty()) return
        commit(notes.filterNot { it.id in set })
        set.forEach {
            Reminders.cancel(app, it)
            Reminders.cancelNotification(app, it)
        }
    }

    fun emptyTrash() = deleteForever(notes.filter { it.trashed }.map { it.id })

    fun setColor(ids: Collection<String>, colorIndex: Int) =
        update(ids) { it.copy(colorIndex = colorIndex) }

    fun addLabel(ids: Collection<String>, label: String) {
        val clean = label.trim()
        if (clean.isEmpty()) return
        update(ids) { if (clean in it.labels) it else it.copy(labels = it.labels + clean) }
    }

    fun duplicate(ids: Collection<String>) {
        val set = ids.toSet()
        val now = System.currentTimeMillis()
        val copies = notes.filter { it.id in set }.map {
            it.copy(
                id = java.util.UUID.randomUUID().toString(),
                title = if (it.title.isBlank()) "" else it.title + " (copy)",
                reminderAt = null,
                repeat = com.noe.app.data.Repeat.None,
                pinned = false,
                createdAt = now,
                updatedAt = now,
            )
        }
        if (copies.isNotEmpty()) commit(notes + copies)
    }

    fun renameLabel(old: String, new: String) {
        val clean = new.trim()
        if (clean.isEmpty() || clean == old) return
        commit(notes.map { n ->
            if (old in n.labels) n.copy(labels = n.labels.map { if (it == old) clean else it }.distinct()) else n
        })
        val s = section
        if (s is Section.Label && s.name == old) section = Section.Label(clean)
    }

    fun deleteLabel(name: String) {
        commit(notes.map { n -> if (name in n.labels) n.copy(labels = n.labels - name) else n })
        val s = section
        if (s is Section.Label && s.name == name) section = Section.Notes
    }

    // ---------- Editor ----------

    fun openNew(checklist: Boolean = false, text: String? = null) {
        val s = section
        val note = Note(
            isChecklist = checklist,
            body = if (checklist) "" else text.orEmpty(),
            items = if (checklist) listOf(com.noe.app.data.ChecklistItem()) else emptyList(),
            labels = if (s is Section.Label) listOf(s.name) else emptyList(),
        )
        editor = EditorState(note)
    }

    fun openNote(id: String) {
        val note = notes.firstOrNull { it.id == id } ?: return
        editor = EditorState(note)
    }

    /** Saves the editor's content (also used when the app goes to the background). */
    fun commitEditor() {
        val e = editor ?: return
        val edited = e.toNote()
        val existing = notes.firstOrNull { it.id == edited.id }

        if (edited.isEmpty) {
            if (existing != null) deleteForever(listOf(edited.id))
            return
        }
        if (existing != null && existing.copy(updatedAt = 0L) == edited.copy(updatedAt = 0L)) return

        val saved = edited.copy(updatedAt = System.currentTimeMillis())
        commit(
            if (existing == null) notes + saved else notes.map { if (it.id == saved.id) saved else it },
            listOf(saved),
        )
    }

    fun closeEditor() {
        commitEditor()
        editor = null
    }

    fun archiveFromEditor() {
        val e = editor ?: return
        e.archived = !e.archived
        val nowArchived = e.archived
        val id = e.id
        closeEditor()
        if (notes.any { it.id == id }) {
            showUndo(if (nowArchived) "Archived" else "Moved to notes") { setArchivedQuiet(listOf(id), !nowArchived) }
        }
    }

    fun trashFromEditor() {
        val e = editor ?: return
        commitEditor()
        editor = null
        if (notes.any { it.id == e.id }) trash(listOf(e.id))
    }

    fun duplicateFromEditor() {
        val e = editor ?: return
        commitEditor()
        duplicate(listOf(e.id))
    }

    /** Called with whatever launched the app: a notification tap or shared text. */
    fun handleLaunch(noteId: String?, sharedText: String?, action: String? = null) {
        when {
            noteId != null -> openNote(noteId)
            !sharedText.isNullOrBlank() -> openNew(text = sharedText)
            action == "new_note" -> openNew()
            action == "new_checklist" -> openNew(checklist = true)
        }
    }

    // ---------- Undo ----------

    private fun showUndo(message: String, revert: () -> Unit) {
        undo = UndoAction(message, revert)
    }

    fun performUndo() {
        undo?.revert?.invoke()
        undo = null
    }

    fun consumeUndo() {
        undo = null
    }

    // ---------- Settings and backup ----------

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settings = transform(settings)
        settingsStore.save(settings)
    }

    fun exportJson(): String = NoteStore.exportBackup(notes)

    /** Merges notes from a backup. Returns how many were added or updated. */
    fun importJson(text: String): Int {
        val incoming = NoteStore.parseNotes(text)
        val byId = notes.associateBy { it.id }.toMutableMap()
        val changed = ArrayList<Note>()
        incoming.forEach { n ->
            val current = byId[n.id]
            if (current == null || n.updatedAt > current.updatedAt) {
                byId[n.id] = n
                changed.add(n)
            }
        }
        if (changed.isNotEmpty()) commit(byId.values.toList(), changed)
        return changed.size
    }
}

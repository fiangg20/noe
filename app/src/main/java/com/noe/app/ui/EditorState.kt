package com.noe.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.noe.app.data.ChecklistItem
import com.noe.app.data.Note

/**
 * Live, editable copy of a note. It uses Compose snapshot state so text fields stay
 * responsive, and it lives in the ViewModel so it survives screen rotation.
 */
class EditorState(private val base: Note) {
    val id: String = base.id

    var title by mutableStateOf(base.title)
    var body by mutableStateOf(base.body)
    var isChecklist by mutableStateOf(base.isChecklist)
    var colorIndex by mutableStateOf(base.colorIndex)
    var pinned by mutableStateOf(base.pinned)
    var archived by mutableStateOf(base.archived)
    var reminderAt: Long? by mutableStateOf(base.reminderAt)

    val items = mutableStateListOf<ChecklistItem>().also { it.addAll(base.items) }
    val labels = mutableStateListOf<String>().also { it.addAll(base.labels) }

    /** Switches between plain text and checklist, converting the content. */
    fun toggleChecklist() {
        if (isChecklist) {
            body = items.filter { it.text.isNotBlank() }.joinToString("\n") { it.text }
            items.clear()
            isChecklist = false
        } else {
            items.clear()
            body.lines().filter { it.isNotBlank() }.forEach { items.add(ChecklistItem(text = it.trim())) }
            if (items.isEmpty()) items.add(ChecklistItem())
            body = ""
            isChecklist = true
        }
    }

    fun toNote(): Note = base.copy(
        title = title.trim(),
        body = body,
        isChecklist = isChecklist,
        items = items.toList(),
        colorIndex = colorIndex,
        labels = labels.toList(),
        pinned = pinned,
        archived = archived,
        reminderAt = reminderAt,
    )
}

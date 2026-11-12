package com.noe.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.noe.app.data.ChecklistItem
import com.noe.app.data.FormatState
import com.noe.app.data.Note
import com.noe.app.data.Repeat
import com.noe.app.data.SpanKind
import com.noe.app.data.SpanOps
import com.noe.app.data.TextSpan

/**
 * Live, editable copy of a note. It uses Compose snapshot state so text fields stay
 * responsive, and it lives in the ViewModel so it survives screen rotation.
 */
class EditorState(private val base: Note) {
    val id: String = base.id

    var title by mutableStateOf(base.title)
    var colorIndex by mutableStateOf(base.colorIndex)
    var pinned by mutableStateOf(base.pinned)
    var archived by mutableStateOf(base.archived)
    var isChecklist by mutableStateOf(base.isChecklist)
    var reminderAt: Long? by mutableStateOf(base.reminderAt)
    var repeat by mutableStateOf(base.repeat)

    // The body is kept as a TextFieldValue so the keyboard's composing text and the
    // selection survive, and formatting is stored as separate spans.
    var bodyValue by mutableStateOf(TextFieldValue(base.body, TextRange(base.body.length)))
        private set
    var spans by mutableStateOf(SpanOps.clamp(base.spans, base.body.length))
        private set

    /** Formatting that new text gets when nothing is selected. */
    private var typing by mutableStateOf(
        SpanOps.styleAt(base.spans, base.body.length, base.body.length)
    )

    var body: String
        get() = bodyValue.text
        set(value) {
            bodyValue = TextFieldValue(value, TextRange(value.length))
            spans = emptyList()
            typing = FormatState()
        }

    val items = mutableStateListOf<ChecklistItem>().also { it.addAll(base.items) }
    val labels = mutableStateListOf<String>().also { it.addAll(base.labels) }

    // ---------- Text formatting ----------

    fun onBodyChange(new: TextFieldValue) {
        val old = bodyValue
        if (new.text != old.text) {
            val adjusted = SpanOps.adjust(spans, old.text, new.text)
            var list = adjusted.spans
            if (adjusted.inserted > 0) {
                list = SpanOps.applyFormat(
                    list,
                    adjusted.start,
                    adjusted.start + adjusted.inserted,
                    typing,
                )
            }
            spans = SpanOps.clamp(list, new.text.length)
            bodyValue = new
            if (adjusted.inserted == 0) {
                typing = SpanOps.styleAt(spans, new.selection.start, new.text.length)
            }
        } else {
            bodyValue = new
            if (new.selection != old.selection && new.selection.collapsed) {
                typing = SpanOps.styleAt(spans, new.selection.start, new.text.length)
            }
        }
    }

    /** What the formatting toolbar should show as active right now. */
    fun currentFormat(): FormatState {
        val sel = bodyValue.selection
        return if (sel.collapsed) typing else SpanOps.styleOf(spans, sel.min, sel.max)
    }

    fun toggleFormat(kind: SpanKind) {
        val sel = bodyValue.selection
        if (sel.collapsed) {
            typing = typing.toggled(kind)
        } else {
            spans = if (SpanOps.covers(spans, kind, sel.min, sel.max)) {
                SpanOps.remove(spans, kind, sel.min, sel.max)
            } else {
                SpanOps.add(spans, kind, sel.min, sel.max)
            }
        }
    }

    fun setTextSize(scale: Float) {
        val sel = bodyValue.selection
        if (sel.collapsed) typing = typing.copy(size = scale)
        else spans = SpanOps.setSize(spans, sel.min, sel.max, scale)
    }

    fun clearFormatting() {
        val sel = bodyValue.selection
        if (sel.collapsed) typing = FormatState()
        else spans = SpanOps.clearAll(spans, sel.min, sel.max)
    }

    // ---------- Checklist conversion ----------

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

    /** Words in the title and content, shown in the editor's bottom bar. */
    fun wordCount(): Int {
        val text = if (isChecklist) {
            items.joinToString(" ") { it.text }
        } else {
            body
        }
        return (title + " " + text).split(Regex("\\s+")).count { it.isNotBlank() }
    }

    /** Number of checked and total non-empty checklist items. */
    fun progress(): Pair<Int, Int> {
        val real = items.filter { it.text.isNotBlank() }
        return real.count { it.checked } to real.size
    }

    fun toNote(): Note = base.copy(
        title = title.trim(),
        body = body,
        spans = if (isChecklist) emptyList<TextSpan>() else SpanOps.clamp(spans, body.length),
        isChecklist = isChecklist,
        items = items.toList(),
        colorIndex = colorIndex,
        labels = labels.toList(),
        pinned = pinned,
        archived = archived,
        reminderAt = reminderAt,
        repeat = if (reminderAt == null) Repeat.None else repeat,
    )
}

package com.noe.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val checked: Boolean = false,
)

enum class Repeat(val label: String) {
    None("Never"),
    Daily("Daily"),
    Weekly("Weekly"),
}

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val spans: List<TextSpan> = emptyList(),
    val isChecklist: Boolean = false,
    val items: List<ChecklistItem> = emptyList(),
    val colorIndex: Int = 0,
    val labels: List<String> = emptyList(),
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashed: Boolean = false,
    val reminderAt: Long? = null,
    val repeat: Repeat = Repeat.None,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val trashedAt: Long? = null,
) {
    val isEmpty: Boolean
        get() = title.isBlank() && body.isBlank() && items.none { it.text.isNotBlank() }

    /** Checked and total count of the non-empty checklist items. */
    val checklistProgress: Pair<Int, Int>
        get() {
            val real = items.filter { it.text.isNotBlank() }
            return real.count { it.checked } to real.size
        }

    /** Plain text used for search, previews and sharing. */
    fun contentText(): String =
        if (isChecklist) {
            items.filter { it.text.isNotBlank() }
                .joinToString("\n") { (if (it.checked) "[x] " else "[ ] ") + it.text }
        } else {
            body
        }

    fun shareText(): String =
        listOf(title, contentText()).filter { it.isNotBlank() }.joinToString("\n\n")

    fun wordCount(): Int =
        contentText().split(Regex("\\s+")).count { it.isNotBlank() }
}

fun Note.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("body", body)
    put("spans", spansToJson(spans))
    put("isChecklist", isChecklist)
    put("items", JSONArray().also { arr ->
        items.forEach { item ->
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("text", item.text)
                    .put("checked", item.checked)
            )
        }
    })
    put("colorIndex", colorIndex)
    put("labels", JSONArray(labels))
    put("pinned", pinned)
    put("archived", archived)
    put("trashed", trashed)
    put("reminderAt", reminderAt ?: JSONObject.NULL)
    put("repeat", repeat.name)
    put("createdAt", createdAt)
    put("updatedAt", updatedAt)
    put("trashedAt", trashedAt ?: JSONObject.NULL)
}

fun noteFromJson(o: JSONObject): Note {
    val items = o.optJSONArray("items")?.let { arr ->
        (0 until arr.length()).map { i ->
            val it = arr.getJSONObject(i)
            ChecklistItem(
                id = it.optString("id").ifBlank { UUID.randomUUID().toString() },
                text = it.optString("text"),
                checked = it.optBoolean("checked"),
            )
        }
    } ?: emptyList()

    val labels = o.optJSONArray("labels")?.let { arr ->
        (0 until arr.length()).map { i -> arr.getString(i) }
    } ?: emptyList()

    val now = System.currentTimeMillis()
    return Note(
        id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
        title = o.optString("title"),
        body = o.optString("body"),
        spans = SpanOps.clamp(spansFromJson(o.optJSONArray("spans")), o.optString("body").length),
        isChecklist = o.optBoolean("isChecklist"),
        items = items,
        colorIndex = o.optInt("colorIndex"),
        labels = labels,
        pinned = o.optBoolean("pinned"),
        archived = o.optBoolean("archived"),
        trashed = o.optBoolean("trashed"),
        reminderAt = if (o.isNull("reminderAt")) null else o.optLong("reminderAt"),
        repeat = Repeat.entries.firstOrNull { it.name == o.optString("repeat") } ?: Repeat.None,
        createdAt = o.optLong("createdAt", now),
        updatedAt = o.optLong("updatedAt", now),
        trashedAt = if (o.isNull("trashedAt")) null else o.optLong("trashedAt"),
    )
}

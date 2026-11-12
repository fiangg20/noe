package com.noe.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Stores all notes in one JSON file inside the app's private storage. */
class NoteStore(private val context: Context) {

    private val file: File get() = File(context.filesDir, "notes.json")

    fun load(): List<Note> {
        if (!file.exists()) return emptyList()
        return try {
            parseNotes(file.readText())
        } catch (e: Exception) {
            // Keep the unreadable file so nothing is lost, then start fresh.
            runCatching { file.copyTo(File(context.filesDir, "notes.corrupt.json"), overwrite = true) }
            emptyList()
        }
    }

    fun save(notes: List<Note>) {
        val text = notesToJsonArray(notes).toString()
        val tmp = File(context.filesDir, "notes.json.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    companion object {
        fun notesToJsonArray(notes: List<Note>): JSONArray {
            val arr = JSONArray()
            notes.forEach { arr.put(it.toJson()) }
            return arr
        }

        fun parseNotes(text: String): List<Note> {
            val trimmed = text.trim()
            val arr = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONObject(trimmed).getJSONArray("notes")
            }
            return (0 until arr.length()).map { noteFromJson(arr.getJSONObject(it)) }
        }

        fun exportBackup(notes: List<Note>): String =
            JSONObject()
                .put("app", "noe")
                .put("version", 1)
                .put("exportedAt", System.currentTimeMillis())
                .put("notes", notesToJsonArray(notes))
                .toString(2)
    }
}

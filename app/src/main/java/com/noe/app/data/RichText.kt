package com.noe.app.data

import org.json.JSONArray
import org.json.JSONObject

enum class SpanKind { Bold, Italic, Underline, Strike, Size }

/** A formatted range [start, end) of a note's text. [value] is only used by Size (a scale factor). */
data class TextSpan(
    val start: Int,
    val end: Int,
    val kind: SpanKind,
    val value: Float = 1f,
)

/** The formatting that applies at the cursor or across a selection. */
data class FormatState(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val size: Float = 1f,
) {
    fun has(kind: SpanKind): Boolean = when (kind) {
        SpanKind.Bold -> bold
        SpanKind.Italic -> italic
        SpanKind.Underline -> underline
        SpanKind.Strike -> strike
        SpanKind.Size -> size != 1f
    }

    fun toggled(kind: SpanKind): FormatState = when (kind) {
        SpanKind.Bold -> copy(bold = !bold)
        SpanKind.Italic -> copy(italic = !italic)
        SpanKind.Underline -> copy(underline = !underline)
        SpanKind.Strike -> copy(strike = !strike)
        SpanKind.Size -> this
    }
}

object TextSizes {
    const val Small = 0.8f
    const val Normal = 1f
    const val Large = 1.4f
    const val Huge = 1.9f
}

/** Pure functions that keep a list of spans in step with edits to the text. */
object SpanOps {

    private val onOffKinds = listOf(SpanKind.Bold, SpanKind.Italic, SpanKind.Underline, SpanKind.Strike)

    /** Sorts, drops empty spans and merges neighbours that have the same style. */
    fun normalize(spans: List<TextSpan>): List<TextSpan> {
        val result = ArrayList<TextSpan>()
        spans.filter { it.end > it.start }
            .groupBy { it.kind to it.value }
            .values
            .forEach { group ->
                var current: TextSpan? = null
                for (sp in group.sortedBy { it.start }) {
                    val c = current
                    if (c != null && sp.start <= c.end) {
                        current = c.copy(end = maxOf(c.end, sp.end))
                    } else {
                        if (c != null) result.add(c)
                        current = sp
                    }
                }
                current?.let { result.add(it) }
            }
        return result.sortedWith(compareBy({ it.start }, { it.kind.ordinal }))
    }

    /** Removes [kind] formatting from [s, e). */
    fun remove(spans: List<TextSpan>, kind: SpanKind, s: Int, e: Int): List<TextSpan> {
        if (e <= s) return spans
        val out = ArrayList<TextSpan>()
        for (sp in spans) {
            if (sp.kind != kind || sp.end <= s || sp.start >= e) {
                out.add(sp)
            } else {
                if (sp.start < s) out.add(sp.copy(end = s))
                if (sp.end > e) out.add(sp.copy(start = e))
            }
        }
        return normalize(out)
    }

    fun add(spans: List<TextSpan>, kind: SpanKind, s: Int, e: Int, value: Float = 1f): List<TextSpan> {
        if (e <= s) return spans
        return normalize(remove(spans, kind, s, e) + TextSpan(s, e, kind, value))
    }

    /** True when every character in [s, e) has [kind]. */
    fun covers(spans: List<TextSpan>, kind: SpanKind, s: Int, e: Int): Boolean {
        if (e <= s) return false
        var cursor = s
        for (sp in spans.filter { it.kind == kind }.sortedBy { it.start }) {
            if (sp.end <= cursor) continue
            if (sp.start > cursor) return false
            cursor = sp.end
            if (cursor >= e) return true
        }
        return cursor >= e
    }

    private fun sizeOver(spans: List<TextSpan>, s: Int, e: Int): Float =
        spans.firstOrNull { it.kind == SpanKind.Size && it.start <= s && it.end >= e }?.value ?: 1f

    /** The formatting shared by every character in [s, e). */
    fun styleOf(spans: List<TextSpan>, s: Int, e: Int): FormatState {
        if (e <= s) return FormatState()
        return FormatState(
            bold = covers(spans, SpanKind.Bold, s, e),
            italic = covers(spans, SpanKind.Italic, s, e),
            underline = covers(spans, SpanKind.Underline, s, e),
            strike = covers(spans, SpanKind.Strike, s, e),
            size = sizeOver(spans, s, e),
        )
    }

    /** The formatting new text would get at the cursor: that of the character before it. */
    fun styleAt(spans: List<TextSpan>, cursor: Int, textLength: Int): FormatState = when {
        textLength == 0 -> FormatState()
        cursor > 0 -> styleOf(spans, cursor - 1, cursor)
        else -> styleOf(spans, 0, 1)
    }

    fun setSize(spans: List<TextSpan>, s: Int, e: Int, scale: Float): List<TextSpan> =
        if (scale == 1f) remove(spans, SpanKind.Size, s, e) else add(spans, SpanKind.Size, s, e, scale)

    fun clearAll(spans: List<TextSpan>, s: Int, e: Int): List<TextSpan> {
        var list = spans
        SpanKind.entries.forEach { list = remove(list, it, s, e) }
        return list
    }

    /** Makes [s, e) match the given formatting exactly. Used for freshly typed text. */
    fun applyFormat(spans: List<TextSpan>, s: Int, e: Int, format: FormatState): List<TextSpan> {
        var list = spans
        for (kind in onOffKinds) {
            list = if (format.has(kind)) add(list, kind, s, e) else remove(list, kind, s, e)
        }
        return setSize(list, s, e, format.size)
    }

    class Adjusted(val spans: List<TextSpan>, val start: Int, val inserted: Int)

    /**
     * Moves spans so they still cover the same words after the text changed from [old] to [new].
     * Also reports where the edit happened and how many characters were inserted there.
     */
    fun adjust(spans: List<TextSpan>, old: String, new: String): Adjusted {
        var prefix = 0
        val maxPrefix = minOf(old.length, new.length)
        while (prefix < maxPrefix && old[prefix] == new[prefix]) prefix++

        var suffix = 0
        val maxSuffix = maxPrefix - prefix
        while (suffix < maxSuffix && old[old.length - 1 - suffix] == new[new.length - 1 - suffix]) suffix++

        val removedEnd = old.length - suffix
        val delta = new.length - old.length
        val inserted = new.length - suffix - prefix

        fun map(x: Int): Int = when {
            x <= prefix -> x
            x >= removedEnd -> x + delta
            else -> prefix
        }

        val moved = spans.map { it.copy(start = map(it.start), end = map(it.end)) }
        return Adjusted(normalize(moved), prefix, inserted)
    }

    /** Drops or trims spans that run past the end of the text. */
    fun clamp(spans: List<TextSpan>, length: Int): List<TextSpan> =
        normalize(spans.filter { it.start < length }.map { it.copy(end = minOf(it.end, length)) })
}

fun spansToJson(spans: List<TextSpan>): JSONArray = JSONArray().also { arr ->
    spans.forEach {
        arr.put(
            JSONObject()
                .put("s", it.start)
                .put("e", it.end)
                .put("k", it.kind.name)
                .put("v", it.value.toDouble())
        )
    }
}

fun spansFromJson(arr: JSONArray?): List<TextSpan> {
    if (arr == null) return emptyList()
    val list = ArrayList<TextSpan>()
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val kind = SpanKind.entries.firstOrNull { it.name == o.optString("k") } ?: continue
        list.add(
            TextSpan(
                start = o.optInt("s"),
                end = o.optInt("e"),
                kind = kind,
                value = o.optDouble("v", 1.0).toFloat(),
            )
        )
    }
    return SpanOps.normalize(list)
}

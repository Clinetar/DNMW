package com.clinetar.dnmw

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

data class RichSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strikethrough: Boolean = false,
    val code: Boolean = false,
    val heading: Boolean = false
)

enum class RichFormat { BOLD, ITALIC, STRIKETHROUGH, CODE, HEADING, BULLET }

class RichTextState(initialContent: String) {
    private val spans = mutableStateListOf<RichSpan>()
    private var text by mutableStateOf("")
    var selection by mutableStateOf(TextRange.Zero)
        private set

    init {
        val (t, s) = deserializeRichContent(initialContent)
        text = t
        spans.addAll(s)
    }

    val textFieldValue: TextFieldValue
        get() = TextFieldValue(
            annotatedString = buildRichAnnotatedString(text, spans),
            selection = selection
        )

    fun onValueChange(new: TextFieldValue) {
        val newText = new.annotatedString.text
        if (newText != text) {
            migrateSpans(text, newText)
            text = newText
        }
        selection = new.selection
    }

    fun applyFormat(format: RichFormat) {
        val sel = selection
        when (format) {
            RichFormat.BULLET -> {
                val lineStart = text.lastIndexOf('\n', maxOf(sel.start - 1, 0)) + 1
                text = text.substring(0, lineStart) + "• " + text.substring(lineStart)
                shiftSpansAfter(lineStart, 2)
                selection = TextRange(sel.start + 2, sel.end + 2)
            }
            else -> {
                if (sel.collapsed) return
                val start = sel.min; val end = sel.max
                val prop: (RichSpan) -> Boolean = when (format) {
                    RichFormat.BOLD -> { s -> s.bold }
                    RichFormat.ITALIC -> { s -> s.italic }
                    RichFormat.STRIKETHROUGH -> { s -> s.strikethrough }
                    RichFormat.CODE -> { s -> s.code }
                    RichFormat.HEADING -> { s -> s.heading }
                    RichFormat.BULLET -> return
                }
                if (isRangeFormatted(start, end, prop)) {
                    removeFormatInRange(start, end, format)
                } else {
                    spans.add(when (format) {
                        RichFormat.BOLD -> RichSpan(start, end, bold = true)
                        RichFormat.ITALIC -> RichSpan(start, end, italic = true)
                        RichFormat.STRIKETHROUGH -> RichSpan(start, end, strikethrough = true)
                        RichFormat.CODE -> RichSpan(start, end, code = true)
                        RichFormat.HEADING -> RichSpan(start, end, heading = true)
                        RichFormat.BULLET -> return
                    })
                }
            }
        }
    }

    fun serialize(): String = serializeRichContent(text, spans.toList())

    private fun isRangeFormatted(start: Int, end: Int, prop: (RichSpan) -> Boolean): Boolean {
        val relevant = spans.filter { prop(it) && it.start < end && it.end > start }
        if (relevant.isEmpty()) return false
        var covered = start
        for (span in relevant.sortedBy { it.start }) {
            if (span.start > covered) return false
            covered = maxOf(covered, span.end)
            if (covered >= end) return true
        }
        return false
    }

    private fun removeFormatInRange(start: Int, end: Int, format: RichFormat) {
        val prop: (RichSpan) -> Boolean = when (format) {
            RichFormat.BOLD -> { s -> s.bold }
            RichFormat.ITALIC -> { s -> s.italic }
            RichFormat.STRIKETHROUGH -> { s -> s.strikethrough }
            RichFormat.CODE -> { s -> s.code }
            RichFormat.HEADING -> { s -> s.heading }
            RichFormat.BULLET -> return
        }
        val overlapping = spans.filter { prop(it) && it.start < end && it.end > start }
        val toAdd = mutableListOf<RichSpan>()
        for (span in overlapping) {
            if (span.start < start) toAdd.add(span.copy(end = start))
            if (span.end > end) toAdd.add(span.copy(start = end))
        }
        spans.removeAll(overlapping.toSet())
        spans.addAll(toAdd)
    }

    private fun shiftSpansAfter(position: Int, count: Int) {
        val adjusted = spans.map { span ->
            when {
                span.start >= position -> span.copy(start = span.start + count, end = span.end + count)
                span.end > position -> span.copy(end = span.end + count)
                else -> span
            }
        }
        spans.clear()
        spans.addAll(adjusted)
    }

    private fun migrateSpans(oldText: String, newText: String) {
        var start = 0
        while (start < oldText.length && start < newText.length && oldText[start] == newText[start]) start++
        var oldEnd = oldText.length
        var newEnd = newText.length
        while (oldEnd > start && newEnd > start && oldText[oldEnd - 1] == newText[newEnd - 1]) { oldEnd--; newEnd-- }
        val deletedCount = oldEnd - start
        val insertedCount = newEnd - start
        val adjusted = spans.mapNotNull { adjustSpan(it, start, deletedCount, insertedCount) }
        spans.clear()
        spans.addAll(adjusted)
    }

    private fun adjustSpan(span: RichSpan, changeStart: Int, deletedCount: Int, insertedCount: Int): RichSpan? {
        val changeEnd = changeStart + deletedCount
        val delta = insertedCount - deletedCount
        return when {
            span.end <= changeStart -> span
            span.start >= changeEnd -> span.copy(start = span.start + delta, end = span.end + delta)
            span.start >= changeStart && span.end <= changeEnd -> null
            span.start < changeStart && span.end <= changeEnd -> span.copy(end = changeStart).takeIf { it.start < it.end }
            span.start >= changeStart && span.end > changeEnd -> span.copy(start = changeStart + insertedCount, end = span.end + delta).takeIf { it.start < it.end }
            else -> span.copy(end = span.end + delta)
        }
    }
}

fun buildRichAnnotatedString(text: String, spans: List<RichSpan>) = buildAnnotatedString {
    append(text)
    spans.filter { it.start >= 0 && it.end <= text.length && it.start < it.end }.forEach { span ->
        addStyle(
            SpanStyle(
                fontWeight = if (span.bold || span.heading) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
                textDecoration = if (span.strikethrough) TextDecoration.LineThrough else null,
                fontFamily = if (span.code) FontFamily.Monospace else null,
                background = if (span.code) Color(0x22808080) else Color.Unspecified,
                fontSize = if (span.heading) 20.sp else androidx.compose.ui.unit.TextUnit.Unspecified
            ),
            span.start, span.end
        )
    }
}

fun serializeRichContent(text: String, spans: List<RichSpan>): String {
    if (spans.isEmpty()) return text
    val arr = JSONArray()
    spans.forEach { span ->
        val s = JSONObject()
        s.put("a", span.start); s.put("z", span.end)
        if (span.bold) s.put("b", true)
        if (span.italic) s.put("i", true)
        if (span.strikethrough) s.put("s", true)
        if (span.code) s.put("c", true)
        if (span.heading) s.put("h", true)
        arr.put(s)
    }
    return JSONObject().apply { put("v", 1); put("t", text); put("s", arr) }.toString()
}

fun deserializeRichContent(content: String): Pair<String, List<RichSpan>> {
    if (!content.startsWith("{")) return Pair(content, emptyList())
    return try {
        val json = JSONObject(content)
        val text = json.getString("t")
        val arr = json.getJSONArray("s")
        val spans = (0 until arr.length()).map { i ->
            val s = arr.getJSONObject(i)
            RichSpan(
                start = s.getInt("a"), end = s.getInt("z"),
                bold = s.optBoolean("b"), italic = s.optBoolean("i"),
                strikethrough = s.optBoolean("s"), code = s.optBoolean("c"),
                heading = s.optBoolean("h")
            )
        }
        Pair(text, spans)
    } catch (e: Exception) {
        Pair(content, emptyList())
    }
}

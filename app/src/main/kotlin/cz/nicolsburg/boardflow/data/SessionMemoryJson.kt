package cz.nicolsburg.boardflow.data

import cz.nicolsburg.boardflow.model.SessionMemory
import org.json.JSONArray
import org.json.JSONObject

internal fun String.toSessionMemoryOrNull(): SessionMemory? {
    if (isBlank()) return null
    return runCatching { JSONObject(this).toSessionMemoryOrNull() }.getOrNull()
}

internal fun JSONObject.toSessionMemoryOrNull(): SessionMemory? {
    val moods = when {
        has("moods") -> getJSONArray("moods").let { arr ->
            (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
        }
        has("mood") -> optString("mood", "").let { if (it.isNotBlank()) listOf(it) else emptyList() }
        else -> emptyList()
    }
    return SessionMemory(
        moods = moods,
        momentType = optString("momentType", ""),
        note = optString("note", ""),
        quote = optString("quote", ""),
        chronicleLine = optString("chronicleLine", ""),
        chronicleSourceKey = optString("chronicleSourceKey", ""),
        chronicleCreatedAt = if (has("chronicleCreatedAt") && !isNull("chronicleCreatedAt")) {
            optLong("chronicleCreatedAt")
        } else {
            null
        }
    ).takeIf {
        it.moods.isNotEmpty() ||
            it.note.isNotBlank() ||
            it.quote.isNotBlank() ||
            it.chronicleLine.isNotBlank()
    }
}

internal fun SessionMemory.toJsonObject(): JSONObject = JSONObject().apply {
    put("moods", JSONArray().also { arr -> moods.forEach { arr.put(it) } })
    put("momentType", momentType)
    put("note", note)
    put("quote", quote)
    put("chronicleLine", chronicleLine)
    put("chronicleSourceKey", chronicleSourceKey)
    put("chronicleCreatedAt", chronicleCreatedAt)
}

internal fun SessionMemory.toJsonString(): String = toJsonObject().toString()

/** Parses a [SessionMemory] from BGG notes that contain $$mood:/$$quote: lines. Returns null if none found. */
internal fun String.parseMemoryFromNotes(): SessionMemory? {
    val lines = lines()
    val moodLine = lines.firstOrNull { it.startsWith("\$\$mood:") }
    val quoteLine = lines.firstOrNull { it.startsWith("\$\$quote:") }
    if (moodLine == null && quoteLine == null) return null
    val moods = moodLine?.removePrefix("\$\$mood:")?.trim()
        ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        ?: emptyList()
    val quote = quoteLine?.removePrefix("\$\$quote:")?.trim() ?: ""
    return SessionMemory(moods = moods, quote = quote)
        .takeIf { it.moods.isNotEmpty() || it.quote.isNotBlank() }
}

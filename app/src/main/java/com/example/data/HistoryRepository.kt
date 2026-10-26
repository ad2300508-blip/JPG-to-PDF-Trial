package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class HistoryKind(val label: String) {
    PDF("PDF"),
    MARKDOWN("Markdown"),
    ZIP("ZIP"),
    IMAGE("Immagini"),
    AUDIO("Audio")
}

data class HistoryEntry(
    val id: String,
    val name: String,
    val toolLabel: String,
    val kind: HistoryKind,
    val uri: String,
    val sizeBytes: Long,
    val timestamp: Long
)

/**
 * Persists the list of files produced by the app so the "Cronologia" tab and the
 * "Recenti" panel can reopen, share or delete them later.
 */
object HistoryRepository {

    private const val PREFS_NAME = "omniconvert_history"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 200

    private var prefs: SharedPreferences? = null

    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p
        _entries.value = decode(p.getString(KEY_ENTRIES, null))
    }

    /**
     * Records a produced file. Name and size are read from the content resolver when available.
     */
    fun record(
        context: Context,
        uri: Uri,
        toolLabel: String,
        kind: HistoryKind,
        fallbackName: String = "File",
        fallbackSize: Long = 0L
    ) {
        init(context)
        var name = fallbackName
        var size = fallbackSize
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIdx >= 0) cursor.getString(nameIdx)?.let { name = it }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
                }
            }
        }
        val entry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            name = name,
            toolLabel = toolLabel,
            kind = kind,
            uri = uri.toString(),
            sizeBytes = size,
            timestamp = System.currentTimeMillis()
        )
        update((listOf(entry) + _entries.value).take(MAX_ENTRIES))
    }

    fun remove(id: String) {
        update(_entries.value.filterNot { it.id == id })
    }

    /** Removes the entry and tries to delete the underlying file (works for files created by this app). */
    fun deleteWithFile(context: Context, id: String): Boolean {
        val entry = _entries.value.firstOrNull { it.id == id } ?: return false
        val deleted = runCatching {
            context.contentResolver.delete(Uri.parse(entry.uri), null, null) > 0
        }.getOrDefault(false)
        remove(id)
        return deleted
    }

    fun clear() {
        update(emptyList())
    }

    private fun update(list: List<HistoryEntry>) {
        _entries.value = list
        prefs?.edit()?.putString(KEY_ENTRIES, encode(list))?.apply()
    }

    private fun encode(list: List<HistoryEntry>): String {
        val array = JSONArray()
        list.forEach { e ->
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("name", e.name)
                    .put("tool", e.toolLabel)
                    .put("kind", e.kind.name)
                    .put("uri", e.uri)
                    .put("size", e.sizeBytes)
                    .put("ts", e.timestamp)
            )
        }
        return array.toString()
    }

    private fun decode(raw: String?): List<HistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                HistoryEntry(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    name = o.optString("name", "File"),
                    toolLabel = o.optString("tool", ""),
                    kind = HistoryKind.entries.firstOrNull { it.name == o.optString("kind") } ?: HistoryKind.PDF,
                    uri = o.optString("uri", ""),
                    sizeBytes = o.optLong("size", 0L),
                    timestamp = o.optLong("ts", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }
}

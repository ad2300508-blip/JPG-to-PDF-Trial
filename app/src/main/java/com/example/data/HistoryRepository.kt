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
    AUDIO("Audio"),
    OTHER("Altro")
}

/** Best-guess history category from a file name (used for files extracted from ZIPs). */
fun kindForFileName(name: String): HistoryKind = when (name.substringAfterLast('.', "").lowercase()) {
    "pdf" -> HistoryKind.PDF
    "md", "markdown", "txt" -> HistoryKind.MARKDOWN
    "zip" -> HistoryKind.ZIP
    "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp" -> HistoryKind.IMAGE
    "mp3", "m4a", "aac", "wav", "ogg", "opus", "flac" -> HistoryKind.AUDIO
    else -> HistoryKind.OTHER
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
    @Synchronized
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

    @Synchronized
    fun remove(id: String) {
        update(_entries.value.filterNot { it.id == id })
    }

    /** Puts back an entry removed by mistake (undo), at its original position when possible. */
    @Synchronized
    fun restore(entry: HistoryEntry, index: Int) {
        if (_entries.value.any { it.id == entry.id }) return
        val list = _entries.value.toMutableList()
        list.add(index.coerceIn(0, list.size), entry)
        update(list.take(MAX_ENTRIES))
    }

    /** Drops entries whose file no longer exists (deleted from the file manager). Returns how many were removed. */
    @Synchronized
    fun pruneMissing(context: Context): Int {
        val alive = _entries.value.filter { entry ->
            runCatching {
                context.contentResolver.openFileDescriptor(Uri.parse(entry.uri), "r")?.use { true } ?: false
            }.getOrDefault(false)
        }
        val removed = _entries.value.size - alive.size
        if (removed > 0) update(alive)
        return removed
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

    @Synchronized
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

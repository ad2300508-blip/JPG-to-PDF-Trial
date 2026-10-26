package com.example.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Sub-folders of Download/OmniConvert where each tool saves its output. */
enum class OutputFolder(val dirName: String) {
    PDFS("PDFs"),
    IMAGES("Images"),
    AUDIO("Audio"),
    ARCHIVES("Archives"),
    MARKDOWN("Markdown"),
    STUDY_GUIDES("StudyGuides")
}

/**
 * Single place where the app writes its output files.
 *
 * - Android 10+: MediaStore Downloads (`Download/OmniConvert/<folder>`), written with IS_PENDING so a
 *   half-written file never shows up in other apps and is removed if the write fails or is cancelled.
 * - Android 8-9: `MediaStore.Downloads` does not exist there, so files go to the app's own external
 *   Download folder and are exposed through the FileProvider (no storage permission needed).
 */
object OutputStorage {

    const val ROOT_DIR = "OmniConvert"

    fun displayPath(folder: OutputFolder): String = "Download/$ROOT_DIR/${folder.dirName}"

    suspend fun save(
        context: Context,
        fileName: String,
        mimeType: String,
        folder: OutputFolder,
        writer: suspend (OutputStream) -> Unit
    ): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(context, fileName, mimeType, folder, writer)
        } else {
            saveToAppFolder(context, fileName, folder, writer)
        }
    }

    private suspend fun saveToMediaStore(
        context: Context,
        fileName: String,
        mimeType: String,
        folder: OutputFolder,
        writer: suspend (OutputStream) -> Unit
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$ROOT_DIR/${folder.dirName}")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Impossibile creare il file in Download")
        try {
            val stream = resolver.openOutputStream(uri)
                ?: throw IllegalStateException("Impossibile scrivere il file in Download")
            BufferedOutputStream(stream).use { writer(it) }
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
    }

    private suspend fun saveToAppFolder(
        context: Context,
        fileName: String,
        folder: OutputFolder,
        writer: suspend (OutputStream) -> Unit
    ): Uri {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(context.filesDir, "Download")
        val dir = File(base, "$ROOT_DIR/${folder.dirName}").apply { mkdirs() }
        val file = uniqueFile(dir, fileName)
        try {
            BufferedOutputStream(FileOutputStream(file)).use { writer(it) }
        } catch (t: Throwable) {
            file.delete()
            throw t
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        val base = fileName.substringBeforeLast('.', fileName)
        val ext = fileName.substringAfterLast('.', "")
        var n = 2
        while (candidate.exists()) {
            candidate = File(dir, if (ext.isEmpty()) "$base ($n)" else "$base ($n).$ext")
            n++
        }
        return candidate
    }
}

object FileNames {

    private val INVALID = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    /** Keeps letters (accents included), digits and common punctuation; never returns an empty name. */
    fun sanitize(raw: String, fallback: String): String {
        val cleaned = raw.replace(INVALID, "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.', ' ')
            .take(80)
        return cleaned.ifBlank { fallback }
    }

    fun timestamp(date: Date = Date()): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(date)

    /** "Appunti" + "pdf" -> "Appunti_20261026_153000.pdf" */
    fun stamped(raw: String, fallback: String, extension: String): String =
        "${sanitize(raw.removeSuffix(".$extension"), fallback)}_${timestamp()}.$extension"

    fun baseName(name: String): String = name.substringBeforeLast('.', name).ifBlank { name }

    /** Makes ZIP entry names unique: "a.txt", "a (2).txt", "a (3).txt"... */
    fun dedupe(names: List<String>): List<String> {
        val used = HashSet<String>()
        return names.map { original ->
            val name = original.ifBlank { "file" }
            var candidate = name
            var n = 2
            while (!used.add(candidate.lowercase(Locale.ROOT))) {
                val base = name.substringBeforeLast('.', name)
                val ext = name.substringAfterLast('.', "")
                candidate = if (ext.isEmpty() || base == name) "$name ($n)" else "$base ($n).$ext"
                n++
            }
            candidate
        }
    }
}

/** Like runCatching, but lets coroutine cancellation propagate instead of turning it into a failure. */
inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (c: CancellationException) {
    throw c
} catch (t: Throwable) {
    Result.failure(t)
}

/** Human readable Italian message for an exception thrown by a conversion. */
fun Throwable.userMessage(fallback: String): String = when (this) {
    is OutOfMemoryError -> "Memoria insufficiente: prova con meno file o una qualità più bassa"
    is SecurityException -> "Permesso negato per leggere il file"
    is java.io.FileNotFoundException -> "File non trovato o non più accessibile"
    else -> localizedMessage?.takeIf { it.isNotBlank() } ?: fallback
}

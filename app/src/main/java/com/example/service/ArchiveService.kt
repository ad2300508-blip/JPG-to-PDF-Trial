package com.example.service

import android.content.Context
import android.net.Uri
import com.example.model.ArchiveConfig
import com.example.model.ArchiveItem
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

class ArchiveService(private val context: Context) {

    /** Streams every file straight into the ZIP (no full copy in RAM), with unique entry names. */
    suspend fun createZip(
        items: List<ArchiveItem>,
        config: ArchiveConfig,
        onProgress: (Int, Int) -> Unit
    ): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        resultOf {
            val entryNames = FileNames.dedupe(items.map { FileNames.sanitize(it.name, "file") })
            val fileName = FileNames.stamped(config.zipFileName, "Archivio", "zip")
            var written = 0L
            val buffer = ByteArray(64 * 1024)

            val uri = OutputStorage.save(context, fileName, "application/zip", OutputFolder.ARCHIVES) { out ->
                val counting = CountingOutputStream(out)
                ZipOutputStream(counting).use { zip ->
                    zip.setLevel(config.compressionLevel.coerceIn(0, 9))
                    items.forEachIndexed { index, item ->
                        coroutineContext.ensureActive()
                        onProgress(index + 1, items.size)
                        val input = context.contentResolver.openInputStream(item.uri)
                            ?: throw IllegalStateException("Impossibile leggere ${item.name}")
                        input.use { stream ->
                            zip.putNextEntry(ZipEntry(entryNames[index]))
                            while (true) {
                                val read = stream.read(buffer)
                                if (read < 0) break
                                zip.write(buffer, 0, read)
                            }
                            zip.closeEntry()
                        }
                    }
                    zip.finish()
                    written = counting.count
                }
            }
            uri to written
        }
    }

    data class ExtractedFile(val name: String, val uri: Uri, val sizeBytes: Long)

    /** Lists the entries of a ZIP without extracting it (name, uncompressed size when known). */
    suspend fun listZip(uri: Uri): Result<List<Pair<String, Long>>> = withContext(Dispatchers.IO) {
        resultOf {
            val entries = mutableListOf<Pair<String, Long>>()
            context.contentResolver.openInputStream(uri)?.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory) entries += entry.name to entry.size
                        zip.closeEntry()
                    }
                }
            } ?: throw IllegalStateException("Impossibile aprire l'archivio")
            if (entries.isEmpty()) throw IllegalStateException("L'archivio è vuoto o non è uno ZIP valido")
            entries
        }
    }

    /**
     * Extracts every file of a ZIP into Download/Morph/Archives. Folder structure is flattened
     * into the file name ("cartella_file.txt") and path traversal entries are neutralised.
     */
    suspend fun extractZip(
        uri: Uri,
        onProgress: (Int, String) -> Unit
    ): Result<List<ExtractedFile>> = withContext(Dispatchers.IO) {
        resultOf {
            val results = mutableListOf<ExtractedFile>()
            val usedNames = mutableListOf<String>()
            val buffer = ByteArray(64 * 1024)
            context.contentResolver.openInputStream(uri)?.use { raw ->
                ZipInputStream(raw.buffered()).use { zip ->
                    var index = 0
                    while (true) {
                        coroutineContext.ensureActive()
                        val entry = zip.nextEntry ?: break
                        if (entry.isDirectory) continue
                        index++
                        val flatName = entry.name.replace('\\', '/').split('/')
                            .filter { it.isNotBlank() && it != "." && it != ".." }
                            .joinToString("_")
                        val safeName = FileNames.dedupe(usedNames + FileNames.sanitize(flatName, "file_$index")).last()
                        usedNames += safeName
                        onProgress(index, safeName)

                        // Copy to a temp file first so a broken entry never leaves a half file in Download.
                        val temp = File(context.cacheDir, "unzip_${System.nanoTime()}")
                        try {
                            temp.outputStream().buffered().use { out ->
                                while (true) {
                                    val read = zip.read(buffer)
                                    if (read < 0) break
                                    out.write(buffer, 0, read)
                                }
                            }
                            val saved = OutputStorage.save(context, safeName, mimeFor(safeName), OutputFolder.ARCHIVES) { out ->
                                temp.inputStream().use { it.copyTo(out) }
                            }
                            results += ExtractedFile(safeName, saved, temp.length())
                        } finally {
                            temp.delete()
                        }
                        zip.closeEntry()
                    }
                }
            } ?: throw IllegalStateException("Impossibile aprire l'archivio")
            if (results.isEmpty()) throw IllegalStateException("L'archivio non contiene file")
            results
        }
    }

    private fun mimeFor(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    private class CountingOutputStream(private val target: java.io.OutputStream) : java.io.OutputStream() {
        var count = 0L
            private set

        override fun write(b: Int) {
            target.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            target.write(b, off, len)
            count += len
        }

        override fun flush() = target.flush()

        // The underlying stream is closed by OutputStorage.
        override fun close() = target.flush()
    }
}

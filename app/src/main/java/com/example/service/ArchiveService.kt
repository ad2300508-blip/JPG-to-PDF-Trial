package com.example.service

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.ArchiveConfig
import com.example.model.ArchiveItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveService(private val context: Context) {

    suspend fun createZip(
        items: List<ArchiveItem>,
        config: ArchiveConfig,
        onProgress: (Int, Int) -> Unit
    ): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        try {
            val byteStream = ByteArrayOutputStream()
            val zipOut = ZipOutputStream(byteStream)
            zipOut.setLevel(config.compressionLevel.coerceIn(0, 9))

            val buffer = ByteArray(8192)
            val total = items.size

            for ((index, item) in items.withIndex()) {
                onProgress(index + 1, total)
                val inputStream = context.contentResolver.openInputStream(item.uri) ?: continue
                val bufferedIn = BufferedInputStream(inputStream)

                val zipEntry = ZipEntry(item.name)
                zipOut.putNextEntry(zipEntry)

                var count: Int
                while (bufferedIn.read(buffer).also { count = it } != -1) {
                    zipOut.write(buffer, 0, count)
                }

                zipOut.closeEntry()
                bufferedIn.close()
            }

            zipOut.finish()
            zipOut.close()

            val zipBytes = byteStream.toByteArray()
            val filename = "${config.zipFileName.ifBlank { "OmniArchive" }}_${System.currentTimeMillis()}.zip"

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OmniConvert/Archives")
                }
            }

            val outputUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Could not create MediaStore entry for ZIP"))

            resolver.openOutputStream(outputUri)?.use { out ->
                out.write(zipBytes)
            }

            Result.success(Pair(outputUri, zipBytes.size.toLong()))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

package com.example.service

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.ArchiveFileItem
import com.example.model.ArchiveResult
import com.example.model.ArchiveSettings
import com.example.model.ZipCompressionLevel
import com.example.util.DownloadUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ArchiveService {

  suspend fun extractMetadata(context: Context, uri: Uri): ArchiveFileItem = withContext(Dispatchers.IO) {
    var fileName = "file_${System.currentTimeMillis()}"
    var fileSize = 0L

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (nameIndex != -1) {
            val name = cursor.getString(nameIndex)
            if (!name.isNullOrBlank()) fileName = name
          }
          if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
            fileSize = cursor.getLong(sizeIndex)
          }
        }
      }
    } catch (_: Exception) { }

    val mime = context.contentResolver.getType(uri) ?: "*/*"

    ArchiveFileItem(
      id = UUID.randomUUID().toString(),
      uri = uri,
      fileName = fileName,
      sizeBytes = fileSize,
      mimeType = mime
    )
  }

  suspend fun createZipArchive(
    context: Context,
    items: List<ArchiveFileItem>,
    settings: ArchiveSettings,
    onProgress: (progress: Float, currentFile: String) -> Unit
  ): Result<ArchiveResult> = withContext(Dispatchers.IO) {
    try {
      if (items.isEmpty()) throw IllegalArgumentException("No files selected to archive")

      val rawName = settings.archiveName.trim().ifEmpty { "Archive_${System.currentTimeMillis() / 1000}" }
      val cleanName = if (rawName.endsWith(".zip", ignoreCase = true)) rawName else "$rawName.zip"
      val outputFile = File(context.cacheDir, "omni_${System.currentTimeMillis()}_$cleanName")

      val usedNames = mutableSetOf<String>()
      var originalTotalBytes = 0L

      ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zipOut ->
        zipOut.setLevel(settings.compressionLevel.level)

        val total = items.size
        val buffer = ByteArray(16384)

        for (i in 0 until total) {
          val item = items[i]
          onProgress(i.toFloat() / total.toFloat(), item.fileName)

          // Deduplicate entry names
          var entryName = item.fileName
          var counter = 1
          while (usedNames.contains(entryName)) {
            val base = item.fileName.substringBeforeLast(".")
            val ext = item.fileName.substringAfterLast(".", "")
            entryName = if (ext.isNotEmpty() && ext != item.fileName) {
              "${base}_$counter.$ext"
            } else {
              "${item.fileName}_$counter"
            }
            counter++
          }
          usedNames.add(entryName)

          val entry = ZipEntry(entryName)
          zipOut.putNextEntry(entry)

          var fileBytesRead = 0L
          context.contentResolver.openInputStream(item.uri)?.use { inStream ->
            var read: Int
            while (inStream.read(buffer).also { read = it } != -1) {
              zipOut.write(buffer, 0, read)
              fileBytesRead += read
            }
          }
          zipOut.closeEntry()

          originalTotalBytes += if (item.sizeBytes > 0) item.sizeBytes else fileBytesRead
        }
      }

      onProgress(1f, "Finalizing archive...")

      Result.success(
        ArchiveResult(
          file = outputFile,
          fileName = cleanName,
          originalTotalBytes = originalTotalBytes,
          compressedBytes = outputFile.length()
        )
      )
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun exportArchiveToDownloads(
    context: Context,
    result: ArchiveResult
  ): Result<DownloadUtils.DownloadResult> {
    return DownloadUtils.saveFileToDownloads(
      context = context,
      sourceFile = result.file,
      desiredFileName = result.fileName,
      mimeType = "application/zip",
      subfolder = "OmniConvert/Archives"
    )
  }
}

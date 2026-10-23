package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

object DownloadUtils {

  data class DownloadResult(
    val uri: Uri,
    val displayPath: String,
    val fileName: String,
  )

  /**
   * Saves any generated file into the public Downloads directory (with optional subfolder like "OmniConvert/Images")
   * so it appears immediately in the user's Files / Downloads app without requiring SAF picker navigation.
   */
  suspend fun saveFileToDownloads(
    context: Context,
    sourceFile: File,
    desiredFileName: String,
    mimeType: String,
    subfolder: String = ""
  ): Result<DownloadResult> = withContext(Dispatchers.IO) {
    try {
      val relativePath = if (subfolder.isNotBlank()) {
        "${Environment.DIRECTORY_DOWNLOADS}/$subfolder"
      } else {
        Environment.DIRECTORY_DOWNLOADS
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, desiredFileName)
          put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
          put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
          put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val itemUri = resolver.insert(collectionUri, contentValues)
          ?: throw IllegalStateException("Failed to create download entry in MediaStore")

        resolver.openOutputStream(itemUri, "w")?.use { outStream ->
          FileInputStream(sourceFile).use { inStream ->
            inStream.copyTo(outStream)
          }
          outStream.flush()
        } ?: throw IllegalStateException("Failed to open output stream for download URI: $itemUri")

        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(itemUri, contentValues, null, null)

        val display = if (subfolder.isNotBlank()) "Downloads/$subfolder/$desiredFileName" else "Downloads/$desiredFileName"
        Result.success(
          DownloadResult(
            uri = itemUri,
            displayPath = display,
            fileName = desiredFileName,
          )
        )
      } else {
        // Fallback for API 24 - 28
        val baseDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val targetDir = if (subfolder.isNotBlank()) File(baseDownloads, subfolder) else baseDownloads
        if (!targetDir.exists()) {
          targetDir.mkdirs()
        }
        val targetFile = File(targetDir, desiredFileName)
        FileInputStream(sourceFile).use { inStream ->
          targetFile.outputStream().use { outStream ->
            inStream.copyTo(outStream)
            outStream.flush()
          }
        }

        MediaScannerConnection.scanFile(
          context,
          arrayOf(targetFile.absolutePath),
          arrayOf(mimeType),
          null
        )

        Result.success(
          DownloadResult(
            uri = Uri.fromFile(targetFile),
            displayPath = targetFile.absolutePath,
            fileName = desiredFileName,
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Saves the generated PDF directly into the public Downloads directory so it appears
   * immediately in the user's Files / Downloads app without requiring SAF picker navigation.
   */
  suspend fun savePdfToDownloads(
    context: Context,
    sourceFile: File,
    desiredFileName: String,
  ): Result<DownloadResult> {
    val cleanFileName = if (desiredFileName.endsWith(".pdf", ignoreCase = true)) {
      desiredFileName
    } else {
      "$desiredFileName.pdf"
    }
    return saveFileToDownloads(context, sourceFile, cleanFileName, "application/pdf")
  }

  /**
   * Safely launches an intent to open a file with any installed viewer.
   */
  fun openFile(context: Context, uri: Uri, mimeType: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, mimeType)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      try {
        context.startActivity(Intent.createChooser(intent, "Open File"))
        true
      } catch (_: Exception) {
        false
      }
    }
  }

  fun openPdf(context: Context, uri: Uri): Boolean = openFile(context, uri, "application/pdf")

  /**
   * Safely launches an intent to share a file.
   */
  fun shareFile(context: Context, uri: Uri, mimeType: String, title: String = "Share File"): Boolean {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = mimeType
      putExtra(Intent.EXTRA_STREAM, uri)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
      context.startActivity(Intent.createChooser(intent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      })
      true
    } catch (_: Exception) {
      false
    }
  }

  fun sharePdf(context: Context, uri: Uri, title: String = "Share PDF"): Boolean =
    shareFile(context, uri, "application/pdf", title)
}

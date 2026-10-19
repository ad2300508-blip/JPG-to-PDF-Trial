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
   * Saves the generated PDF directly into the public Downloads directory so it appears
   * immediately in the user's Files / Downloads app without requiring SAF picker navigation.
   */
  suspend fun savePdfToDownloads(
    context: Context,
    sourceFile: File,
    desiredFileName: String,
  ): Result<DownloadResult> = withContext(Dispatchers.IO) {
    try {
      val cleanFileName = if (desiredFileName.endsWith(".pdf", ignoreCase = true)) {
        desiredFileName
      } else {
        "$desiredFileName.pdf"
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, cleanFileName)
          put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
          put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
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

        Result.success(
          DownloadResult(
            uri = itemUri,
            displayPath = "Downloads/$cleanFileName",
            fileName = cleanFileName,
          )
        )
      } else {
        // Fallback for API 24 - 28
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
          downloadsDir.mkdirs()
        }
        val targetFile = File(downloadsDir, cleanFileName)
        FileInputStream(sourceFile).use { inStream ->
          targetFile.outputStream().use { outStream ->
            inStream.copyTo(outStream)
            outStream.flush()
          }
        }

        MediaScannerConnection.scanFile(
          context,
          arrayOf(targetFile.absolutePath),
          arrayOf("application/pdf"),
          null
        )

        Result.success(
          DownloadResult(
            uri = Uri.fromFile(targetFile),
            displayPath = targetFile.absolutePath,
            fileName = cleanFileName,
          )
        )
      }
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  /**
   * Safely launches an intent to open a PDF file with any installed viewer.
   */
  fun openPdf(context: Context, uri: Uri): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, "application/pdf")
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
      context.startActivity(intent)
      true
    } catch (_: Exception) {
      try {
        context.startActivity(Intent.createChooser(intent, "Open PDF"))
        true
      } catch (_: Exception) {
        false
      }
    }
  }

  /**
   * Safely launches an intent to share a PDF file.
   */
  fun sharePdf(context: Context, uri: Uri, title: String = "Share PDF"): Boolean {
    val intent = Intent(Intent.ACTION_SEND).apply {
      type = "application/pdf"
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
}

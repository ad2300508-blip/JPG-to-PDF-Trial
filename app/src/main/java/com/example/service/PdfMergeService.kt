package com.example.service

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.PdfDocumentItem
import com.example.model.PdfMergeSettings
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfMergeService(private val context: Context) {

    init {
        try {
            PDFBoxResourceLoader.init(context)
        } catch (_: Throwable) {}
    }

    suspend fun mergePdfs(
        documents: List<PdfDocumentItem>,
        settings: PdfMergeSettings,
        onProgress: (Int, Int) -> Unit
    ): Result<Uri> = withContext(Dispatchers.IO) {
        if (documents.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No documents provided for merging"))
        }

        try {
            val validDocs = documents.filter { !it.isCorrupted }
            if (validDocs.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("All selected documents are marked as corrupted"))
            }

            val tempOutputFile = File(context.cacheDir, "merged_temp_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(tempOutputFile)

            val merger = PDFMergerUtility()
            merger.destinationStream = outputStream

            val loadedDocs = mutableListOf<PDDocument>()
            val total = validDocs.size

            try {
                for ((index, item) in validDocs.withIndex()) {
                    onProgress(index + 1, total)

                    val docUri = item.uri ?: continue
                    val pfd = context.contentResolver.openFileDescriptor(docUri, "r")
                        ?: throw IllegalStateException("Cannot access file descriptor for ${item.name}")

                    val doc = pfd.use { descriptor ->
                        context.contentResolver.openInputStream(item.uri)?.use { stream ->
                            PDDocument.load(stream, MemoryUsageSetting.setupTempFileOnly())
                        } ?: throw IllegalStateException("Cannot open input stream for ${item.name}")
                    }

                    loadedDocs.add(doc)
                    merger.addSource(tempOutputFile) // or append documents directly
                }

                // Append pages into target PDDocument
                val targetDoc = PDDocument()
                for (doc in loadedDocs) {
                    for (i in 0 until doc.numberOfPages) {
                        targetDoc.addPage(doc.getPage(i))
                    }
                }

                if (settings.enableEncryption && settings.hasValidPassword) {
                    // Password protection if requested
                    val accessPermissions = com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission()
                    val standardProtection = com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy(
                        settings.ownerPassword.ifBlank { settings.userPassword },
                        settings.userPassword,
                        accessPermissions
                    )
                    standardProtection.encryptionKeyLength = 128
                    targetDoc.protect(standardProtection)
                }

                targetDoc.save(outputStream)
                targetDoc.close()
            } finally {
                outputStream.close()
                for (doc in loadedDocs) {
                    try { doc.close() } catch (_: Throwable) {}
                }
            }

            val baseName = settings.outputFileName.ifBlank { "Merged_Document" }
            val fileName = "${baseName}_${System.currentTimeMillis()}.pdf"

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/OmniConvert/PDFs")
                }
            }

            val resultUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(IllegalStateException("Could not create MediaStore entry for merged PDF"))

            resolver.openOutputStream(resultUri)?.use { out ->
                tempOutputFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            tempOutputFile.delete()

            Result.success(resultUri)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

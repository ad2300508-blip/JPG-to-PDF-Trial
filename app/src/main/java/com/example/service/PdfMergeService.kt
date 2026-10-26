package com.example.service

import android.content.Context
import android.net.Uri
import com.example.model.PdfDocumentItem
import com.example.model.PdfMergeSettings
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.PageRange
import com.example.util.resultOf
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

class PdfMergeService(private val context: Context) {

    init {
        runCatching { PDFBoxResourceLoader.init(context) }
    }

    /** Page count read with PDFBox (works for protected PDFs once the password is known). */
    suspend fun readPageCount(uri: Uri, password: String): Result<Int> = withContext(Dispatchers.IO) {
        resultOf {
            PDFBoxResourceLoader.init(context)
            openDocument(uri, password).use { it.numberOfPages }
        }
    }

    suspend fun mergePdfs(
        documents: List<PdfDocumentItem>,
        settings: PdfMergeSettings,
        onProgress: (Int, Int) -> Unit
    ): Result<Uri> = withContext(Dispatchers.IO) {
        resultOf {
            PDFBoxResourceLoader.init(context)
            val valid = documents.filter { !it.isCorrupted && it.uri != null }
            if (valid.isEmpty()) throw IllegalStateException("Nessun PDF valido da unire")
            if (!settings.hasValidPassword) throw IllegalStateException("Inserisci una password per proteggere il PDF")

            val sources = mutableListOf<PDDocument>()
            try {
                PDDocument(MemoryUsageSetting.setupTempFileOnly()).use { target ->
                    valid.forEachIndexed { index, item ->
                        coroutineContext.ensureActive()
                        onProgress(index + 1, valid.size)
                        val source = openDocument(item.uri!!, item.password)
                        sources += source
                        if (source.isEncrypted) source.isAllSecurityToBeRemoved = true
                        val pages = PageRange.parse(item.pageRange, source.numberOfPages)
                        if (pages.isEmpty()) throw IllegalStateException("Intervallo di pagine non valido per ${item.name}")
                        pages.forEach { pageIndex -> target.importPage(source.getPage(pageIndex)) }
                    }

                    target.documentInformation.title = settings.outputFileName.ifBlank { "PDF unito" }
                    target.documentInformation.producer = "OmniConvert Studio"

                    if (settings.enableEncryption) {
                        val policy = StandardProtectionPolicy(
                            settings.ownerPassword.ifBlank { settings.userPassword },
                            settings.userPassword,
                            AccessPermission()
                        )
                        policy.encryptionKeyLength = 128
                        target.protect(policy)
                    }

                    val fileName = FileNames.stamped(settings.outputFileName, "PDF_unito", "pdf")
                    OutputStorage.save(context, fileName, "application/pdf", OutputFolder.PDFS) { out -> target.save(out) }
                }
            } finally {
                sources.forEach { runCatching { it.close() } }
            }
        }
    }

    private fun openDocument(uri: Uri, password: String): PDDocument {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Impossibile aprire il PDF")
        return try {
            stream.use { PDDocument.load(it, password, MemoryUsageSetting.setupTempFileOnly()) }
        } catch (e: InvalidPasswordException) {
            throw IllegalStateException(if (password.isEmpty()) "PDF protetto: inserisci la password" else "Password errata")
        }
    }
}

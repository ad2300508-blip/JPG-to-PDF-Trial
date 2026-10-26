package com.example.service

import android.content.Context
import android.net.Uri
import com.example.util.DocStats
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.resultOf
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import kotlin.math.abs

/** Editable document properties (blank = not set). */
data class PdfMetadata(
    val title: String = "",
    val author: String = "",
    val subject: String = "",
    val keywords: String = ""
)

/** What "Info PDF" shows about a document. */
data class PdfDetails(
    val metadata: PdfMetadata,
    val creator: String,
    val producer: String,
    val createdAt: Long?,
    val modifiedAt: Long?,
    val version: Float,
    val pageCount: Int,
    /** First page, as displayed (rotation applied), in points. */
    val pageWidthPt: Float,
    val pageHeightPt: Float,
    val mixedPageSizes: Boolean,
    val imageCount: Int,
    val fontCount: Int,
    val words: Int,
    val characters: Int,
    /** Pages whose text was counted (very long PDFs are sampled up to [TEXT_PAGE_LIMIT]). */
    val countedPages: Int
) {
    companion object {
        const val TEXT_PAGE_LIMIT = 400
    }
}

class PdfInsightService(private val context: Context) {

    init {
        runCatching { PDFBoxResourceLoader.init(context) }
    }

    suspend fun details(uri: Uri): Result<PdfDetails> = withContext(Dispatchers.IO) {
        resultOf {
            load(uri).use { doc ->
                val info = doc.documentInformation
                val pages = doc.pages.toList()
                var firstW = 0f
                var firstH = 0f
                var mixed = false
                var images = 0
                val fonts = HashSet<String>()
                pages.forEachIndexed { index, page ->
                    ensureActive()
                    val box = page.mediaBox
                    val sideways = (page.rotation % 180 + 180) % 180 == 90
                    val w = if (sideways) box.height else box.width
                    val h = if (sideways) box.width else box.height
                    if (index == 0) {
                        firstW = w
                        firstH = h
                    } else if (abs(w - firstW) > 2f || abs(h - firstH) > 2f) {
                        mixed = true
                    }
                    val resources = page.resources ?: return@forEachIndexed
                    images += runCatching { resources.xObjectNames.count { resources.isImageXObject(it) } }.getOrDefault(0)
                    runCatching {
                        resources.fontNames.forEach { name -> resources.getFont(name)?.name?.let { fonts += it.substringAfter('+') } }
                    }
                }
                ensureActive()
                val counted = minOf(pages.size, PdfDetails.TEXT_PAGE_LIMIT)
                val text = runCatching {
                    PDFTextStripper().apply {
                        startPage = 1
                        endPage = counted
                    }.getText(doc)
                }.getOrDefault("")
                PdfDetails(
                    metadata = PdfMetadata(
                        title = info.title.orEmpty(),
                        author = info.author.orEmpty(),
                        subject = info.subject.orEmpty(),
                        keywords = info.keywords.orEmpty()
                    ),
                    creator = info.creator.orEmpty(),
                    producer = info.producer.orEmpty(),
                    createdAt = runCatching { info.creationDate?.timeInMillis }.getOrNull(),
                    modifiedAt = runCatching { info.modificationDate?.timeInMillis }.getOrNull(),
                    version = doc.version,
                    pageCount = pages.size,
                    pageWidthPt = firstW,
                    pageHeightPt = firstH,
                    mixedPageSizes = mixed,
                    imageCount = images,
                    fontCount = fonts.size,
                    words = DocStats.words(text),
                    characters = DocStats.characters(text),
                    countedPages = counted
                )
            }
        }
    }

    /**
     * Saves a copy of the PDF with [metadata]; null removes every property (information
     * dictionary and XMP), handy before sending a document around.
     */
    suspend fun saveMetadata(uri: Uri, name: String, metadata: PdfMetadata?): Result<Uri> = withContext(Dispatchers.IO) {
        resultOf {
            load(uri).use { doc ->
                if (metadata == null) {
                    doc.documentInformation = PDDocumentInformation()
                    doc.documentCatalog.metadata = null
                } else {
                    val info = doc.documentInformation
                    info.title = metadata.title.trim().ifBlank { null }
                    info.author = metadata.author.trim().ifBlank { null }
                    info.subject = metadata.subject.trim().ifBlank { null }
                    info.keywords = metadata.keywords.trim().ifBlank { null }
                    info.modificationDate = Calendar.getInstance()
                    doc.documentInformation = info
                }
                val suffix = if (metadata == null) "senza_metadati" else "info"
                val fileName = FileNames.stamped("${FileNames.baseName(name)}_$suffix", "Documento", "pdf")
                val temp = File(context.cacheDir, "insight_${System.nanoTime()}.pdf")
                try {
                    temp.outputStream().buffered().use { doc.save(it) }
                    ensureActive()
                    OutputStorage.save(context, fileName, "application/pdf", OutputFolder.PDFS) { out ->
                        temp.inputStream().use { it.copyTo(out) }
                    }
                } finally {
                    temp.delete()
                }
            }
        }
    }

    private fun load(uri: Uri): PDDocument {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Impossibile aprire il PDF")
        return try {
            stream.use { PDDocument.load(it, MemoryUsageSetting.setupTempFileOnly()) }
        } catch (e: InvalidPasswordException) {
            throw IllegalStateException("Il PDF è protetto da password: sbloccalo prima con «Proteggi PDF» › Sblocca")
        }
    }
}

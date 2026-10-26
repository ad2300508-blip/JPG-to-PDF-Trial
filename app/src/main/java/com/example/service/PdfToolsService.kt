package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.example.util.BitmapLoader
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.resultOf
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSBase
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.IdentityHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

/** How hard "Comprimi PDF" recompresses the images inside a document. */
enum class PdfCompressionLevel(val label: String, val description: String, val maxDimension: Int, val jpegQuality: Float) {
    LIGHT("Leggera", "Quasi nessuna perdita visibile", 2400, 0.85f),
    MEDIUM("Media", "Il giusto compromesso per studiare", 1600, 0.70f),
    STRONG("Forte", "File minimo, ideale per email e WhatsApp", 1100, 0.50f)
}

enum class PdfImageFormat(val label: String, val extension: String, val mimeType: String) {
    JPEG("JPG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png")
}

/** One page in "Organizza PDF": its position in the source document and the extra rotation chosen by the user. */
data class PdfPageEdit(val sourceIndex: Int, val rotation: Int = 0)

data class PdfInfo(val pageCount: Int, val isEncrypted: Boolean)

class PdfToolsService(private val context: Context) {

    private val renderMutex = Mutex()

    init {
        runCatching { PDFBoxResourceLoader.init(context) }
    }

    // ---------------------------------------------------------------------------------------------
    // Inspection & thumbnails
    // ---------------------------------------------------------------------------------------------

    suspend fun readInfo(uri: Uri): Result<PdfInfo> = withContext(Dispatchers.IO) {
        resultOf {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { PdfInfo(it.pageCount, isEncrypted = false) }
                } ?: throw IllegalStateException("Impossibile aprire il PDF")
            } catch (e: SecurityException) {
                PdfInfo(pageCount = 0, isEncrypted = true)
            }
        }
    }

    /** Renders one page to a bitmap [widthPx] wide (white background). PdfRenderer is not thread safe, hence the lock. */
    suspend fun renderPage(uri: Uri, pageIndex: Int, widthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        renderMutex.withLock {
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (pageIndex !in 0 until renderer.pageCount) return@use null
                        renderer.openPage(pageIndex).use { page ->
                            // Very tall pages (receipts, long scans) would need hundreds of MB at full width.
                            val scale = minOf(widthPx.toFloat() / page.width, MAX_PREVIEW_SIDE / maxOf(page.width, page.height))
                            renderToBitmap(page, scale)
                        }
                    }
                }
            }.getOrNull()
        }
    }

    private fun renderToBitmap(page: PdfRenderer.Page, scale: Float): Bitmap {
        val width = (page.width * scale).roundToInt().coerceAtLeast(1)
        val height = (page.height * scale).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
    }

    // ---------------------------------------------------------------------------------------------
    // Compress
    // ---------------------------------------------------------------------------------------------

    data class CompressionResult(val uri: Uri, val originalBytes: Long, val outputBytes: Long, val imagesRecompressed: Int)

    /**
     * Recompresses the raster images inside the PDF (scans, photos) as JPEG at a lower resolution.
     * Text and vector content are untouched. Images are only replaced when the new version is smaller.
     */
    suspend fun compress(
        uri: Uri,
        name: String,
        originalBytes: Long,
        level: PdfCompressionLevel,
        grayscale: Boolean,
        removeMetadata: Boolean,
        onProgress: (Int, Int) -> Unit
    ): Result<CompressionResult> = withContext(Dispatchers.IO) {
        resultOf {
            PDFBoxResourceLoader.init(context)
            val document = loadDocument(uri)

            var replaced = 0
            document.use { doc ->
                if (doc.isEncrypted) doc.isAllSecurityToBeRemoved = true
                val replacements = IdentityHashMap<COSBase, PDImageXObject>()
                val visitedForms = HashSet<COSBase>()
                val total = doc.numberOfPages
                doc.pages.forEachIndexed { index, page ->
                    coroutineContext.ensureActive()
                    onProgress(index + 1, total)
                    replaced += compressResources(doc, page.resources, level, grayscale, replacements, visitedForms)
                }
                if (removeMetadata) {
                    doc.documentCatalog.metadata = null
                    val info = doc.documentInformation
                    info.author = null
                    info.keywords = null
                    info.subject = null
                }
                doc.documentInformation.producer = "Morph"

                val fileName = "${FileNames.sanitize(FileNames.baseName(name), "Documento")}_compresso.pdf"
                val temp = File(context.cacheDir, "compress_${System.nanoTime()}.pdf")
                try {
                    temp.outputStream().buffered().use { doc.save(it) }
                    val outputUri = OutputStorage.save(context, fileName, "application/pdf", OutputFolder.PDFS) { out ->
                        temp.inputStream().use { it.copyTo(out) }
                    }
                    CompressionResult(outputUri, originalBytes, temp.length(), replaced)
                } finally {
                    temp.delete()
                }
            }
        }
    }

    private suspend fun compressResources(
        doc: PDDocument,
        resources: PDResources?,
        level: PdfCompressionLevel,
        grayscale: Boolean,
        replacements: IdentityHashMap<COSBase, PDImageXObject>,
        visitedForms: MutableSet<COSBase>
    ): Int {
        if (resources == null) return 0
        var count = 0
        for (name in resources.xObjectNames.toList()) {
            coroutineContext.ensureActive()
            val xObject = runCatching { resources.getXObject(name) }.getOrNull() ?: continue
            when (xObject) {
                is PDImageXObject -> {
                    val key = xObject.cosObject
                    val existing = replacements[key]
                    if (existing != null) {
                        resources.put(name, existing)
                        continue
                    }
                    val recompressed = recompressImage(doc, xObject, level, grayscale) ?: continue
                    replacements[key] = recompressed
                    resources.put(name, recompressed)
                    count++
                }
                is PDFormXObject -> {
                    if (visitedForms.add(xObject.cosObject)) {
                        count += compressResources(doc, xObject.resources, level, grayscale, replacements, visitedForms)
                    }
                }
            }
        }
        return count
    }

    private fun recompressImage(doc: PDDocument, image: PDImageXObject, level: PdfCompressionLevel, grayscale: Boolean): PDImageXObject? {
        return try {
            // Skip tiny images, masks and images with transparency (JPEG would lose it).
            if (image.width * image.height < 120 * 120) return null
            if (image.isStencil) return null
            val dict = image.cosObject
            if (dict.containsKey(COSName.SMASK) || dict.containsKey(COSName.MASK)) return null

            val originalLength = dict.length
            var bitmap = image.image ?: return null
            bitmap = BitmapLoader.scaleDown(bitmap, level.maxDimension)
            val paint = if (grayscale) Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) } else null
            bitmap = BitmapLoader.flattenOnWhite(bitmap, paint)
            val replacement = JPEGFactory.createFromImage(doc, bitmap, level.jpegQuality)
            bitmap.recycle()
            if (replacement.cosObject.length < originalLength * 0.95) replacement else null
        } catch (oom: OutOfMemoryError) {
            System.gc()
            null
        } catch (t: Throwable) {
            // Unsupported encodings (e.g. JPEG 2000) stay as they are.
            null
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Organize / extract
    // ---------------------------------------------------------------------------------------------

    /** Writes a new PDF with [pages] in the given order and rotation. */
    suspend fun exportPages(
        uri: Uri,
        outputName: String,
        pages: List<PdfPageEdit>,
        onProgress: (Int, Int) -> Unit
    ): Result<Uri> = withContext(Dispatchers.IO) {
        resultOf {
            if (pages.isEmpty()) throw IllegalStateException("Seleziona almeno una pagina")
            PDFBoxResourceLoader.init(context)
            loadDocument(uri).use { src ->
                if (src.isEncrypted) src.isAllSecurityToBeRemoved = true
                PDDocument(MemoryUsageSetting.setupTempFileOnly()).use { target ->
                    pages.forEachIndexed { index, edit ->
                        coroutineContext.ensureActive()
                        onProgress(index + 1, pages.size)
                        if (edit.sourceIndex !in 0 until src.numberOfPages) return@forEachIndexed
                        val imported = target.importPage(src.getPage(edit.sourceIndex))
                        if (edit.rotation != 0) {
                            imported.rotation = ((imported.rotation + edit.rotation) % 360 + 360) % 360
                        }
                    }
                    target.documentInformation.producer = "Morph"
                    val fileName = FileNames.stamped(outputName, "Documento", "pdf")
                    OutputStorage.save(context, fileName, "application/pdf", OutputFolder.PDFS) { out -> target.save(out) }
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Split
    // ---------------------------------------------------------------------------------------------

    data class SplitResult(val uris: List<Uri>, val zipUri: Uri?, val parts: Int, val totalBytes: Long)

    /** Writes one PDF per group of 0-based pages; with [asZip] they are bundled in a single archive. */
    suspend fun split(
        uri: Uri,
        name: String,
        groups: List<List<Int>>,
        asZip: Boolean,
        onProgress: (Int, Int) -> Unit
    ): Result<SplitResult> = withContext(Dispatchers.IO) {
        resultOf {
            if (groups.isEmpty()) throw IllegalStateException("Nessuna parte da creare: controlla gli intervalli")
            PDFBoxResourceLoader.init(context)
            val baseName = FileNames.sanitize(FileNames.baseName(name), "Documento")
            val digits = max(2, groups.size.toString().length)
            val uris = mutableListOf<Uri>()
            var totalBytes = 0L
            val zipTemp = if (asZip) File(context.cacheDir, "split_${System.nanoTime()}.zip") else null
            val zip = zipTemp?.let { ZipOutputStream(it.outputStream().buffered()) }
            try {
                loadDocument(uri).use { src ->
                    if (src.isEncrypted) src.isAllSecurityToBeRemoved = true
                    groups.forEachIndexed { index, pages ->
                        coroutineContext.ensureActive()
                        onProgress(index + 1, groups.size)
                        val valid = pages.filter { it in 0 until src.numberOfPages }
                        if (valid.isEmpty()) return@forEachIndexed
                        val label = PageRangeLabel.of(valid)
                        val fileName = "${baseName}_${(index + 1).toString().padStart(digits, '0')}_p$label.pdf"
                        val temp = File(context.cacheDir, "part_${System.nanoTime()}.pdf")
                        try {
                            PDDocument(MemoryUsageSetting.setupTempFileOnly()).use { part ->
                                valid.forEach { part.importPage(src.getPage(it)) }
                                part.documentInformation.title = "$baseName ($label)"
                                part.documentInformation.producer = "Morph"
                                temp.outputStream().buffered().use { part.save(it) }
                            }
                            if (zip != null) {
                                zip.putNextEntry(ZipEntry(fileName))
                                temp.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            } else {
                                totalBytes += temp.length()
                                uris += OutputStorage.save(context, fileName, "application/pdf", OutputFolder.PDFS) { out ->
                                    temp.inputStream().use { it.copyTo(out) }
                                }
                            }
                        } finally {
                            temp.delete()
                        }
                    }
                }
                zip?.close()
                if (zipTemp != null) {
                    totalBytes = zipTemp.length()
                    val zipUri = OutputStorage.save(context, FileNames.stamped("${baseName}_diviso", "PDF", "zip"), "application/zip", OutputFolder.ARCHIVES) { out ->
                        zipTemp.inputStream().use { it.copyTo(out) }
                    }
                    SplitResult(emptyList(), zipUri, groups.size, totalBytes)
                } else {
                    SplitResult(uris, null, uris.size, totalBytes)
                }
            } finally {
                runCatching { zip?.close() }
                zipTemp?.delete()
            }
        }
    }

    private object PageRangeLabel {
        /** [0,1,2] -> "1-3", [4] -> "5", [0,2] -> "1_3". */
        fun of(pages: List<Int>): String {
            val first = pages.first() + 1
            val last = pages.last() + 1
            val contiguous = pages.zipWithNext().all { (a, b) -> b == a + 1 }
            return when {
                pages.size == 1 -> "$first"
                contiguous -> "$first-$last"
                else -> pages.take(4).joinToString("_") { "${it + 1}" } + if (pages.size > 4) "_ecc" else ""
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // PDF → images
    // ---------------------------------------------------------------------------------------------

    data class ImagesResult(val uris: List<Uri>, val zipUri: Uri?, val totalBytes: Long)

    suspend fun exportImages(
        uri: Uri,
        name: String,
        pageIndices: List<Int>,
        dpi: Int,
        format: PdfImageFormat,
        asZip: Boolean,
        onProgress: (Int, Int) -> Unit
    ): Result<ImagesResult> = withContext(Dispatchers.IO) {
        resultOf {
            if (pageIndices.isEmpty()) throw IllegalStateException("Seleziona almeno una pagina")
            val baseName = FileNames.sanitize(FileNames.baseName(name), "Pagina")
            val digits = max(2, pageIndices.max().plus(1).toString().length)
            val uris = mutableListOf<Uri>()
            var totalBytes = 0L
            val zipTemp = if (asZip) File(context.cacheDir, "pages_${System.nanoTime()}.zip") else null

            val zip = zipTemp?.let { ZipOutputStream(it.outputStream().buffered()) }
            try {
                renderMutex.withLock {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            pageIndices.forEachIndexed { i, pageIndex ->
                                coroutineContext.ensureActive()
                                onProgress(i + 1, pageIndices.size)
                                if (pageIndex !in 0 until renderer.pageCount) return@forEachIndexed
                                val bitmap = renderer.openPage(pageIndex).use { page ->
                                    var scale = dpi / 72f
                                    val longest = max(page.width, page.height) * scale
                                    if (longest > MAX_EXPORT_SIDE) scale *= MAX_EXPORT_SIDE / longest
                                    renderToBitmap(page, scale)
                                }
                                val fileName = "${baseName}_p${(pageIndex + 1).toString().padStart(digits, '0')}.${format.extension}"
                                val compressFormat = if (format == PdfImageFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                                if (zip != null) {
                                    zip.putNextEntry(ZipEntry(fileName))
                                    bitmap.compress(compressFormat, 92, zip)
                                    zip.closeEntry()
                                } else {
                                    val temp = File(context.cacheDir, "page_${System.nanoTime()}")
                                    try {
                                        temp.outputStream().buffered().use { bitmap.compress(compressFormat, 92, it) }
                                        totalBytes += temp.length()
                                        uris += OutputStorage.save(context, fileName, format.mimeType, OutputFolder.IMAGES) { out ->
                                            temp.inputStream().use { it.copyTo(out) }
                                        }
                                    } finally {
                                        temp.delete()
                                    }
                                }
                                bitmap.recycle()
                            }
                        }
                    } ?: throw IllegalStateException("Impossibile aprire il PDF")
                }
                zip?.close()

                if (zipTemp != null) {
                    totalBytes = zipTemp.length()
                    val zipUri = OutputStorage.save(context, FileNames.stamped("${baseName}_immagini", "Pagine", "zip"), "application/zip", OutputFolder.ARCHIVES) { out ->
                        zipTemp.inputStream().use { it.copyTo(out) }
                    }
                    ImagesResult(emptyList(), zipUri, totalBytes)
                } else {
                    ImagesResult(uris, null, totalBytes)
                }
            } catch (e: SecurityException) {
                throw IllegalStateException("Il PDF è protetto da password")
            } finally {
                runCatching { zip?.close() }
                zipTemp?.delete()
            }
        }
    }

    private fun loadDocument(uri: Uri): PDDocument {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Impossibile aprire il PDF")
        return try {
            stream.use { PDDocument.load(it, MemoryUsageSetting.setupTempFileOnly()) }
        } catch (e: InvalidPasswordException) {
            throw IllegalStateException("Il PDF è protetto da password: sbloccalo prima con «Proteggi PDF» › Sblocca")
        }
    }

    private companion object {
        const val MAX_EXPORT_SIDE = 5000f
        const val MAX_PREVIEW_SIDE = 4096f
    }
}

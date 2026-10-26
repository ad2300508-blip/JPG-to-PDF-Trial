package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.example.util.FileNames
import com.example.util.NotesSide
import com.example.util.NotesStyle
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.PageMath
import com.example.util.resultOf
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.common.PDStream
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.graphics.form.PDFormXObject
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

/**
 * Page-level PDF tools that keep text and drawings as vectors: pages are re-drawn as form
 * XObjects (N-up, notes), their crop box is changed (crop) or whole pages are moved (duplex,
 * blank pages). Only the blank-page scan renders pages, at a tiny size.
 */
class PdfPageToolsService(private val context: Context) {

    init {
        runCatching { PDFBoxResourceLoader.init(context) }
    }

    /** Ink share of every page (see [PageMath.inkRatio]), rendered ~100 px wide. */
    suspend fun scanInk(uri: Uri, onProgress: (Int, Int) -> Unit): Result<List<Float>> = withContext(Dispatchers.IO) {
        resultOf {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: throw IllegalStateException("Impossibile aprire il PDF")
            pfd.use {
                PdfRenderer(it).use { renderer ->
                    val total = renderer.pageCount
                    (0 until total).map { index ->
                        ensureActive()
                        onProgress(index + 1, total)
                        renderer.openPage(index).use { page ->
                            val width = SCAN_WIDTH
                            val height = (SCAN_WIDTH * page.height / page.width.toFloat()).roundToInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            try {
                                bitmap.eraseColor(Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val pixels = IntArray(width * height)
                                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                                PageMath.inkRatio(pixels, width, height)
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        }
    }

    /** Copy of the PDF without the pages in [remove] (0-based). */
    suspend fun removePages(uri: Uri, name: String, remove: Set<Int>): Result<Uri> = withContext(Dispatchers.IO) {
        resultOf {
            load(uri).use { doc ->
                require(remove.size < doc.numberOfPages) { "Resterebbe un PDF vuoto: tieni almeno una pagina" }
                remove.filter { it in 0 until doc.numberOfPages }.sortedDescending().forEach { doc.removePage(it) }
                save(doc, FileNames.stamped("${FileNames.baseName(name)}_senza_bianche", "Documento", "pdf"))
            }
        }
    }

    /** [perSheet] pages (2 or 4) on each A4 sheet, optionally with a thin frame around every page. */
    suspend fun nUp(uri: Uri, name: String, perSheet: Int, frames: Boolean, onProgress: (Int, Int) -> Unit): Result<Uri> =
        withContext(Dispatchers.IO) {
            resultOf {
                load(uri).use { source ->
                    PDDocument().use { out ->
                        val pages = source.pages.toList()
                        val (sheetW, sheetH) = PageMath.nUpSheet(perSheet)
                        val slots = PageMath.nUpSlots(perSheet, sheetW, sheetH, margin = 18f, gap = 12f)
                        pages.chunked(perSheet).forEachIndexed { sheetIndex, group ->
                            ensureActive()
                            onProgress(sheetIndex + 1, (pages.size + perSheet - 1) / perSheet)
                            val sheet = PDPage(PDRectangle(sheetW, sheetH))
                            out.addPage(sheet)
                            PDPageContentStream(out, sheet).use { cs ->
                                group.forEachIndexed { i, page ->
                                    val drawn = drawPage(out, cs, page, slots[i])
                                    if (frames) {
                                        cs.setStrokingColor(0.7f, 0.7f, 0.7f)
                                        cs.setLineWidth(0.6f)
                                        cs.addRect(drawn.x, drawn.y, drawn.w, drawn.h)
                                        cs.stroke()
                                    }
                                }
                            }
                        }
                        save(out, FileNames.stamped("${FileNames.baseName(name)}_${perSheet}_per_foglio", "Documento", "pdf"))
                    }
                }
            }
        }

    /** Each page shrunk on a sheet of its own size, with lines, squares or blank space for notes. */
    suspend fun notes(uri: Uri, name: String, side: NotesSide, style: NotesStyle, onProgress: (Int, Int) -> Unit): Result<Uri> =
        withContext(Dispatchers.IO) {
            resultOf {
                load(uri).use { source ->
                    PDDocument().use { out ->
                        val pages = source.pages.toList()
                        pages.forEachIndexed { index, page ->
                            ensureActive()
                            onProgress(index + 1, pages.size)
                            val box = page.cropBox
                            val (sheetW, sheetH) = PageMath.displayedSize(page.rotation, box.width, box.height)
                            val sheet = PDPage(PDRectangle(sheetW, sheetH))
                            out.addPage(sheet)
                            val margin = minOf(sheetW, sheetH) * 0.04f
                            val (pageArea, notesArea) = PageMath.notesLayout(sheetW, sheetH, side, margin)
                            PDPageContentStream(out, sheet).use { cs ->
                                val drawn = drawPage(out, cs, page, pageArea)
                                cs.setStrokingColor(0.8f, 0.8f, 0.8f)
                                cs.setLineWidth(0.5f)
                                cs.addRect(drawn.x, drawn.y, drawn.w, drawn.h)
                                cs.stroke()
                                drawNotesArea(cs, notesArea, style)
                            }
                        }
                        save(out, FileNames.stamped("${FileNames.baseName(name)}_appunti", "Documento", "pdf"))
                    }
                }
            }
        }

    /**
     * Interleaves fronts and backs scanned separately. With [backs] null, [fronts] holds all the
     * fronts first and then all the backs (one scan of the whole stack, turned over).
     */
    suspend fun duplex(fronts: Uri, backs: Uri?, name: String, backsReversed: Boolean, onProgress: (Int, Int) -> Unit): Result<Uri> =
        withContext(Dispatchers.IO) {
            resultOf {
                val first = load(fronts)
                val second = backs?.let { load(it) }
                try {
                    val firstPages = first.pages.toList()
                    val (frontPages, backPages) = if (second == null) {
                        val half = (firstPages.size + 1) / 2
                        firstPages.take(half) to firstPages.drop(half)
                    } else {
                        firstPages to second.pages.toList()
                    }
                    require(frontPages.isNotEmpty() && backPages.isNotEmpty()) { "Servono pagine sia di fronte sia di retro" }
                    PDDocument().use { out ->
                        val order = PageMath.duplexOrder(frontPages.size, backPages.size, backsReversed)
                        order.forEachIndexed { i, (side, index) ->
                            ensureActive()
                            onProgress(i + 1, order.size)
                            out.importPage(if (side == 0) frontPages[index] else backPages[index])
                        }
                        save(out, FileNames.stamped("${FileNames.baseName(name)}_fronte_retro", "Documento", "pdf"))
                    }
                } finally {
                    runCatching { second?.close() }
                    runCatching { first.close() }
                }
            }
        }

    /** Crops every page by [margins] (left, top, right, bottom as fractions of the page as shown). */
    suspend fun crop(uri: Uri, name: String, margins: FloatArray, onProgress: (Int, Int) -> Unit): Result<Uri> = withContext(Dispatchers.IO) {
        resultOf {
            require(margins[0] + margins[2] < 0.95f && margins[1] + margins[3] < 0.95f) { "Margini troppo grandi" }
            load(uri).use { doc ->
                val pages = doc.pages.toList()
                pages.forEachIndexed { index, page ->
                    ensureActive()
                    onProgress(index + 1, pages.size)
                    val box = page.cropBox
                    val (l, b, r, t) = PageMath.cropInBoxSpace(page.rotation, margins[0], margins[1], margins[2], margins[3]).toList()
                    page.cropBox = PDRectangle(
                        box.lowerLeftX + box.width * l,
                        box.lowerLeftY + box.height * b,
                        box.width * (1f - l - r),
                        box.height * (1f - b - t)
                    )
                }
                save(doc, FileNames.stamped("${FileNames.baseName(name)}_ritagliato", "Documento", "pdf"))
            }
        }
    }

    /** Draws [page] (rotation and crop box respected) fitted into [slot]; returns where it landed. */
    private fun drawPage(out: PDDocument, cs: PDPageContentStream, page: PDPage, slot: PageMath.Box): PageMath.Box {
        val box = page.cropBox
        val (dw, dh) = PageMath.displayedSize(page.rotation, box.width, box.height)
        val target = PageMath.fit(dw, dh, slot)
        val form = PDFormXObject(PDStream(out, page.contents, COSName.FLATE_DECODE))
        form.resources = page.resources
        form.bBox = PDRectangle(box.lowerLeftX, box.lowerLeftY, box.width, box.height)
        val m = PageMath.placement(page.rotation, box.lowerLeftX, box.lowerLeftY, box.width, box.height, target)
        cs.saveGraphicsState()
        cs.transform(Matrix(m[0], m[1], m[2], m[3], m[4], m[5]))
        cs.drawForm(form)
        cs.restoreGraphicsState()
        return target
    }

    private fun drawNotesArea(cs: PDPageContentStream, area: PageMath.Box, style: NotesStyle) {
        if (area.w <= 0f || area.h <= 0f) return
        cs.setStrokingColor(0.78f, 0.8f, 0.85f)
        cs.setLineWidth(0.4f)
        when (style) {
            NotesStyle.LINES -> {
                var y = area.y + area.h - LINE_GAP
                while (y > area.y) {
                    cs.moveTo(area.x, y)
                    cs.lineTo(area.x + area.w, y)
                    y -= LINE_GAP
                }
                cs.stroke()
            }
            NotesStyle.GRID -> {
                var y = area.y + area.h
                while (y >= area.y) {
                    cs.moveTo(area.x, y)
                    cs.lineTo(area.x + area.w, y)
                    y -= GRID_GAP
                }
                var x = area.x
                while (x <= area.x + area.w) {
                    cs.moveTo(x, area.y)
                    cs.lineTo(x, area.y + area.h)
                    x += GRID_GAP
                }
                cs.stroke()
            }
            NotesStyle.BLANK -> Unit
        }
        cs.setStrokingColor(0.7f, 0.7f, 0.7f)
        cs.setLineWidth(0.6f)
        cs.addRect(area.x, area.y, area.w, area.h)
        cs.stroke()
    }

    private suspend fun save(doc: PDDocument, fileName: String): Uri {
        val temp = File(context.cacheDir, "pages_${System.nanoTime()}.pdf")
        try {
            temp.outputStream().buffered().use { doc.save(it) }
            return OutputStorage.save(context, fileName, "application/pdf", OutputFolder.PDFS) { out ->
                temp.inputStream().use { it.copyTo(out) }
            }
        } finally {
            temp.delete()
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

    private companion object {
        const val SCAN_WIDTH = 100
        const val LINE_GAP = 20f
        const val GRID_GAP = 14f
    }
}

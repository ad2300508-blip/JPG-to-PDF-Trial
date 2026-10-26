package com.example.service

import android.content.Context
import android.net.Uri
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.resultOf
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.io.MemoryUsageSetting
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.util.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.coroutineContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class WatermarkPosition(val label: String) {
    DIAGONAL("Diagonale"),
    CENTER("Centro"),
    TILED("Ripetuta"),
    TOP("In alto")
}

enum class StampColor(val label: String, val r: Float, val g: Float, val b: Float) {
    GRAY("Grigio", 0.45f, 0.45f, 0.45f),
    RED("Rosso", 0.86f, 0.15f, 0.15f),
    BLUE("Blu", 0.15f, 0.35f, 0.85f),
    BLACK("Nero", 0f, 0f, 0f)
}

enum class PageNumberStyle(val label: String) {
    NONE("Nessuno"),
    SIMPLE("1"),
    OF_TOTAL("1 / 10"),
    VERBOSE("Pagina 1 di 10");

    fun format(number: Int, total: Int): String = when (this) {
        NONE -> ""
        SIMPLE -> "$number"
        OF_TOTAL -> "$number / $total"
        VERBOSE -> "Pagina $number di $total"
    }
}

enum class PageNumberPosition(val label: String) {
    BOTTOM_CENTER("In basso al centro"),
    BOTTOM_RIGHT("In basso a destra"),
    TOP_RIGHT("In alto a destra")
}

data class StampConfig(
    val text: String = "",
    val opacity: Float = 0.25f,
    val fontSize: Float = 54f,
    val position: WatermarkPosition = WatermarkPosition.DIAGONAL,
    val color: StampColor = StampColor.GRAY,
    val numberStyle: PageNumberStyle = PageNumberStyle.NONE,
    val numberPosition: PageNumberPosition = PageNumberPosition.BOTTOM_CENTER,
    val startNumber: Int = 1,
    val skipFirstPage: Boolean = false
) {
    val hasWatermark: Boolean get() = text.isNotBlank()
    val hasNumbers: Boolean get() = numberStyle != PageNumberStyle.NONE
    val isEmpty: Boolean get() = !hasWatermark && !hasNumbers
}

/** What "Proteggi PDF" knows about a document before touching it. */
data class PdfSecurityInfo(val needsPassword: Boolean, val isEncrypted: Boolean, val pageCount: Int)

data class ProtectOptions(
    val password: String,
    val allowPrinting: Boolean = true,
    val allowCopying: Boolean = false,
    val allowEditing: Boolean = false
)

/** Watermarks, page numbers and passwords on existing PDFs. */
class PdfStampService(private val context: Context) {

    init {
        runCatching { PDFBoxResourceLoader.init(context) }
    }

    // ---------------------------------------------------------------------------------------------
    // Watermark & page numbers
    // ---------------------------------------------------------------------------------------------

    suspend fun stamp(uri: Uri, name: String, config: StampConfig, onProgress: (Int, Int) -> Unit): Result<Uri> =
        withContext(Dispatchers.IO) {
            resultOf {
                if (config.isEmpty) throw IllegalStateException("Scrivi una filigrana o scegli uno stile di numerazione")
                loadDocument(uri, null).use { doc ->
                    if (doc.isEncrypted) doc.isAllSecurityToBeRemoved = true
                    val font = PDType1Font.HELVETICA_BOLD
                    val numberFont = PDType1Font.HELVETICA
                    val watermark = encodable(font, config.text.trim())
                    if (config.text.isNotBlank() && watermark.isBlank()) {
                        throw IllegalStateException("La filigrana contiene solo caratteri non supportati (emoji, simboli)")
                    }
                    val total = doc.numberOfPages
                    val numbered = if (config.skipFirstPage) total - 1 else total
                    val lastNumber = config.startNumber + numbered - 1

                    doc.pages.forEachIndexed { index, page ->
                        coroutineContext.ensureActive()
                        onProgress(index + 1, total)
                        PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                            val (width, height) = uprightFrame(cs, page)
                            if (watermark.isNotBlank()) drawWatermark(cs, font, watermark, config, width, height)
                            val showNumber = config.hasNumbers && !(config.skipFirstPage && index == 0)
                            if (showNumber) {
                                val number = config.startNumber + index - if (config.skipFirstPage) 1 else 0
                                drawPageNumber(cs, numberFont, config.numberStyle.format(number, lastNumber), config, width, height)
                            }
                        }
                    }
                    doc.documentInformation.producer = "Morph"
                    val suffix = if (watermark.isNotBlank()) "filigrana" else "numerato"
                    save(doc, "${FileNames.sanitize(FileNames.baseName(name), "Documento")}_$suffix.pdf")
                }
            }
        }

    /**
     * Draws the hand-written [signature] (transparent PNG-like bitmap) on the chosen pages, centred at
     * the placement's fractions of the visible page; rotated pages are handled like the stamp.
     */
    suspend fun sign(
        uri: Uri,
        name: String,
        signature: android.graphics.Bitmap,
        pages: SignPages,
        placement: SignPlacement,
        onProgress: (Int, Int) -> Unit
    ): Result<Uri> = withContext(Dispatchers.IO) {
        resultOf {
            loadDocument(uri, null).use { doc ->
                if (doc.isEncrypted) doc.isAllSecurityToBeRemoved = true
                val image = LosslessFactory.createFromImage(doc, signature)
                val targets = pages.indices(doc.numberOfPages)
                targets.forEachIndexed { i, index ->
                    coroutineContext.ensureActive()
                    onProgress(i + 1, targets.size)
                    val page = doc.getPage(index)
                    PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                        val (width, height) = uprightFrame(cs, page)
                        val rect = placement.rectOn(width, height, signature.width.toFloat() / signature.height)
                        cs.drawImage(image, rect[0], rect[1], rect[2], rect[3])
                    }
                }
                doc.documentInformation.producer = "Morph"
                save(doc, "${FileNames.sanitize(FileNames.baseName(name), "Documento")}_firmato.pdf")
            }
        }
    }

    /**
     * Rotated pages (/Rotate 90, 180, 270) are drawn in unrotated user space. This applies the inverse
     * transform so everything below can draw upright, and returns the visual width and height.
     */
    private fun uprightFrame(cs: PDPageContentStream, page: PDPage): Pair<Float, Float> {
        val box = page.cropBox
        val w = box.width
        val h = box.height
        val x0 = box.lowerLeftX
        val y0 = box.lowerLeftY
        return when ((page.rotation % 360 + 360) % 360) {
            90 -> {
                cs.transform(Matrix(0f, 1f, -1f, 0f, x0 + w, y0))
                h to w
            }
            180 -> {
                cs.transform(Matrix(-1f, 0f, 0f, -1f, x0 + w, y0 + h))
                w to h
            }
            270 -> {
                cs.transform(Matrix(0f, -1f, 1f, 0f, x0, y0 + h))
                h to w
            }
            else -> {
                if (x0 != 0f || y0 != 0f) cs.transform(Matrix.getTranslateInstance(x0, y0))
                w to h
            }
        }
    }

    private fun drawWatermark(cs: PDPageContentStream, font: PDFont, text: String, config: StampConfig, width: Float, height: Float) {
        var size = config.fontSize
        var textWidth = font.getStringWidth(text) / 1000f * size
        // Keep long watermarks inside the page.
        val maxWidth = if (config.position == WatermarkPosition.DIAGONAL) kotlin.math.hypot(width, height) * 0.8f else width * 0.85f
        if (config.position != WatermarkPosition.TILED && textWidth > maxWidth) {
            size *= maxWidth / textWidth
            textWidth = maxWidth
        }

        cs.saveGraphicsState()
        val state = PDExtendedGraphicsState().apply {
            nonStrokingAlphaConstant = config.opacity.coerceIn(0.05f, 1f)
            strokingAlphaConstant = config.opacity.coerceIn(0.05f, 1f)
        }
        cs.setGraphicsStateParameters(state)
        cs.setNonStrokingColor(config.color.r, config.color.g, config.color.b)
        cs.setFont(font, size)

        fun place(cx: Float, cy: Float, angle: Double) {
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()
            // Start point so the text's centre lands on (cx, cy).
            val tx = cx - c * textWidth / 2f + s * size / 3f
            val ty = cy - s * textWidth / 2f - c * size / 3f
            cs.beginText()
            cs.setTextMatrix(Matrix.getRotateInstance(angle, tx, ty))
            cs.showText(text)
            cs.endText()
        }

        when (config.position) {
            WatermarkPosition.DIAGONAL -> place(width / 2f, height / 2f, atan2(height.toDouble(), width.toDouble()))
            WatermarkPosition.CENTER -> place(width / 2f, height / 2f, 0.0)
            WatermarkPosition.TOP -> place(width / 2f, height - size * 1.2f, 0.0)
            WatermarkPosition.TILED -> {
                val stepX = (textWidth + size * 2f).coerceAtLeast(80f)
                val stepY = (size * 4f).coerceAtLeast(60f)
                var row = 0
                var y = stepY / 2f
                while (y < height + stepY) {
                    var x = if (row % 2 == 0) stepX / 2f else 0f
                    while (x < width + stepX) {
                        place(x, y, Math.toRadians(30.0))
                        x += stepX
                    }
                    y += stepY
                    row++
                }
            }
        }
        cs.restoreGraphicsState()
    }

    private fun drawPageNumber(cs: PDPageContentStream, font: PDFont, text: String, config: StampConfig, width: Float, height: Float) {
        val size = 10f
        val margin = 22f
        val textWidth = font.getStringWidth(text) / 1000f * size
        val (x, y) = when (config.numberPosition) {
            PageNumberPosition.BOTTOM_CENTER -> (width - textWidth) / 2f to margin
            PageNumberPosition.BOTTOM_RIGHT -> width - margin - textWidth to margin
            PageNumberPosition.TOP_RIGHT -> width - margin - textWidth to height - margin - size
        }
        cs.beginText()
        cs.setFont(font, size)
        cs.setNonStrokingColor(0.35f, 0.35f, 0.35f)
        cs.newLineAtOffset(x, y)
        cs.showText(text)
        cs.endText()
    }

    /** Standard 14 fonts only cover WinAnsi: drop characters (emoji, CJK…) that would make showText throw. */
    private fun encodable(font: PDFont, text: String): String = buildString {
        for (ch in text) {
            if (runCatching { font.encode(ch.toString()) }.isSuccess) append(ch)
        }
    }.trim()

    // ---------------------------------------------------------------------------------------------
    // Passwords
    // ---------------------------------------------------------------------------------------------

    suspend fun inspect(uri: Uri): Result<PdfSecurityInfo> = withContext(Dispatchers.IO) {
        resultOf {
            try {
                loadDocument(uri, null, rawPasswordErrors = true).use { doc ->
                    PdfSecurityInfo(needsPassword = false, isEncrypted = doc.isEncrypted, pageCount = doc.numberOfPages)
                }
            } catch (e: InvalidPasswordException) {
                PdfSecurityInfo(needsPassword = true, isEncrypted = true, pageCount = 0)
            }
        }
    }

    suspend fun protect(uri: Uri, name: String, currentPassword: String?, options: ProtectOptions): Result<Uri> =
        withContext(Dispatchers.IO) {
            resultOf {
                if (options.password.length < 4) throw IllegalStateException("La password deve avere almeno 4 caratteri")
                loadDocument(uri, currentPassword).use { doc ->
                    if (doc.isEncrypted) doc.isAllSecurityToBeRemoved = true
                    val permissions = AccessPermission().apply {
                        setCanPrint(options.allowPrinting)
                        setCanExtractContent(options.allowCopying)
                        setCanExtractForAccessibility(true)
                        setCanModify(options.allowEditing)
                        setCanModifyAnnotations(options.allowEditing)
                        setCanFillInForm(true)
                        setCanAssembleDocument(options.allowEditing)
                    }
                    // A random owner password: with the user password the document opens, but the
                    // restrictions above cannot be lifted by a regular viewer.
                    val policy = StandardProtectionPolicy(UUID.randomUUID().toString(), options.password, permissions)
                    policy.encryptionKeyLength = 128
                    doc.protect(policy)
                    save(doc, "${FileNames.sanitize(FileNames.baseName(name), "Documento")}_protetto.pdf")
                }
            }
        }

    suspend fun unlock(uri: Uri, name: String, password: String?): Result<Uri> = withContext(Dispatchers.IO) {
        resultOf {
            loadDocument(uri, password).use { doc ->
                if (!doc.isEncrypted) throw IllegalStateException("Questo PDF non è protetto: non c'è niente da sbloccare")
                doc.isAllSecurityToBeRemoved = true
                save(doc, "${FileNames.sanitize(FileNames.baseName(name), "Documento")}_sbloccato.pdf")
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    private suspend fun save(doc: PDDocument, fileName: String): Uri {
        val temp = File(context.cacheDir, "stamp_${System.nanoTime()}.pdf")
        try {
            temp.outputStream().buffered().use { doc.save(it) }
            return OutputStorage.save(context, fileName, "application/pdf", OutputFolder.PDFS) { out ->
                temp.inputStream().use { it.copyTo(out) }
            }
        } finally {
            temp.delete()
        }
    }

    private fun loadDocument(uri: Uri, password: String?, rawPasswordErrors: Boolean = false): PDDocument {
        PDFBoxResourceLoader.init(context)
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Impossibile aprire il PDF")
        return try {
            stream.use { PDDocument.load(it, password.orEmpty(), MemoryUsageSetting.setupTempFileOnly()) }
        } catch (e: InvalidPasswordException) {
            if (rawPasswordErrors) throw e
            throw IllegalStateException(if (password.isNullOrEmpty()) "Il PDF è protetto: inserisci la password" else "Password errata")
        }
    }
}

/** Which pages receive the signature. */
enum class SignPages(val label: String) {
    LAST("Ultima"),
    FIRST("Prima"),
    ALL("Tutte");

    fun indices(total: Int): List<Int> = when {
        total <= 0 -> emptyList()
        this == LAST -> listOf(total - 1)
        this == FIRST -> listOf(0)
        else -> (0 until total).toList()
    }
}

/** Signature position as fractions of the visible page (centre, from the top-left) and its width. */
data class SignPlacement(val centerX: Float = 0.72f, val centerY: Float = 0.86f, val widthFraction: Float = 0.32f) {
    /** x, y (bottom-left, PDF points), width, height of the signature, kept inside the page. */
    fun rectOn(pageWidth: Float, pageHeight: Float, aspect: Float): FloatArray {
        val w = (widthFraction * pageWidth).coerceAtMost(pageWidth)
        val h = (w / aspect.coerceAtLeast(0.05f)).coerceAtMost(pageHeight)
        val x = (centerX * pageWidth - w / 2f).coerceIn(0f, pageWidth - w)
        val yTop = (centerY * pageHeight - h / 2f).coerceIn(0f, pageHeight - h)
        return floatArrayOf(x, pageHeight - yTop - h, w, h)
    }
}

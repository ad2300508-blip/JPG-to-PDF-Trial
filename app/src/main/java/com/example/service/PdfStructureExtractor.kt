package com.example.service

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.abs
import kotlin.math.roundToInt

/** How page boundaries appear in the Markdown. */
enum class PageMarkerStyle(val label: String) {
    COMMENT("Nascosti"),
    HEADING("Titoli"),
    NONE("Nessuno")
}

data class PdfMarkdownOptions(
    val pageMarkers: PageMarkerStyle = PageMarkerStyle.COMMENT,
    /** Drop running headers/footers and bare page numbers. */
    val cleanMargins: Boolean = true
)

/** One word with the style of its first glyph. */
internal data class StyledWord(val text: String, val bold: Boolean, val italic: Boolean, val startX: Float, val endX: Float)

/** One visual line of a PDF page, in top-down coordinates. */
internal data class PdfLine(
    val words: List<StyledWord>,
    val x: Float,
    val y: Float,
    val endX: Float,
    val fontSize: Float,
    val height: Float
) {
    val text: String get() = words.joinToString(" ") { it.text }
    val isAllBold: Boolean get() = words.isNotEmpty() && words.all { it.bold }
    val charCount: Int get() = words.sumOf { it.text.length }

    /** Text runs separated by wide gaps (table cells / columns). */
    fun segments(): List<String> {
        if (words.isEmpty()) return emptyList()
        val gapLimit = fontSize * 2.2f
        val result = mutableListOf<StringBuilder>()
        var current = StringBuilder(words.first().text)
        for (i in 1 until words.size) {
            val gap = words[i].startX - words[i - 1].endX
            if (gap > gapLimit) {
                result += current
                current = StringBuilder(words[i].text)
            } else {
                current.append(' ').append(words[i].text)
            }
        }
        result += current
        return result.map { it.toString().trim() }
    }
}

/** Collects styled lines instead of plain text: PDFTextStripper calls writeString once per word. */
internal class LineCollectingStripper : PDFTextStripper() {

    private val lines = mutableListOf<PdfLine>()
    private val words = mutableListOf<StyledWord>()
    private var lineX = Float.MAX_VALUE
    private var lineY = 0f
    private var lineEnd = 0f
    private var sizeSum = 0f
    private var heightMax = 0f
    private var glyphs = 0

    init {
        sortByPosition = true
        suppressDuplicateOverlappingText = true
    }

    fun collect(document: PDDocument, pageNumber: Int): List<PdfLine> {
        lines.clear()
        resetLine()
        startPage = pageNumber
        endPage = pageNumber
        getText(document)
        flushLine()
        return lines.toList()
    }

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        if (textPositions.isEmpty() || text.isBlank()) return
        val first = textPositions.first()
        val last = textPositions.last()
        val font = first.font
        words += StyledWord(
            text = text.trim(),
            bold = isBold(font, textPositions),
            italic = isItalic(font),
            startX = first.xDirAdj,
            endX = last.xDirAdj + last.widthDirAdj
        )
        lineX = minOf(lineX, first.xDirAdj)
        lineEnd = maxOf(lineEnd, last.xDirAdj + last.widthDirAdj)
        lineY = maxOf(lineY, first.yDirAdj)
        for (p in textPositions) {
            sizeSum += p.fontSizeInPt.takeIf { it > 0f } ?: p.heightDir
            heightMax = maxOf(heightMax, p.heightDir)
            glyphs++
        }
    }

    override fun writeWordSeparator() = Unit
    override fun writeLineSeparator() = flushLine()

    private fun flushLine() {
        if (words.isNotEmpty() && glyphs > 0) {
            val size = sizeSum / glyphs
            lines += PdfLine(words.toList(), lineX, lineY, lineEnd, size, heightMax.takeIf { it > 0f } ?: size)
        }
        resetLine()
    }

    private fun resetLine() {
        words.clear()
        lineX = Float.MAX_VALUE
        lineY = 0f
        lineEnd = 0f
        sizeSum = 0f
        heightMax = 0f
        glyphs = 0
    }

    private fun isBold(font: PDFont?, positions: List<TextPosition>): Boolean {
        if (font == null) return false
        val name = font.name.orEmpty().lowercase()
        if (BOLD_NAMES.any { name.contains(it) }) return true
        val descriptor = font.fontDescriptor ?: return false
        return descriptor.isForceBold || descriptor.fontWeight >= 600f ||
            // Some generators fake bold by stroking: fall back to the stem width when present.
            (descriptor.stemV >= 130f && positions.isNotEmpty())
    }

    private fun isItalic(font: PDFont?): Boolean {
        if (font == null) return false
        val name = font.name.orEmpty().lowercase()
        if (name.contains("italic") || name.contains("oblique")) return true
        val descriptor = font.fontDescriptor ?: return false
        return descriptor.isItalic || abs(descriptor.italicAngle) > 5f
    }

    private companion object {
        val BOLD_NAMES = listOf("bold", "black", "heavy", "semibold", "demi", ",b")
    }
}

/** Turns styled PDF lines into Markdown: headings, lists, simple tables, bold/italic, clean paragraphs. */
internal object PdfMarkdownBuilder {

    private val symbolBullet = Regex("^([•●○◦▪▫■□►▶‣∙·✓✔➢➤])\\s*(.+)")
    private val dashBullet = Regex("^([\\-–—*])\\s+(.+)")
    private val orderedRegex = Regex("^(\\d{1,3})[.)]\\s+(.+)")
    private val letterRegex = Regex("^([a-zA-Z])\\)\\s+(.+)")
    private val pageNumberRegex = Regex("^(pag\\.?|pagina|page|p\\.)?\\s*[-–]?\\s*\\d{1,4}\\s*[-–]?(\\s*(/|di|of|su)\\s*\\d{1,4})?$", RegexOption.IGNORE_CASE)
    private val sentenceEnd = charArrayOf('.', '!', '?', ':', ';')

    /** Most common font size weighted by characters: the body text size. */
    fun bodySize(pages: List<List<PdfLine>>): Float {
        val histogram = HashMap<Int, Int>()
        pages.flatten().forEach { line ->
            val key = (line.fontSize * 2).roundToInt()
            histogram[key] = (histogram[key] ?: 0) + line.charCount
        }
        val best = histogram.maxByOrNull { it.value }?.key ?: return 11f
        return (best / 2f).coerceAtLeast(4f)
    }

    private fun marginKey(line: PdfLine): String =
        line.text.lowercase().replace(Regex("\\d+"), "#").replace(Regex("\\s+"), " ").trim()

    /** Lines repeated at the top or bottom of at least half the pages (running headers and footers). */
    fun repeatedMargins(pages: List<List<PdfLine>>): Set<String> {
        if (pages.size < 3) return emptySet()
        val counts = HashMap<String, Int>()
        pages.forEach { lines ->
            val sorted = lines.sortedBy { it.y }
            (sorted.take(2) + sorted.takeLast(2)).map { marginKey(it) }.filter { it.length in 1..120 }.toSet()
                .forEach { counts[it] = (counts[it] ?: 0) + 1 }
        }
        val threshold = maxOf(2, (pages.size * 0.5f).roundToInt())
        return counts.filter { it.value >= threshold }.keys
    }

    fun render(lines: List<PdfLine>, body: Float, repeated: Set<String>, options: PdfMarkdownOptions): String {
        if (lines.isEmpty()) return ""
        val sorted = lines.sortedWith(compareBy({ it.y }, { it.x }))
        val edge = sorted.take(2).toSet() + sorted.takeLast(2).toSet()
        val content = if (!options.cleanMargins) sorted else sorted.filterNot { line ->
            line in edge && (marginKey(line) in repeated || pageNumberRegex.matches(line.text.trim()))
        }
        if (content.isEmpty()) return ""
        val leftMargin = content.minOf { it.x }

        val out = StringBuilder()
        val paragraph = StringBuilder()
        var paragraphKind = Kind.NONE
        var previous: PdfLine? = null
        var listIndentX = 0f
        var i = 0

        fun flush() {
            if (paragraph.isNotBlank()) out.append(paragraph.toString().trimEnd()).append("\n\n")
            paragraph.clear()
            paragraphKind = Kind.NONE
        }

        while (i < content.size) {
            val line = content[i]
            val text = line.text.trim()
            val prev = previous
            val gap = if (prev != null) line.y - prev.y else Float.MAX_VALUE
            val closeToPrevious = prev != null && gap <= maxOf(prev.height, line.height) * 1.9f && gap > 0f
            val sameSize = prev != null && abs(line.fontSize - prev.fontSize) <= body * 0.12f

            // Tables: consecutive lines split into the same number (≥3) of wide-gap segments.
            val segments = line.segments()
            if (segments.size >= 3) {
                val rows = mutableListOf(segments)
                var j = i + 1
                while (j < content.size && content[j].segments().size == segments.size) {
                    rows += content[j].segments()
                    j++
                }
                if (rows.size >= 2) {
                    flush()
                    out.append(markdownTable(rows)).append('\n')
                    previous = content[j - 1]
                    i = j
                    continue
                }
            }

            val level = headingLevel(line, body, text)
            if (level > 0) {
                flush()
                // Multi-line headings: merge following lines with the same style.
                val heading = StringBuilder(text)
                var j = i + 1
                while (j < content.size) {
                    val next = content[j]
                    val nextGap = next.y - content[j - 1].y
                    if (headingLevel(next, body, next.text.trim()) == level && abs(next.fontSize - line.fontSize) < 0.6f &&
                        nextGap <= line.height * 1.8f && heading.length < 160
                    ) {
                        heading.append(' ').append(next.text.trim())
                        j++
                    } else break
                }
                out.append("#".repeat(level)).append(' ').append(heading.toString().trim().trimEnd(':')).append("\n\n")
                previous = content[j - 1]
                i = j
                continue
            }

            val bullet = symbolBullet.find(text) ?: dashBullet.find(text)
            val ordered = orderedRegex.find(text)
            val lettered = letterRegex.find(text)
            val isListStart = (bullet != null && (text.length > 2)) || ordered != null || lettered != null
            if (isListStart) {
                if (paragraphKind != Kind.LIST) flush() else paragraph.append('\n')
                paragraphKind = Kind.LIST
                val depth = ((line.x - leftMargin) / 16f).roundToInt().coerceIn(0, 3)
                paragraph.append("  ".repeat(depth))
                when {
                    ordered != null -> paragraph.append(ordered.groupValues[1]).append(". ").append(styled(line, skipPrefix = ordered.groupValues[1].length + 1))
                    lettered != null -> paragraph.append("- ").append(lettered.groupValues[1]).append(") ").append(styled(line, skipPrefix = 2))
                    else -> paragraph.append("- ").append(styled(line, skipPrefix = bullet!!.groupValues[1].length))
                }
                listIndentX = line.x
                previous = line
                i++
                continue
            }

            val continuesList = paragraphKind == Kind.LIST && closeToPrevious && line.x > listIndentX + 2f
            val continuesParagraph = paragraphKind == Kind.TEXT && closeToPrevious && sameSize &&
                !(prev != null && prev.endX < pageRightEstimate(content) * 0.72f && prev.text.trimEnd().lastOrNull() in sentenceEnd.toList())

            if (continuesList || continuesParagraph) {
                appendJoined(paragraph, styled(line))
            } else {
                flush()
                paragraphKind = Kind.TEXT
                paragraph.append(styled(line))
            }
            previous = line
            i++
        }
        flush()
        return out.toString().replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private enum class Kind { NONE, TEXT, LIST }

    private fun pageRightEstimate(lines: List<PdfLine>): Float = lines.maxOf { it.endX }

    private fun headingLevel(line: PdfLine, body: Float, text: String): Int {
        if (text.length < 2 || text.length > 200 || text.none { it.isLetter() }) return 0
        val ratio = line.fontSize / body
        return when {
            ratio >= 1.6f -> 2
            ratio >= 1.25f -> 3
            line.isAllBold && ratio >= 0.95f && text.length <= 90 && line.words.size <= 14 &&
                text.last() !in charArrayOf('.', ',', ';') && !symbolBullet.matches(text) && !dashBullet.matches(text) -> 4
            else -> 0
        }
    }

    /** Joins a wrapped line, merging words hyphenated across the break. */
    private fun appendJoined(sb: StringBuilder, next: String) {
        if (sb.endsWith("-") && next.firstOrNull()?.isLowerCase() == true && sb.length > 1 && sb[sb.length - 2].isLetter()) {
            sb.setLength(sb.length - 1)
            sb.append(next)
        } else {
            sb.append(' ').append(next)
        }
    }

    /** Line text with **bold** and *italic* runs; [skipPrefix] drops a list marker. */
    private fun styled(line: PdfLine, skipPrefix: Int = 0): String {
        val words = line.words.toMutableList()
        if (skipPrefix > 0 && words.isNotEmpty()) {
            val first = words.first()
            val rest = first.text.drop(skipPrefix).trim()
            if (rest.isEmpty()) words.removeAt(0) else words[0] = first.copy(text = rest)
        }
        if (words.isEmpty()) return ""
        // A fully bold or italic line inside a paragraph is usually emphasis worth keeping.
        val sb = StringBuilder()
        var i = 0
        while (i < words.size) {
            val style = words[i].bold to words[i].italic
            var j = i
            val run = StringBuilder()
            while (j < words.size && (words[j].bold to words[j].italic) == style) {
                if (run.isNotEmpty()) run.append(' ')
                run.append(words[j].text)
                j++
            }
            val marker = when {
                style.first && style.second -> "***"
                style.first -> "**"
                style.second -> "*"
                else -> ""
            }
            if (sb.isNotEmpty()) sb.append(' ')
            val textRun = run.toString()
            if (marker.isNotEmpty() && textRun.any { it.isLetterOrDigit() }) sb.append(marker).append(textRun).append(marker) else sb.append(textRun)
            i = j
        }
        return sb.toString()
    }

    private fun markdownTable(rows: List<List<String>>): String {
        val columns = rows.maxOf { it.size }
        fun row(cells: List<String>) = "| " + (0 until columns).joinToString(" | ") { cells.getOrElse(it) { "" }.replace("|", "\\|") } + " |"
        val sb = StringBuilder()
        sb.append(row(rows.first())).append('\n')
        sb.append("|").append(" --- |".repeat(columns)).append('\n')
        rows.drop(1).forEach { sb.append(row(it)).append('\n') }
        return sb.toString()
    }
}

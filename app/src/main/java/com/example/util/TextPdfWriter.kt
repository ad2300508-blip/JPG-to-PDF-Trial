package com.example.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import java.io.OutputStream

/**
 * Turns Markdown (headings, lists, bold/italic, code, quotes) into a clean, paginated A4 PDF.
 * Uses Android's text engine so accents and any Unicode character render correctly.
 */
object TextPdfWriter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 50f
    private const val HEADER_SPACE = 26f
    private const val FOOTER_SPACE = 28f

    private enum class BlockType { H1, H2, H3, PARAGRAPH, BULLET, QUOTE, CODE, RULE }

    private data class Block(val type: BlockType, val text: String)

    fun write(markdown: String, title: String, out: OutputStream) {
        val document = PdfDocument()
        val contentWidth = (PAGE_WIDTH - MARGIN * 2).toInt()
        val bottom = PAGE_HEIGHT - MARGIN - FOOTER_SPACE

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 8.5f }
        val rulePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.7f }

        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f

        fun finishPage() {
            val current = page ?: return
            val c = current.canvas
            c.drawLine(MARGIN, PAGE_HEIGHT - MARGIN - 12f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN - 12f, rulePaint)
            val label = "$pageNumber"
            c.drawText(label, (PAGE_WIDTH - headerPaint.measureText(label)) / 2f, PAGE_HEIGHT - MARGIN + 4f, headerPaint)
            document.finishPage(current)
            page = null
        }

        fun newPage() {
            finishPage()
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val p = document.startPage(info)
            page = p
            canvas = p.canvas
            p.canvas.drawText(title.take(90), MARGIN, MARGIN, headerPaint)
            y = MARGIN + HEADER_SPACE
        }

        newPage()
        for (block in parse(markdown)) {
            if (block.type == BlockType.RULE) {
                if (y + 16f > bottom) newPage()
                canvas!!.drawLine(MARGIN, y + 8f, PAGE_WIDTH - MARGIN, y + 8f, rulePaint)
                y += 16f
                continue
            }
            val paint = paintFor(block.type)
            val indent = when (block.type) {
                BlockType.BULLET -> 14f
                BlockType.QUOTE, BlockType.CODE -> 10f
                else -> 0f
            }
            val text = styled(block.text, block.type)
            val width = (contentWidth - indent).toInt().coerceAtLeast(50)
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, if (block.type == BlockType.CODE) 1.1f else 1.25f)
                .setIncludePad(false)
                .build()

            val spaceBefore = when (block.type) {
                BlockType.H1 -> 14f
                BlockType.H2 -> 12f
                BlockType.H3 -> 9f
                BlockType.BULLET -> 2f
                else -> 5f
            }
            if (y > MARGIN + HEADER_SPACE) y += spaceBefore
            // Keep headings together with the first lines that follow them.
            if (block.type in setOf(BlockType.H1, BlockType.H2, BlockType.H3) && y + layout.height + 40f > bottom) newPage()

            for (line in 0 until layout.lineCount) {
                val top = layout.getLineTop(line).toFloat()
                val lineHeight = layout.getLineBottom(line) - top
                if (y + lineHeight > bottom) newPage()
                val c = canvas!!
                if (line == 0 && block.type == BlockType.BULLET) {
                    c.drawCircle(MARGIN + 4f, y + lineHeight / 2f, 1.8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY })
                }
                if (block.type == BlockType.QUOTE || block.type == BlockType.CODE) {
                    c.drawRect(MARGIN, y, MARGIN + 2.5f, y + lineHeight, Paint().apply { color = Color.rgb(170, 180, 230) })
                }
                c.save()
                c.clipRect(MARGIN + indent, y, PAGE_WIDTH - MARGIN, y + lineHeight)
                c.translate(MARGIN + indent, y - top)
                layout.draw(c)
                c.restore()
                y += lineHeight
            }
        }
        finishPage()
        document.writeTo(out)
        document.close()
    }

    private fun paintFor(type: BlockType): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 22, 28)
        when (type) {
            BlockType.H1 -> { textSize = 19f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
            BlockType.H2 -> { textSize = 15f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
            BlockType.H3 -> { textSize = 12.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
            BlockType.CODE -> { textSize = 9f; typeface = Typeface.MONOSPACE; color = Color.rgb(50, 56, 70) }
            BlockType.QUOTE -> { textSize = 10.5f; typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC); color = Color.rgb(70, 76, 90) }
            else -> { textSize = 10.5f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL) }
        }
    }

    private fun parse(markdown: String): List<Block> {
        val blocks = mutableListOf<Block>()
        val paragraph = StringBuilder()
        val code = StringBuilder()
        var inCode = false

        fun flush() {
            if (paragraph.isNotBlank()) blocks += Block(BlockType.PARAGRAPH, paragraph.toString().trim())
            paragraph.setLength(0)
        }

        for (raw in markdown.replace("\r", "").lines()) {
            val line = raw.trimEnd()
            if (line.trimStart().startsWith("```")) {
                if (inCode) {
                    blocks += Block(BlockType.CODE, code.toString().trimEnd())
                    code.setLength(0)
                } else {
                    flush()
                }
                inCode = !inCode
                continue
            }
            if (inCode) {
                code.append(line).append('\n')
                continue
            }
            val trimmed = line.trim()
            when {
                trimmed.isEmpty() -> flush()
                trimmed.startsWith("<!--") || trimmed.startsWith("-->") -> flush()
                trimmed.startsWith("### ") || trimmed.startsWith("#### ") -> { flush(); blocks += Block(BlockType.H3, trimmed.trimStart('#').trim()) }
                trimmed.startsWith("## ") -> { flush(); blocks += Block(BlockType.H2, trimmed.removePrefix("## ").trim()) }
                trimmed.startsWith("# ") -> { flush(); blocks += Block(BlockType.H1, trimmed.removePrefix("# ").trim()) }
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> { flush(); blocks += Block(BlockType.RULE, "") }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") ->
                    { flush(); blocks += Block(BlockType.BULLET, trimmed.drop(2).trim()) }
                Regex("^\\d+[.)] .*").matches(trimmed) -> { flush(); blocks += Block(BlockType.PARAGRAPH, trimmed) }
                trimmed.startsWith(">") -> { flush(); blocks += Block(BlockType.QUOTE, trimmed.trimStart('>').trim()) }
                trimmed.startsWith("|") -> { flush(); blocks += Block(BlockType.CODE, trimmed) }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(trimmed)
                }
            }
        }
        if (inCode && code.isNotBlank()) blocks += Block(BlockType.CODE, code.toString().trimEnd())
        flush()
        return blocks
    }

    /** Converts **bold**, *italic* and `code` markers into spans (markers removed). */
    private fun styled(text: String, type: BlockType): CharSequence {
        if (type == BlockType.CODE) return text
        val builder = SpannableStringBuilder()
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 2) {
                        val start = builder.length
                        builder.append(text, i + 2, end)
                        builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = end + 2
                    } else {
                        builder.append(text[i]); i++
                    }
                }
                text[i] == '*' || text[i] == '_' -> {
                    val marker = text[i]
                    val end = text.indexOf(marker, i + 1)
                    if (end > i + 1 && !text[i + 1].isWhitespace()) {
                        val start = builder.length
                        builder.append(text, i + 1, end)
                        builder.setSpan(StyleSpan(Typeface.ITALIC), start, builder.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        i = end + 1
                    } else {
                        builder.append(text[i]); i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        builder.append(text, i + 1, end)
                        i = end + 1
                    } else {
                        builder.append(text[i]); i++
                    }
                }
                else -> {
                    builder.append(text[i]); i++
                }
            }
        }
        return builder
    }
}

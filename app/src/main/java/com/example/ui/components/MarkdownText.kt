package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OmniAccent
import com.example.ui.theme.OmniBorder
import com.example.ui.theme.OmniSurface
import com.example.ui.theme.OmniText
import com.example.ui.theme.OmniTextMuted

/** One block of a parsed Markdown document. */
internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val text: String, val marker: String, val indent: Int) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Table(val rows: List<List<String>>) : MdBlock
    data object Rule : MdBlock
}

/** Small line-based Markdown parser: enough for AI answers (headings, lists, tables, code, quotes). */
internal object MarkdownParser {
    private val orderedRegex = Regex("^(\\d+)[.)]\\s+(.*)")
    private val tableSeparator = Regex("^\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?$")

    fun parse(markdown: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val lines = markdown.replace("\r\n", "\n").split('\n')
        var i = 0
        val paragraph = StringBuilder()

        fun flushParagraph() {
            if (paragraph.isNotBlank()) blocks += MdBlock.Paragraph(paragraph.toString().trim())
            paragraph.clear()
        }

        while (i < lines.size) {
            val raw = lines[i]
            val line = raw.trim()
            when {
                line.startsWith("```") -> {
                    flushParagraph()
                    val code = StringBuilder()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        code.appendLine(lines[i])
                        i++
                    }
                    blocks += MdBlock.Code(code.toString().trimEnd())
                }
                line.isEmpty() -> flushParagraph()
                line.startsWith("#") -> {
                    flushParagraph()
                    val level = line.takeWhile { it == '#' }.length.coerceIn(1, 6)
                    blocks += MdBlock.Heading(level, line.drop(level).trim())
                }
                line == "---" || line == "***" || line == "___" -> {
                    flushParagraph()
                    blocks += MdBlock.Rule
                }
                line.startsWith(">") -> {
                    flushParagraph()
                    blocks += MdBlock.Quote(line.trimStart('>').trim())
                }
                line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") -> {
                    flushParagraph()
                    val indent = (raw.length - raw.trimStart().length) / 2
                    val text = line.drop(2)
                    val marker = when {
                        text.startsWith("[ ] ") -> "☐"
                        text.startsWith("[x] ", ignoreCase = true) -> "☑"
                        else -> "•"
                    }
                    blocks += MdBlock.Bullet(if (marker == "•") text else text.drop(4), marker, indent)
                }
                orderedRegex.matches(line) -> {
                    flushParagraph()
                    val match = orderedRegex.find(line)!!
                    val indent = (raw.length - raw.trimStart().length) / 2
                    blocks += MdBlock.Bullet(match.groupValues[2], "${match.groupValues[1]}.", indent)
                }
                line.startsWith("|") -> {
                    flushParagraph()
                    val rows = mutableListOf<List<String>>()
                    while (i < lines.size && lines[i].trim().startsWith("|")) {
                        val row = lines[i].trim()
                        if (!tableSeparator.matches(row)) {
                            rows += row.trim('|').split('|').map { it.trim() }
                        }
                        i++
                    }
                    i--
                    if (rows.isNotEmpty()) blocks += MdBlock.Table(rows)
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(line)
                }
            }
            i++
        }
        flushParagraph()
        return blocks
    }

    /** Inline **bold**, *italic*, `code`; markers that never close are kept as text. */
    fun inline(text: String, codeBackground: androidx.compose.ui.graphics.Color): AnnotatedString = buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) && text.indexOf("**", i + 2) > i + 2 -> {
                    val end = text.indexOf("**", i + 2)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                }
                text[i] == '`' && text.indexOf('`', i + 1) > i + 1 -> {
                    val end = text.indexOf('`', i + 1)
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                }
                (text[i] == '*' || text[i] == '_') && i + 1 < text.length && text[i + 1] != ' ' &&
                    text.indexOf(text[i], i + 1) > i + 1 -> {
                    val end = text.indexOf(text[i], i + 1)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

/** Renders Markdown produced by Gemini with the Morph typography. */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val codeBg = OmniBorder.copy(alpha = 0.5f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = MarkdownParser.inline(block.text, codeBg),
                    fontSize = when (block.level) { 1 -> 21.sp; 2 -> 18.sp; else -> 16.sp },
                    fontWeight = FontWeight.Bold,
                    color = if (block.level <= 2) OmniText else OmniAccent,
                    modifier = Modifier.padding(top = if (block.level <= 2) 6.dp else 2.dp)
                )
                is MdBlock.Paragraph -> Text(MarkdownParser.inline(block.text, codeBg), fontSize = 14.sp, lineHeight = 22.sp, color = OmniText)
                is MdBlock.Bullet -> Row(modifier = Modifier.padding(start = (block.indent * 16).dp)) {
                    Text(block.marker, fontSize = 14.sp, lineHeight = 22.sp, color = OmniAccent, fontWeight = FontWeight.Bold, modifier = Modifier.width(if (block.marker.length > 1) 26.dp else 16.dp))
                    Text(MarkdownParser.inline(block.text, codeBg), fontSize = 14.sp, lineHeight = 22.sp, color = OmniText)
                }
                is MdBlock.Quote -> Row {
                    Box(modifier = Modifier.width(3.dp).padding(vertical = 2.dp).background(OmniAccent).padding(vertical = 10.dp))
                    Text(
                        MarkdownParser.inline(block.text, codeBg),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        fontStyle = FontStyle.Italic,
                        color = OmniTextMuted,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                is MdBlock.Code -> Text(
                    text = block.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = OmniText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(OmniSurface)
                        .horizontalScroll(rememberScrollState())
                        .padding(10.dp)
                )
                is MdBlock.Table -> MarkdownTable(block.rows, codeBg)
                MdBlock.Rule -> HorizontalDivider(color = OmniBorder, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun MarkdownTable(rows: List<List<String>>, codeBg: androidx.compose.ui.graphics.Color) {
    val columns = rows.maxOf { it.size }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(OmniSurface)
            .horizontalScroll(rememberScrollState())
    ) {
        rows.forEachIndexed { index, row ->
            Row(modifier = if (index == 0) Modifier.background(OmniBorder.copy(alpha = 0.35f)) else Modifier) {
                for (c in 0 until columns) {
                    Text(
                        text = MarkdownParser.inline(row.getOrElse(c) { "" }, codeBg),
                        fontSize = 13.sp,
                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                        color = OmniText,
                        modifier = Modifier
                            .width(140.dp)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
            if (index < rows.lastIndex) HorizontalDivider(color = OmniBorder, modifier = Modifier.width((columns * 140).dp))
        }
    }
}

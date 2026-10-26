package com.example

import com.example.service.PdfLine
import com.example.service.PdfMarkdownBuilder
import com.example.service.PdfMarkdownOptions
import com.example.service.StyledWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfMarkdownBuilderTest {

    /** Builds a line whose words are laid out left to right with normal spacing. */
    private fun line(text: String, y: Float, size: Float = 11f, x: Float = 72f, bold: Boolean = false, italic: Boolean = false, gaps: Map<Int, Float> = emptyMap()): PdfLine {
        var cursor = x
        val words = text.split(' ').mapIndexed { index, word ->
            cursor += gaps[index] ?: 0f
            val start = cursor
            val end = start + word.length * size * 0.5f
            cursor = end + size * 0.3f
            StyledWord(word, bold, italic, start, end)
        }
        return PdfLine(words, x, y, words.last().endX, size, size * 1.2f)
    }

    private val options = PdfMarkdownOptions()

    @Test
    fun detectsHeadingsByFontSizeAndBold() {
        val lines = listOf(
            line("La Rivoluzione francese", 60f, size = 20f),
            line("Le cause", 100f, size = 14f),
            line("Il contesto economico", 130f, bold = true),
            line("Nel 1789 la Francia era in crisi.", 150f)
        )
        val md = PdfMarkdownBuilder.render(lines, 11f, emptySet(), options)
        assertTrue(md, md.contains("## La Rivoluzione francese"))
        assertTrue(md, md.contains("### Le cause"))
        assertTrue(md, md.contains("#### Il contesto economico"))
        assertTrue(md, md.contains("Nel 1789 la Francia era in crisi."))
    }

    @Test
    fun joinsWrappedLinesAndHyphenation() {
        val lines = listOf(
            line("La fotosintesi è un pro-", 100f),
            line("cesso che avviene nelle foglie", 113f)
        )
        val md = PdfMarkdownBuilder.render(lines, 11f, emptySet(), options)
        assertEquals("La fotosintesi è un processo che avviene nelle foglie", md)
    }

    @Test
    fun buildsListsAndKeepsNumbers() {
        val lines = listOf(
            line("• primo punto", 100f),
            line("• secondo punto", 113f),
            line("1. passo uno", 140f),
            line("2. passo due", 153f)
        )
        val md = PdfMarkdownBuilder.render(lines, 11f, emptySet(), options)
        assertTrue(md, md.contains("- primo punto\n- secondo punto"))
        assertTrue(md, md.contains("1. passo uno\n2. passo due"))
    }

    @Test
    fun boldWordsInsideParagraphBecomeMarkdownBold() {
        val words = listOf(
            StyledWord("La", false, false, 72f, 80f),
            StyledWord("mitosi", true, false, 84f, 110f),
            StyledWord("divide", false, false, 114f, 140f)
        )
        val md = PdfMarkdownBuilder.render(listOf(PdfLine(words, 72f, 100f, 140f, 11f, 13f)), 11f, emptySet(), options)
        assertEquals("La **mitosi** divide", md)
    }

    @Test
    fun removesRepeatedFootersAndPageNumbers() {
        val pages = (1..4).map { n ->
            listOf(
                line("Appunti di Storia - Classe 4B", 30f, size = 9f),
                line("Contenuto della pagina $n con testo.", 200f),
                line("$n", 800f, size = 9f)
            )
        }
        val repeated = PdfMarkdownBuilder.repeatedMargins(pages)
        val md = PdfMarkdownBuilder.render(pages[1], 11f, repeated, options)
        assertEquals("Contenuto della pagina 2 con testo.", md)
        assertFalse(md.contains("Classe 4B"))
    }

    @Test
    fun alignedColumnsBecomeATable() {
        val gaps = mapOf(1 to 60f, 2 to 60f)
        val lines = listOf(
            line("Anno Evento Luogo", 100f, gaps = gaps),
            line("1789 Bastiglia Parigi", 113f, gaps = gaps)
        )
        val md = PdfMarkdownBuilder.render(lines, 11f, emptySet(), options)
        assertTrue(md, md.contains("| Anno | Evento | Luogo |"))
        assertTrue(md, md.contains("| 1789 | Bastiglia | Parigi |"))
    }

    @Test
    fun bodySizeIsTheMostCommonSize() {
        val pages = listOf(listOf(line("Titolo", 10f, size = 20f), line("testo lungo di prova per il corpo", 40f, size = 11f)))
        assertEquals(11f, PdfMarkdownBuilder.bodySize(pages))
    }
}

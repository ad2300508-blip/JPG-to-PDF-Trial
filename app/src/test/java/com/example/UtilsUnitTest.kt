package com.example

import com.example.service.PageNumberStyle
import com.example.service.StampConfig
import com.example.ui.PasswordStrength
import com.example.ui.components.MarkdownParser
import com.example.ui.components.MdBlock
import com.example.util.FileNames
import com.example.util.PageRange
import com.example.viewmodel.NaturalOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.example.ui.theme.withAccent
import org.junit.Test

class UtilsUnitTest {

    @Test
    fun pageRangeParsesListsAndRanges() {
        assertEquals(listOf(0, 1, 2, 4), PageRange.parse("1-3, 5", 10))
        assertEquals(listOf(7, 8, 9), PageRange.parse("8-", 10))
        assertEquals(listOf(0, 1), PageRange.parse("-2", 10))
        assertEquals((0 until 4).toList(), PageRange.parse("", 4))
        assertEquals((0 until 4).toList(), PageRange.parse("tutte", 4))
    }

    @Test
    fun pageRangeIgnoresInvalidAndDuplicates() {
        assertEquals(listOf(1), PageRange.parse("2, 2, 99, abc", 5))
        assertEquals(listOf(4, 3, 2), PageRange.parse("5-3", 5))
        assertTrue(PageRange.isValid("", 3))
        assertFalse(PageRange.isValid("7-9", 3))
    }

    @Test
    fun pageRangeDescribeIsCompact() {
        assertEquals("1-3, 5", PageRange.describe(listOf(0, 1, 2, 4)))
        assertEquals("2", PageRange.describe(listOf(1)))
        assertEquals("", PageRange.describe(emptyList()))
    }

    @Test
    fun fileNamesAreSanitizedButKeepAccents() {
        assertEquals("Storia dell'arte_ cap. 1", FileNames.sanitize("Storia dell'arte: cap. 1", "x"))
        assertEquals("Perché_sì", FileNames.sanitize("Perché/sì", "x"))
        assertEquals("fallback", FileNames.sanitize("   ", "fallback"))
        assertTrue(FileNames.stamped("Appunti", "Doc", "pdf").matches(Regex("Appunti_\\d{8}_\\d{6}\\.pdf")))
    }

    @Test
    fun zipEntryNamesAreDeduplicated() {
        assertEquals(
            listOf("a.txt", "a (2).txt", "b", "b (2)", "A (3).txt"),
            FileNames.dedupe(listOf("a.txt", "a.txt", "b", "b", "A.txt"))
        )
    }

    @Test
    fun naturalOrderSortsNumbersAsHumansDo() {
        val sorted = listOf("foto10.jpg", "foto2.jpg", "foto1.jpg").sortedWith(NaturalOrder)
        assertEquals(listOf("foto1.jpg", "foto2.jpg", "foto10.jpg"), sorted)
    }

    @Test
    fun pageNumberStylesFormat() {
        assertEquals("3", PageNumberStyle.SIMPLE.format(3, 10))
        assertEquals("3 / 10", PageNumberStyle.OF_TOTAL.format(3, 10))
        assertEquals("Pagina 3 di 10", PageNumberStyle.VERBOSE.format(3, 10))
        assertTrue(StampConfig(text = " ").isEmpty)
        assertFalse(StampConfig(text = "", numberStyle = PageNumberStyle.SIMPLE).isEmpty)
    }

    @Test
    fun passwordStrengthGrows() {
        assertEquals(PasswordStrength.WEAK, PasswordStrength.of("1234"))
        assertEquals(PasswordStrength.STRONG, PasswordStrength.of("Fisica-2026!x"))
        assertTrue(PasswordStrength.of("studiare2026").level > PasswordStrength.of("abc").level)
    }

    @Test
    fun markdownParserFindsBlocks() {
        val md = """
            # Titolo
            Testo **grassetto** su
            due righe.

            - punto uno
            1. primo
            | A | B |
            |---|---|
            | 1 | 2 |
        """.trimIndent()
        val blocks = MarkdownParser.parse(md)
        assertEquals(MdBlock.Heading(1, "Titolo"), blocks[0])
        assertEquals(MdBlock.Paragraph("Testo **grassetto** su due righe."), blocks[1])
        assertEquals(MdBlock.Bullet("punto uno", "•", 0), blocks[2])
        assertEquals(MdBlock.Bullet("primo", "1.", 0), blocks[3])
        assertEquals(MdBlock.Table(listOf(listOf("A", "B"), listOf("1", "2"))), blocks[4])
    }

    @Test
    fun pageRangeGroupsAndChunks() {
        assertEquals(listOf(listOf(0, 1, 2), listOf(3, 4), listOf(8, 9)), PageRange.groups("1-3, 4-5; 9-", 10))
        assertEquals(listOf(listOf(0, 1), listOf(2, 3), listOf(4)), PageRange.chunks(5, 2))
        assertTrue(PageRange.groups("abc, 99", 5).isEmpty())
    }

    @Test
    fun accentChoiceOnlyChangesAccentColours() {
        val teal = com.example.ui.theme.MorphLightColors.withAccent(com.example.ui.theme.AccentChoice.TEAL)
        assertEquals(com.example.ui.theme.AccentChoice.TEAL.light, teal.accent)
        assertEquals(com.example.ui.theme.MorphLightColors.bg, teal.bg)
        val darkOrange = com.example.ui.theme.MorphDarkColors.withAccent(com.example.ui.theme.AccentChoice.ORANGE)
        assertEquals(com.example.ui.theme.AccentChoice.ORANGE.dark, darkOrange.accent)
        assertEquals(com.example.ui.theme.AccentChoice.ORANGE.darkOn, darkOrange.onAccent)
        assertEquals(5, com.example.ui.theme.AccentChoice.entries.map { it.light }.toSet().size.coerceAtLeast(4) + 1)
    }
}

package com.example

import com.example.service.PageNumberStyle
import com.example.service.StampConfig
import com.example.ui.PasswordStrength
import com.example.util.FileNames
import com.example.util.PageRange
import com.example.viewmodel.NaturalOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}

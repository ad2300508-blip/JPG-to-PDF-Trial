package com.example

import com.example.util.DocStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocStatsTest {

    @Test
    fun countsWordsAndCharacters() {
        assertEquals(5, DocStats.words("La  Rivoluzione francese — 1789\n- inizio"))
        assertEquals(0, DocStats.words("  \n — ... "))
        assertEquals(7, DocStats.characters("ab c\nde fg"))
    }

    @Test
    fun readingTimeAndPages() {
        assertEquals(0, DocStats.readingMinutes(0))
        assertEquals(1, DocStats.readingMinutes(50))
        assertEquals(3, DocStats.readingMinutes(401))
        assertEquals(1, DocStats.estimatedPages(0))
        assertEquals(3, DocStats.estimatedPages(901))
    }

    @Test
    fun recognisesPaperSizesInBothOrientations() {
        assertEquals("A4", DocStats.paperName(595f, 842f))
        assertEquals("A4", DocStats.paperName(842f, 595f))
        assertEquals("Letter", DocStats.paperName(612f, 792f))
        assertEquals("A5", DocStats.paperName(420f, 595f))
        assertNull(DocStats.paperName(500f, 500f))
        assertEquals(210, DocStats.ptToMm(595.28f).toInt())
    }
}

package com.example

import com.example.ui.components.remainingTimeText
import com.example.ui.home.OmniTool
import com.example.ui.home.ToolGroup
import com.example.ui.home.toolForHistoryLabel
import com.example.util.SearchText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSearchTest {

    @Test
    fun foldIgnoresAccentsAndCase() {
        assertEquals("citta perche pdf", SearchText.fold("Città Perché PDF"))
        assertTrue(SearchText.matches(SearchText.fold("Unisci PDF combina"), "pdf unisci"))
        assertFalse(SearchText.matches(SearchText.fold("Unisci PDF"), "zip"))
        assertTrue(SearchText.matches("qualsiasi", ""))
    }

    @Test
    fun highlightsMatchOriginalPositions() {
        assertEquals(listOf(0..2, 8..10), SearchText.highlights("PDF and PDF", "pdf"))
        // "à" folds to "a": the range still covers the accented letter in the original title.
        assertEquals(listOf(3..5), SearchText.highlights("La città", "cit"))
        assertEquals(listOf(0..5), SearchText.highlights("Unisci PDF", "uni nisci"))
        assertTrue(SearchText.highlights("Firma", "  ").isEmpty())
    }

    @Test
    fun everyToolHasAGroupAndIsFindable() {
        ToolGroup.entries.forEach { group -> assertTrue(group.label, OmniTool.entries.any { it.group == group }) }
        OmniTool.entries.forEach { tool ->
            assertTrue(tool.name, SearchText.matches(tool.searchIndex, SearchText.fold(tool.title)))
        }
        assertEquals(OmniTool.ORGANIZE_PDF, toolForHistoryLabel("Organizza PDF"))
        assertEquals(OmniTool.MERGE_PDF, toolForHistoryLabel("Unisci PDF"))
        assertNull(toolForHistoryLabel("Gemini"))
    }

    @Test
    fun remainingTimeOnlyWhenMeaningful() {
        assertNull(remainingTimeText(0.02f, 10_000))
        assertNull(remainingTimeText(0.5f, 1_000))
        assertEquals("circa 10 s rimanenti", remainingTimeText(0.5f, 10_000))
        assertEquals("circa 3 min rimanenti", remainingTimeText(0.25f, 60_000))
    }
}

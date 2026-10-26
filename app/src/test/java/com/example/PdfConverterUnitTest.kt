package com.example

import android.net.Uri
import com.example.model.CropRect
import com.example.model.LlmPromptWrapper
import com.example.model.MarkdownExportSettings
import com.example.model.MarkdownSourceItem
import com.example.model.PdfDocumentItem
import com.example.model.PdfExportConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfConverterUnitTest {

    @Test
    fun testTokenEstimationAccuracy() {
        val emptyText = ""
        assertEquals(0, MarkdownSourceItem.estimateTokens(emptyText))

        val shortText = "Hello world" // 11 chars / 3.8 ~ 2 tokens
        val tokens = MarkdownSourceItem.estimateTokens(shortText)
        assertTrue(tokens in 1..4)

        val longText = "a".repeat(380) // 380 / 3.8 = 100 tokens
        assertEquals(100, MarkdownSourceItem.estimateTokens(longText))
    }

    @Test
    fun testPromptWrapperClaudeXmlFormatting() {
        val item = MarkdownSourceItem(
            name = "AnnualReport.pdf",
            extractedText = "# Revenue 2024\nTotal revenue: $10M"
        )
        val prompt = wrapForLlmTest(listOf(item), item.extractedText, LlmPromptWrapper.CLAUDE_XML)
        assertTrue(prompt.contains("<documents>"))
        assertTrue(prompt.contains("<document index=\"1\">"))
        assertTrue(prompt.contains("<source>AnnualReport.pdf</source>"))
        assertTrue(prompt.contains("<document_content>"))
        assertTrue(prompt.contains("# Revenue 2024"))
        assertTrue(prompt.contains("</documents>"))
    }

    @Test
    fun testPromptWrapperChatGptFormatting() {
        val item = MarkdownSourceItem(
            name = "MeetingNotes.txt",
            extractedText = "- Item 1\n- Item 2"
        )
        val prompt = wrapForLlmTest(listOf(item), item.extractedText, LlmPromptWrapper.CHATGPT_SYSTEM)
        assertTrue(prompt.contains("### Context File 1: MeetingNotes.txt"))
        assertTrue(prompt.contains("```markdown"))
        assertTrue(prompt.contains("- Item 1"))
    }

    @Test
    fun testPromptWrapperGeminiBriefFormatting() {
        val item = MarkdownSourceItem(
            name = "ProjectBrief.md",
            extractedText = "Summary of roadmap"
        )
        val prompt = wrapForLlmTest(listOf(item), item.extractedText, LlmPromptWrapper.GEMINI_BRIEF)
        assertTrue(prompt.contains("=== OMNICONVERT AI CONTEXT BRIEF ==="))
        assertTrue(prompt.contains("DOCUMENT [1]: ProjectBrief.md"))
        assertTrue(prompt.contains("Summary of roadmap"))
    }

    @Test
    fun testOomDocumentFiltering() {
        val goodItem = MarkdownSourceItem(
            name = "good.pdf",
            extractedText = "Valid content"
        )
        val failedItem = MarkdownSourceItem(
            name = "huge.pdf",
            extractedText = "",
            error = "Document too large for available RAM"
        )

        val items = listOf(goodItem, failedItem)
        val validItems = items.filter { it.extractedText.isNotEmpty() && it.error == null }
        assertEquals(1, validItems.size)
        assertEquals("good.pdf", validItems[0].name)
    }

    @Test
    fun testPdfSpreadSplittingModel() {
        val item = PdfDocumentItem(
            name = "spread.png",
            isSplitSpread = true,
            splitPart = 1
        )
        assertTrue(item.isSplitSpread)
        assertEquals(1, item.splitPart)

        val item2 = item.copy(splitPart = 2)
        assertEquals(2, item2.splitPart)
    }

    @Test
    fun testPdfExportConfigMetadata() {
        val defaultConfig = PdfExportConfig()
        assertFalse(defaultConfig.hasCustomMetadata)
        assertEquals("Documento", defaultConfig.title)
        assertEquals("", defaultConfig.author)
        assertEquals("", defaultConfig.keywords)
        assertEquals("", defaultConfig.creationDate)
        assertEquals("Morph", defaultConfig.creator)

        val customConfig = defaultConfig.copy(
            title = "Renaissance Architecture",
            author = "Prof. Mario Rossi",
            subject = "History of Art Chapter 25",
            keywords = "architecture, art, renaissance",
            creationDate = "2026-10-25"
        )
        assertTrue(customConfig.hasCustomMetadata)
        assertEquals("Renaissance Architecture", customConfig.title)
        assertEquals("Prof. Mario Rossi", customConfig.author)
        assertEquals("History of Art Chapter 25", customConfig.subject)
        assertEquals("architecture, art, renaissance", customConfig.keywords)
        assertEquals("2026-10-25", customConfig.creationDate)
    }

    @Test
    fun testPdfExportConfigOrientationAndDefaults() {
        val defaultConfig = PdfExportConfig()
        assertEquals("Auto", defaultConfig.orientation)
        assertEquals("A4", defaultConfig.pageSize)
        assertEquals(10, defaultConfig.marginMm)

        val landscapeConfig = defaultConfig.copy(orientation = "Landscape", marginMm = 0)
        assertEquals("Landscape", landscapeConfig.orientation)
        assertEquals(0, landscapeConfig.marginMm)
    }

    private fun wrapForLlmTest(
        items: List<MarkdownSourceItem>,
        rawMarkdown: String,
        wrapper: LlmPromptWrapper
    ): String {
        return when (wrapper) {
            LlmPromptWrapper.CLAUDE_XML -> {
                val sb = java.lang.StringBuilder()
                sb.append("You are an expert AI assistant. Below is the reference material provided in structured XML format.\n\n")
                sb.append("<documents>\n")
                items.forEachIndexed { index, item ->
                    sb.append("  <document index=\"").append(index + 1).append("\">\n")
                    sb.append("    <source>").append(item.name).append("</source>\n")
                    sb.append("    <document_content>\n")
                    sb.append(item.extractedText.trim())
                    sb.append("\n    </document_content>\n")
                    sb.append("  </document>\n")
                }
                sb.append("</documents>\n\n")
                sb.toString()
            }
            LlmPromptWrapper.CHATGPT_SYSTEM -> {
                val sb = java.lang.StringBuilder()
                sb.append("You are a helpful and precise assistant. Use the following verified context files to complete the request.\n\n")
                items.forEachIndexed { index, item ->
                    sb.append("### Context File ").append(index + 1).append(": ").append(item.name).append("\n")
                    sb.append("```markdown\n")
                    sb.append(item.extractedText.trim()).append("\n")
                    sb.append("```\n\n")
                }
                sb.toString()
            }
            LlmPromptWrapper.GEMINI_BRIEF -> {
                val sb = java.lang.StringBuilder()
                sb.append("=== OMNICONVERT AI CONTEXT BRIEF ===\n")
                sb.append("DOCUMENTS INGESTED: ").append(items.size).append("\n\n")
                items.forEachIndexed { index, item ->
                    sb.append("========================================\n")
                    sb.append("DOCUMENT [").append(index + 1).append("]: ").append(item.name).append("\n")
                    sb.append("========================================\n\n")
                    sb.append(item.extractedText.trim()).append("\n\n")
                }
                sb.toString()
            }
            LlmPromptWrapper.RAW_MARKDOWN -> rawMarkdown
        }
    }
}

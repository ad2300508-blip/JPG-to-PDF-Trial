package com.example.service

import android.graphics.Rect
import com.google.mlkit.vision.text.Text

object OcrUtils {

    private val NOISE_REGEXES = listOf(
        Regex("(?i).*tieni premuto esc.*"),
        Regex("(?i).*zanichelli.*"),
        Regex("(?i).*modalità a schermo intero.*"),
        Regex("(?i).*schermo intero.*"),
        Regex("(?i).*premi esc per uscire.*")
    )

    private val CAPTION_PREFIX_REGEX = Regex("(?i)^(fig(\\.|ura)?|schema|tav(\\.|ola)?|foto|doc(\\.|umento)?|tab(\\.|ella)?|\\d+\\.\\d+).*")

    /**
     * Processes ML Kit Text blocks for a page:
     * 1. Strips status bar (top 3.8%) and UI noise.
     * 2. Detects landscape dual-page spreads and splits into Left and Right subpages.
     * 3. Sorts main body columns column-by-column (Left column top-to-bottom, then Right column top-to-bottom).
     * 4. Separates and formats side-margin captions as blockquotes.
     * 5. Reconstructs hyphenated line-breaks and wraps paragraphs smoothly.
     */
    fun processPageBlocks(
        visionText: Text,
        pageWidth: Int,
        pageHeight: Int
    ): String {
        val allBlocks = visionText.textBlocks
        if (allBlocks.isEmpty()) return ""

        val topStatusBarThreshold = pageHeight * 0.038f

        // 1. Filter out status bar noise and UI overlay text
        val cleanedBlocks = allBlocks.filter { block ->
            val box = block.boundingBox ?: return@filter false
            // Ignore blocks entirely within top 3.8% of page (e.g. Android clock "6:15 Mer 21 Ott")
            if (box.top < topStatusBarThreshold && box.bottom < pageHeight * 0.075f) {
                return@filter false
            }
            // Ignore UI noise matching regexes
            val blockText = block.text.trim()
            if (isNoiseText(blockText)) {
                return@filter false
            }
            true
        }

        if (cleanedBlocks.isEmpty()) return ""

        val isLandscape = pageWidth > (pageHeight * 1.15f)

        return if (isLandscape) {
            // Dual-page spread: split into Left Page and Right Page
            val midX = pageWidth / 2
            val leftPageBlocks = cleanedBlocks.filter { (it.boundingBox?.centerX() ?: 0) < midX }
            val rightPageBlocks = cleanedBlocks.filter { (it.boundingBox?.centerX() ?: 0) >= midX }

            val leftText = processSubpage(leftPageBlocks, minX = 0, maxX = midX, pageHeight = pageHeight, isLeftPage = true)
            val rightText = processSubpage(rightPageBlocks, minX = midX, maxX = pageWidth, pageHeight = pageHeight, isLeftPage = false)

            buildString {
                if (leftText.isNotBlank()) {
                    append(leftText.trim())
                    append("\n\n")
                }
                if (rightText.isNotBlank()) {
                    if (leftText.isNotBlank()) {
                        append("---\n\n") // Visual divider between spread pages
                    }
                    append(rightText.trim())
                    append("\n\n")
                }
            }.trim()
        } else {
            // Single portrait page
            processSubpage(cleanedBlocks, minX = 0, maxX = pageWidth, pageHeight = pageHeight, isLeftPage = null).trim()
        }
    }

    private fun processSubpage(
        blocks: List<Text.TextBlock>,
        minX: Int,
        maxX: Int,
        pageHeight: Int,
        isLeftPage: Boolean?
    ): String {
        if (blocks.isEmpty()) return ""

        val subpageWidth = (maxX - minX).coerceAtLeast(1)

        // Identify side-margin captions vs main body blocks
        val sideCaptions = mutableListOf<Text.TextBlock>()
        val mainBlocks = mutableListOf<Text.TextBlock>()

        for (block in blocks) {
            val box = block.boundingBox ?: continue
            val blockWidth = box.width()
            val text = block.text.trim()

            val isNarrow = blockWidth < (subpageWidth * 0.22f)
            val isOuterMargin = when (isLeftPage) {
                true -> (box.left - minX) < (subpageWidth * 0.20f) // Outer left edge on left page
                false -> (maxX - box.right) < (subpageWidth * 0.20f) // Outer right edge on right page
                null -> (box.left - minX) < (subpageWidth * 0.18f) || (maxX - box.right) < (subpageWidth * 0.18f)
            }
            val hasCaptionPrefix = CAPTION_PREFIX_REGEX.matches(text)

            if ((isNarrow && isOuterMargin) || (isNarrow && hasCaptionPrefix) || hasCaptionPrefix && blockWidth < (subpageWidth * 0.35f)) {
                sideCaptions.add(block)
            } else {
                mainBlocks.add(block)
            }
        }

        // Sort main body blocks column-by-column:
        // Check if there are 2 distinct columns
        val subpageMidX = minX + (subpageWidth / 2)

        // Classify spanning banners (spanning > 65% of subpage width)
        val wideBannerBlocks = mutableListOf<Text.TextBlock>()
        val columnBlocks = mutableListOf<Text.TextBlock>()

        for (block in mainBlocks) {
            val box = block.boundingBox ?: continue
            if (box.width() > (subpageWidth * 0.65f)) {
                wideBannerBlocks.add(block)
            } else {
                columnBlocks.add(block)
            }
        }

        val leftCol = mutableListOf<Text.TextBlock>()
        val rightCol = mutableListOf<Text.TextBlock>()

        for (block in columnBlocks) {
            val box = block.boundingBox ?: continue
            if (box.centerX() < subpageMidX) {
                leftCol.add(block)
            } else {
                rightCol.add(block)
            }
        }

        // Sort within columns top-to-bottom
        leftCol.sortBy { it.boundingBox?.top ?: 0 }
        rightCol.sortBy { it.boundingBox?.top ?: 0 }
        wideBannerBlocks.sortBy { it.boundingBox?.top ?: 0 }
        sideCaptions.sortBy { it.boundingBox?.top ?: 0 }

        // Assemble ordered blocks:
        // 1. Top headers/banners (whose bottom is before both columns begin)
        val firstColTop = minOf(
            leftCol.firstOrNull()?.boundingBox?.top ?: Int.MAX_VALUE,
            rightCol.firstOrNull()?.boundingBox?.top ?: Int.MAX_VALUE
        )

        val topBanners = wideBannerBlocks.filter { (it.boundingBox?.bottom ?: Int.MAX_VALUE) <= firstColTop }
        val otherBanners = wideBannerBlocks.filter { (it.boundingBox?.bottom ?: Int.MAX_VALUE) > firstColTop }

        val orderedBlocks = mutableListOf<Text.TextBlock>()
        orderedBlocks.addAll(topBanners)
        orderedBlocks.addAll(leftCol)   // Left column strictly read first!
        orderedBlocks.addAll(rightCol)  // Right column read second!
        orderedBlocks.addAll(otherBanners)

        val sb = StringBuilder()

        for (block in orderedBlocks) {
            val formatted = formatTextBlock(block, isCaption = false)
            if (formatted.isNotBlank()) {
                sb.append(formatted).append("\n\n")
            }
        }

        // Append side-margin captions formatted as blockquotes
        if (sideCaptions.isNotEmpty()) {
            for (caption in sideCaptions) {
                val formattedCaption = formatTextBlock(caption, isCaption = true)
                if (formattedCaption.isNotBlank()) {
                    sb.append(formattedCaption).append("\n\n")
                }
            }
        }

        return sb.toString()
    }

    private fun isNoiseText(text: String): Boolean {
        for (pattern in NOISE_REGEXES) {
            if (pattern.containsMatchIn(text)) return true
        }
        return false
    }

    /**
     * Reconstructs hyphenated line-breaks and wraps lines into smooth paragraphs.
     * When a line ends with a hyphen followed by a lowercase letter on the next line,
     * they are joined seamlessly without the hyphen.
     */
    fun formatTextBlock(block: Text.TextBlock, isCaption: Boolean = false): String {
        val lines = block.lines
        if (lines.isEmpty()) return ""

        val rawText = block.text.trim()
        if (isNoiseText(rawText)) return ""

        val cleanedLines = lines.map { it.text.trim() }.filter { it.isNotEmpty() }
        if (cleanedLines.isEmpty()) return ""

        // Check if this is a short title / heading
        val totalLength = cleanedLines.sumOf { it.length }
        val isHeading = !isCaption && cleanedLines.size <= 2 && totalLength in 3..65 &&
                !cleanedLines.last().endsWith(".") &&
                (cleanedLines.first().all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it == ':' || it == '-' } ||
                        cleanedLines.first().matches(Regex("^(Capitolo|Sezione|Unità|Modulo|\\d+(\\.\\d+)*).*")))

        val paragraphBuilder = StringBuilder()
        var i = 0
        while (i < cleanedLines.size) {
            val currentLine = cleanedLines[i]
            if (i == 0) {
                paragraphBuilder.append(currentLine)
            } else {
                val prevLine = cleanedLines[i - 1]
                val prevEndsWithHyphen = prevLine.endsWith("-") || prevLine.endsWith("‐") || prevLine.endsWith("‑")

                if (prevEndsWithHyphen) {
                    // Remove trailing hyphen from builder and attach next line directly
                    // e.g. "architet-" + "tonico" -> "architettonico"
                    while (paragraphBuilder.isNotEmpty() && (paragraphBuilder.last() == '-' || paragraphBuilder.last() == '‐' || paragraphBuilder.last() == '‑')) {
                        paragraphBuilder.deleteCharAt(paragraphBuilder.length - 1)
                    }
                    paragraphBuilder.append(currentLine)
                } else {
                    // Continuation line in paragraph: join with single space
                    val prevEndsWithPunctuation = prevLine.endsWith(".") || prevLine.endsWith(":") || prevLine.endsWith(";")
                    val nextStartsCapital = currentLine.firstOrNull()?.isUpperCase() == true

                    if (prevEndsWithPunctuation && nextStartsCapital && prevLine.length < 35) {
                        // Separate thought / paragraph break
                        paragraphBuilder.append("\n\n").append(currentLine)
                    } else {
                        paragraphBuilder.append(" ").append(currentLine)
                    }
                }
            }
            i++
        }

        val reconstructed = paragraphBuilder.toString().trim()
        if (reconstructed.isBlank()) return ""

        return when {
            isCaption -> "> *${reconstructed}*"
            isHeading -> "### ${reconstructed}"
            else -> reconstructed
        }
    }
}

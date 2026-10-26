package com.example.util

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Small, pure text/page statistics used by "Info PDF" and "Testo in PDF". */
object DocStats {
    private val whitespace = Regex("\\s+")

    /** Words = whitespace-separated tokens that contain at least a letter or a digit. */
    fun words(text: String): Int = text.split(whitespace).count { token -> token.any { it.isLetterOrDigit() } }

    /** Characters without spaces and line breaks. */
    fun characters(text: String): Int = text.count { !it.isWhitespace() }

    /** Silent reading at about 200 words a minute; 0 only for an empty text. */
    fun readingMinutes(words: Int, wordsPerMinute: Int = 200): Int =
        if (words <= 0) 0 else max(1, ceil(words / wordsPerMinute.toDouble()).toInt())

    /** Roughly how many A4 pages "Testo in PDF" will produce (≈ 450 words each, at least 1). */
    fun estimatedPages(words: Int, wordsPerPage: Int = 450): Int = max(1, ceil(words / wordsPerPage.toDouble()).toInt())

    fun ptToMm(points: Float): Float = points * 25.4f / 72f

    private val papers = listOf(
        "A3" to (297f to 420f),
        "A4" to (210f to 297f),
        "A5" to (148f to 210f),
        "A6" to (105f to 148f),
        "Letter" to (215.9f to 279.4f),
        "Legal" to (215.9f to 355.6f)
    )

    /** "A4", "Letter"… for a page of [widthPt] × [heightPt] points (either orientation), or null. */
    fun paperName(widthPt: Float, heightPt: Float): String? {
        val shortMm = ptToMm(min(widthPt, heightPt))
        val longMm = ptToMm(max(widthPt, heightPt))
        return papers.firstOrNull { (_, size) -> abs(size.first - shortMm) <= 3f && abs(size.second - longMm) <= 3f }?.first
    }
}

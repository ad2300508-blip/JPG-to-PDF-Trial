package com.example.util

import java.text.Normalizer

/** Accent- and case-insensitive search helpers (shared by the home search and its highlighting). */
object SearchText {
    private val marks = Regex("\\p{Mn}+")

    /** "Città PDF" → "citta pdf". */
    fun fold(text: String): String = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(marks, "")

    /** True when every word of [query] appears in [index] (both already folded). */
    fun matches(index: String, query: String): Boolean {
        if (query.isBlank()) return true
        return query.split(' ').filter { it.isNotBlank() }.all { index.contains(it) }
    }

    /** Character ranges of [text] matching the words of [query], merged and sorted. */
    fun highlights(text: String, query: String): List<IntRange> {
        val words = fold(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty() || text.isEmpty()) return emptyList()
        // Fold one character at a time so positions in the folded copy match the original text.
        val folded = buildString(text.length) {
            for (c in text) append(fold(c.toString()).firstOrNull() ?: c)
        }
        val ranges = mutableListOf<IntRange>()
        for (word in words) {
            var from = folded.indexOf(word)
            while (from >= 0) {
                ranges += from until from + word.length
                from = folded.indexOf(word, from + word.length)
            }
        }
        if (ranges.isEmpty()) return emptyList()
        ranges.sortBy { it.first }
        val merged = mutableListOf(ranges.first())
        for (r in ranges.drop(1)) {
            val last = merged.last()
            if (r.first <= last.last + 1) merged[merged.lastIndex] = last.first..maxOf(last.last, r.last) else merged += r
        }
        return merged
    }
}

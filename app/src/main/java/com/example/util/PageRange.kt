package com.example.util

/**
 * Parses page selections typed by the user, e.g. "1-3, 5, 8-" or "tutte".
 * Pages are 1-based in the text and returned 0-based, in the typed order, without duplicates.
 */
object PageRange {

    fun parse(text: String, pageCount: Int): List<Int> {
        if (pageCount <= 0) return emptyList()
        val trimmed = text.trim().lowercase()
        if (trimmed.isEmpty() || trimmed == "tutte" || trimmed == "all" || trimmed == "*") {
            return (0 until pageCount).toList()
        }
        val result = LinkedHashSet<Int>()
        trimmed.split(',', ';', ' ').map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            val parts = token.split('-', '–')
            when {
                parts.size == 1 -> parts[0].toIntOrNull()?.let { p -> if (p in 1..pageCount) result.add(p - 1) }
                parts.size == 2 -> {
                    val start = parts[0].trim().ifEmpty { "1" }.toIntOrNull()
                    val end = parts[1].trim().ifEmpty { pageCount.toString() }.toIntOrNull()
                    if (start != null && end != null && minOf(start, end) <= pageCount && maxOf(start, end) >= 1) {
                        val from = start.coerceIn(1, pageCount)
                        val to = end.coerceIn(1, pageCount)
                        val range = if (from <= to) from..to else from downTo to
                        range.forEach { result.add(it - 1) }
                    }
                }
            }
        }
        return result.toList()
    }

    /** True when the text is empty or selects at least one page. */
    fun isValid(text: String, pageCount: Int): Boolean =
        text.isBlank() || pageCount <= 0 || parse(text, pageCount).isNotEmpty()

    /** Compact description of 0-based pages: [0,1,2,4] -> "1-3, 5". */
    fun describe(pages: List<Int>): String {
        if (pages.isEmpty()) return ""
        val sorted = pages.sorted()
        val parts = mutableListOf<String>()
        var start = sorted.first()
        var prev = start
        for (p in sorted.drop(1) + Int.MIN_VALUE) {
            if (p == prev + 1) {
                prev = p
                continue
            }
            parts += if (start == prev) "${start + 1}" else "${start + 1}-${prev + 1}"
            start = p
            prev = p
        }
        return parts.joinToString(", ")
    }
}

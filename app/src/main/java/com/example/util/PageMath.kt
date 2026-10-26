package com.example.util

import kotlin.math.max
import kotlin.math.min

/** Notes area next to a page in "Spazio per appunti". */
enum class NotesSide(val label: String) { RIGHT("A destra"), BOTTOM("Sotto") }

/** What is drawn in the notes area. */
enum class NotesStyle(val label: String) { LINES("Righe"), GRID("Quadretti"), BLANK("Vuoto") }

/**
 * Pure geometry behind the page tools (N-up, notes, crop, duplex, blank pages), in PDF points
 * with the origin at the bottom-left, so it can be unit tested without PDFBox.
 */
object PageMath {

    data class Box(val x: Float, val y: Float, val w: Float, val h: Float)

    const val A4_SHORT = 595.28f
    const val A4_LONG = 841.89f

    /** Sheet for [perSheet] pages: 2 → landscape A4 (side by side), 4 → portrait A4 (2×2). */
    fun nUpSheet(perSheet: Int): Pair<Float, Float> = if (perSheet == 2) A4_LONG to A4_SHORT else A4_SHORT to A4_LONG

    /** Slots in reading order (left to right, top to bottom). */
    fun nUpSlots(perSheet: Int, sheetW: Float, sheetH: Float, margin: Float, gap: Float): List<Box> {
        val innerW = sheetW - 2 * margin
        val innerH = sheetH - 2 * margin
        return when (perSheet) {
            2 -> {
                val w = (innerW - gap) / 2f
                listOf(Box(margin, margin, w, innerH), Box(margin + w + gap, margin, w, innerH))
            }
            4 -> {
                val w = (innerW - gap) / 2f
                val h = (innerH - gap) / 2f
                listOf(
                    Box(margin, margin + h + gap, w, h),
                    Box(margin + w + gap, margin + h + gap, w, h),
                    Box(margin, margin, w, h),
                    Box(margin + w + gap, margin, w, h)
                )
            }
            else -> listOf(Box(margin, margin, innerW, innerH))
        }
    }

    /** Largest [w]×[h] rectangle that fits [box], centred. */
    fun fit(w: Float, h: Float, box: Box): Box {
        val s = min(box.w / w, box.h / h)
        val dw = w * s
        val dh = h * s
        return Box(box.x + (box.w - dw) / 2f, box.y + (box.h - dh) / 2f, dw, dh)
    }

    /** Size of a page as shown (the /Rotate entry swaps width and height at 90° and 270°). */
    fun displayedSize(rotation: Int, w: Float, h: Float): Pair<Float, Float> =
        if (normalized(rotation) % 180 == 90) h to w else w to h

    /**
     * Matrix (a, b, c, d, e, f) that draws a page whose visible box starts at ([llx], [lly]) and is
     * [w]×[h] in its own space, rotated by its /Rotate entry, scaled into [target] (displayed size).
     */
    fun placement(rotation: Int, llx: Float, lly: Float, w: Float, h: Float, target: Box): FloatArray {
        val (dw, _) = displayedSize(rotation, w, h)
        val s = target.w / dw
        val ox = target.x
        val oy = target.y
        return when (normalized(rotation)) {
            90 -> floatArrayOf(0f, -s, s, 0f, ox - s * lly, oy + s * (w + llx))
            180 -> floatArrayOf(-s, 0f, 0f, -s, ox + s * (w + llx), oy + s * (h + lly))
            270 -> floatArrayOf(0f, s, -s, 0f, ox + s * (h + lly), oy - s * llx)
            else -> floatArrayOf(s, 0f, 0f, s, ox - s * llx, oy - s * lly)
        }
    }

    /** Applies matrix [m] (a, b, c, d, e, f) to a point. */
    fun apply(m: FloatArray, x: Float, y: Float): Pair<Float, Float> = (m[0] * x + m[2] * y + m[4]) to (m[1] * x + m[3] * y + m[5])

    /**
     * Page and notes areas on a sheet the size of the page ([sheetW]×[sheetH]): the page keeps its
     * proportions in ~62% of the sheet, the rest is for notes.
     */
    fun notesLayout(sheetW: Float, sheetH: Float, side: NotesSide, margin: Float): Pair<Box, Box> {
        val gap = margin
        return if (side == NotesSide.RIGHT) {
            val pageArea = Box(margin, margin, (sheetW - 2 * margin - gap) * 0.62f, sheetH - 2 * margin)
            val page = fit(sheetW, sheetH, pageArea).let { it.copy(y = sheetH - margin - it.h) }
            val notesX = page.x + page.w + gap
            page to Box(notesX, margin, sheetW - margin - notesX, sheetH - 2 * margin)
        } else {
            val pageArea = Box(margin, sheetH - margin - (sheetH - 2 * margin - gap) * 0.62f, sheetW - 2 * margin, (sheetH - 2 * margin - gap) * 0.62f)
            val page = fit(sheetW, sheetH, pageArea)
            page to Box(margin, margin, sheetW - 2 * margin, page.y - gap - margin)
        }
    }

    /**
     * Page order when merging separately scanned fronts and backs: (0 = fronts / 1 = backs, page).
     * Backs scanned by turning the stack over come in reverse order.
     */
    fun duplexOrder(fronts: Int, backs: Int, backsReversed: Boolean): List<Pair<Int, Int>> {
        val result = ArrayList<Pair<Int, Int>>(fronts + backs)
        for (i in 0 until max(fronts, backs)) {
            if (i < fronts) result += 0 to i
            if (i < backs) result += 1 to (if (backsReversed) backs - 1 - i else i)
        }
        return result
    }

    /**
     * Margins chosen on the page as shown (left, top, right, bottom, fractions) expressed in the
     * page's own unrotated space as (left, bottom, right, top).
     */
    fun cropInBoxSpace(rotation: Int, left: Float, top: Float, right: Float, bottom: Float): FloatArray = when (normalized(rotation)) {
        90 -> floatArrayOf(top, left, bottom, right)
        180 -> floatArrayOf(right, top, left, bottom)
        270 -> floatArrayOf(bottom, right, top, left)
        else -> floatArrayOf(left, bottom, right, top)
    }

    /** Share of "ink" (pixels darker than [threshold] luma) in the page, ignoring a 4% border (scanner shadows). */
    fun inkRatio(pixels: IntArray, width: Int, height: Int, threshold: Int = 200): Float {
        if (width <= 0 || height <= 0) return 0f
        val bx = (width * 0.04f).toInt()
        val by = (height * 0.04f).toInt()
        var ink = 0
        var total = 0
        for (y in by until height - by) {
            for (x in bx until width - bx) {
                total++
                if (luma(pixels[y * width + x]) < threshold) ink++
            }
        }
        return if (total == 0) 0f else ink.toFloat() / total
    }

    /** Empty margins around the content as fractions (left, top, right, bottom), or null for an empty page. */
    fun contentMargins(pixels: IntArray, width: Int, height: Int, threshold: Int = 200): FloatArray? {
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (luma(pixels[y * width + x]) < threshold) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < 0) return null
        return floatArrayOf(
            minX.toFloat() / width,
            minY.toFloat() / height,
            (width - 1 - maxX).toFloat() / width,
            (height - 1 - maxY).toFloat() / height
        )
    }

    private fun luma(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    private fun normalized(rotation: Int): Int = ((rotation % 360) + 360) % 360
}

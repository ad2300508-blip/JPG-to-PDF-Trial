package com.example.util

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Output shapes of "Formato social". */
enum class SocialAspect(val label: String, val hint: String, val w: Int, val h: Int) {
    SQUARE("1:1", "Post quadrato", 1, 1),
    PORTRAIT("4:5", "Post verticale", 4, 5),
    STORY("9:16", "Storia o reel", 9, 16),
    WIDE("16:9", "Video, copertina", 16, 9),
    CLASSIC("3:2", "Foto classica", 3, 2);

    val ratio: Float get() = w.toFloat() / h
}

/** Where the text goes in "Filigrana foto". */
enum class WatermarkPosition(val label: String) {
    TOP_LEFT("In alto a sinistra"),
    TOP_RIGHT("In alto a destra"),
    CENTER("Al centro"),
    BOTTOM_LEFT("In basso a sinistra"),
    BOTTOM_RIGHT("In basso a destra"),
    TILED("Ripetuta")
}

/** Pure layout and colour maths for the photo tools (unit tested). */
object PhotoMath {

    data class Rect(val x: Float, val y: Float, val w: Float, val h: Float)

    /** Canvas for [aspect] whose long side is [longSide] px. */
    fun canvasFor(aspect: SocialAspect, longSide: Int): Pair<Int, Int> =
        if (aspect.ratio >= 1f) longSide to (longSide / aspect.ratio).roundToInt() else (longSide * aspect.ratio).roundToInt() to longSide

    /** [srcW]×[srcH] fitted inside [boxW]×[boxH] minus [inset] on every side, centred ("contain"). */
    fun contain(srcW: Float, srcH: Float, boxW: Float, boxH: Float, inset: Float = 0f): Rect {
        val w = (boxW - 2 * inset).coerceAtLeast(1f)
        val h = (boxH - 2 * inset).coerceAtLeast(1f)
        val s = min(w / srcW, h / srcH)
        val dw = srcW * s
        val dh = srcH * s
        return Rect((boxW - dw) / 2f, (boxH - dh) / 2f, dw, dh)
    }

    /** [srcW]×[srcH] scaled to cover [boxW]×[boxH] entirely, centred ("cover"). */
    fun cover(srcW: Float, srcH: Float, boxW: Float, boxH: Float): Rect {
        val s = max(boxW / srcW, boxH / srcH)
        val dw = srcW * s
        val dh = srcH * s
        return Rect((boxW - dw) / 2f, (boxH - dh) / 2f, dw, dh)
    }

    /** Size after turning a [w]×[h] image by [degrees] (multiples of 90). */
    fun rotatedSize(w: Int, h: Int, degrees: Int): Pair<Int, Int> = if (((degrees % 360) + 360) % 180 == 90) h to w else w to h

    /** Top-left corner of a [textW]×[textH] label at [position] on a [w]×[h] image. */
    fun watermarkOrigin(position: WatermarkPosition, w: Float, h: Float, textW: Float, textH: Float, margin: Float): Pair<Float, Float> = when (position) {
        WatermarkPosition.TOP_LEFT -> margin to margin
        WatermarkPosition.TOP_RIGHT -> (w - margin - textW) to margin
        WatermarkPosition.BOTTOM_LEFT -> margin to (h - margin - textH)
        WatermarkPosition.BOTTOM_RIGHT -> (w - margin - textW) to (h - margin - textH)
        WatermarkPosition.CENTER, WatermarkPosition.TILED -> ((w - textW) / 2f) to ((h - textH) / 2f)
    }

    /** "#1A2B3C" for an ARGB colour. */
    fun hex(argb: Int): String = String.format("#%06X", argb and 0xFFFFFF)

    /** Perceived brightness 0..255 (used to pick black or white text on a swatch). */
    fun luma(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    /**
     * Up to [count] dominant colours of [pixels] with their share (0..1), largest first.
     * Deterministic k-means: centres start from evenly spaced brightness ranks.
     */
    fun palette(pixels: IntArray, count: Int = 6, iterations: Int = 10): List<Pair<Int, Float>> {
        val samples = pixels.filter { (it ushr 24) > 0x80 }
        if (samples.isEmpty()) return emptyList()
        val sorted = samples.sortedBy { luma(it) }
        val k = min(count, samples.toSet().size)
        val centers = Array(k) { i ->
            val c = sorted[((i + 0.5f) / k * sorted.size).toInt().coerceIn(0, sorted.lastIndex)]
            floatArrayOf(((c shr 16) and 0xFF).toFloat(), ((c shr 8) and 0xFF).toFloat(), (c and 0xFF).toFloat())
        }
        val assignment = IntArray(samples.size)
        repeat(iterations) {
            for ((i, c) in samples.withIndex()) {
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                var best = 0
                var bestD = Float.MAX_VALUE
                for (j in 0 until k) {
                    val dr = r - centers[j][0]
                    val dg = g - centers[j][1]
                    val db = b - centers[j][2]
                    val d = dr * dr * 0.3f + dg * dg * 0.59f + db * db * 0.11f
                    if (d < bestD) {
                        bestD = d
                        best = j
                    }
                }
                assignment[i] = best
            }
            val sums = Array(k) { DoubleArray(4) }
            for ((i, c) in samples.withIndex()) {
                val s = sums[assignment[i]]
                s[0] += ((c shr 16) and 0xFF).toDouble()
                s[1] += ((c shr 8) and 0xFF).toDouble()
                s[2] += (c and 0xFF).toDouble()
                s[3] += 1.0
            }
            for (j in 0 until k) {
                if (sums[j][3] > 0) {
                    centers[j][0] = (sums[j][0] / sums[j][3]).toFloat()
                    centers[j][1] = (sums[j][1] / sums[j][3]).toFloat()
                    centers[j][2] = (sums[j][2] / sums[j][3]).toFloat()
                }
            }
        }
        val counts = IntArray(k)
        assignment.forEach { counts[it]++ }
        return (0 until k)
            .filter { counts[it] > 0 }
            .map { j ->
                val color = (0xFF shl 24) or
                    (centers[j][0].roundToInt().coerceIn(0, 255) shl 16) or
                    (centers[j][1].roundToInt().coerceIn(0, 255) shl 8) or
                    centers[j][2].roundToInt().coerceIn(0, 255)
                color to counts[j].toFloat() / samples.size
            }
            .sortedByDescending { it.second }
    }
}

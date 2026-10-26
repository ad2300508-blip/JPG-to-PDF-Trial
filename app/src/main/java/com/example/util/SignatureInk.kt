package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

data class InkPoint(val x: Float, val y: Float)

/** Finger-drawn signature strokes: bounds and rendering to a transparent bitmap for the PDF. */
object SignatureInk {

    /** left, top, right, bottom of all points, or null when nothing was drawn. */
    fun bounds(strokes: List<List<InkPoint>>): FloatArray? {
        val points = strokes.flatten()
        if (points.isEmpty()) return null
        return floatArrayOf(points.minOf { it.x }, points.minOf { it.y }, points.maxOf { it.x }, points.maxOf { it.y })
    }

    /** Width / height of the drawn area including the stroke, used to size the signature on the page. */
    fun aspect(strokes: List<List<InkPoint>>, strokeWidth: Float): Float {
        val b = bounds(strokes) ?: return 3f
        val w = (b[2] - b[0]) + strokeWidth * 2
        val h = (b[3] - b[1]) + strokeWidth * 2
        return (w / h.coerceAtLeast(1f)).coerceIn(0.2f, 12f)
    }

    /** Smoothed path through the stroke points (quadratic curves between midpoints). */
    fun path(stroke: List<InkPoint>, dx: Float = 0f, dy: Float = 0f, scale: Float = 1f): Path = Path().apply {
        if (stroke.isEmpty()) return@apply
        fun px(p: InkPoint) = (p.x + dx) * scale
        fun py(p: InkPoint) = (p.y + dy) * scale
        moveTo(px(stroke[0]), py(stroke[0]))
        if (stroke.size == 1) {
            lineTo(px(stroke[0]) + 0.1f, py(stroke[0]))
            return@apply
        }
        for (i in 1 until stroke.size) {
            val prev = stroke[i - 1]
            val cur = stroke[i]
            quadTo(px(prev), py(prev), (px(prev) + px(cur)) / 2f, (py(prev) + py(cur)) / 2f)
        }
        lineTo(px(stroke.last()), py(stroke.last()))
    }

    /** Renders the strokes cropped to their bounds, at up to [maxWidth] pixels, on a transparent background. */
    fun render(strokes: List<List<InkPoint>>, color: Int, strokeWidth: Float, maxWidth: Int = 1400): Bitmap? {
        val b = bounds(strokes) ?: return null
        val pad = strokeWidth * 1.5f
        val w = (b[2] - b[0]) + pad * 2
        val h = (b[3] - b[1]) + pad * 2
        val scale = (maxWidth / w).coerceIn(1f, 4f)
        val bitmap = Bitmap.createBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.strokeWidth = strokeWidth * scale
        }
        val canvas = Canvas(bitmap)
        strokes.forEach { canvas.drawPath(path(it, pad - b[0], pad - b[1], scale), paint) }
        return bitmap
    }
}

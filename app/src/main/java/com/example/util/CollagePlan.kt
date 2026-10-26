package com.example.util

import kotlin.math.ceil
import kotlin.math.sqrt

enum class CollageLayout(val label: String) {
    COLUMN("Colonna"),
    ROW("Riga"),
    GRID("Griglia")
}

/** One photo's place on the collage, in the plan's own units. */
data class CollageCell(val x: Float, val y: Float, val width: Float, val height: Float)

/** Size of the whole collage plus where each photo goes (same order as the input aspects). */
data class CollagePlan(val width: Float, val height: Float, val cells: List<CollageCell>)

/**
 * Pure layout math shared by the live preview and the renderer.
 * Units are arbitrary: the side every photo shares is [UNIT] long, [gap] is a fraction of it.
 * COLUMN and ROW keep each photo's proportions; GRID uses square cells (photos are center-cropped)
 * and centers an incomplete last row.
 */
object CollagePlanner {
    const val UNIT = 1000f

    fun plan(layout: CollageLayout, aspects: List<Float>, gap: Float): CollagePlan {
        if (aspects.isEmpty()) return CollagePlan(UNIT, UNIT, emptyList())
        val g = gap.coerceIn(0f, 0.5f) * UNIT
        val safe = aspects.map { if (it.isFinite() && it > 0f) it else 1f }
        return when (layout) {
            CollageLayout.COLUMN -> {
                var y = g
                val cells = safe.map { aspect ->
                    val h = UNIT / aspect
                    CollageCell(g, y, UNIT, h).also { y += h + g }
                }
                CollagePlan(UNIT + 2 * g, y, cells)
            }
            CollageLayout.ROW -> {
                var x = g
                val cells = safe.map { aspect ->
                    val w = UNIT * aspect
                    CollageCell(x, g, w, UNIT).also { x += w + g }
                }
                CollagePlan(x, UNIT + 2 * g, cells)
            }
            CollageLayout.GRID -> {
                val count = safe.size
                val cols = gridColumns(count)
                val rows = ceil(count / cols.toFloat()).toInt()
                val width = cols * UNIT + (cols + 1) * g
                val cells = (0 until count).map { i ->
                    val row = i / cols
                    val col = i % cols
                    val inRow = if (row == rows - 1) count - row * cols else cols
                    val offset = (cols - inRow) * (UNIT + g) / 2f
                    CollageCell(offset + g + col * (UNIT + g), g + row * (UNIT + g), UNIT, UNIT)
                }
                CollagePlan(width, rows * UNIT + (rows + 1) * g, cells)
            }
        }
    }

    fun gridColumns(count: Int): Int = ceil(sqrt(count.coerceAtLeast(1).toDouble())).toInt()

    /** Scale factor that makes the plan's longest side [maxSide] px. */
    fun scaleFor(plan: CollagePlan, maxSide: Int): Float = maxSide / maxOf(plan.width, plan.height)
}

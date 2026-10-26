package com.example

import com.example.util.CollageLayout
import com.example.util.CollagePlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollagePlanTest {

    private val unit = CollagePlanner.UNIT

    @Test
    fun columnStacksPhotosKeepingProportions() {
        val plan = CollagePlanner.plan(CollageLayout.COLUMN, listOf(2f, 0.5f), gap = 0.1f)
        val g = 0.1f * unit
        assertEquals(unit + 2 * g, plan.width, 0.01f)
        assertEquals(g + unit / 2f + g + unit * 2f + g, plan.height, 0.01f)
        assertEquals(g, plan.cells[0].y, 0.01f)
        assertEquals(g + unit / 2f + g, plan.cells[1].y, 0.01f)
        assertEquals(unit * 2f, plan.cells[1].height, 0.01f)
    }

    @Test
    fun rowPlacesPhotosSideBySide() {
        val plan = CollagePlanner.plan(CollageLayout.ROW, listOf(1.5f, 1f), gap = 0f)
        assertEquals(2.5f * unit, plan.width, 0.01f)
        assertEquals(unit, plan.height, 0.01f)
        assertEquals(1.5f * unit, plan.cells[1].x, 0.01f)
    }

    @Test
    fun gridCentersTheIncompleteLastRow() {
        assertEquals(2, CollagePlanner.gridColumns(3))
        assertEquals(3, CollagePlanner.gridColumns(7))
        val plan = CollagePlanner.plan(CollageLayout.GRID, listOf(1f, 1f, 1f), gap = 0f)
        assertEquals(2 * unit, plan.width, 0.01f)
        assertEquals(2 * unit, plan.height, 0.01f)
        // Third photo alone on row two, centered.
        assertEquals(unit / 2f, plan.cells[2].x, 0.01f)
        assertEquals(unit, plan.cells[2].y, 0.01f)
    }

    @Test
    fun badAspectsFallBackToSquareAndScaleFitsLongestSide() {
        val plan = CollagePlanner.plan(CollageLayout.COLUMN, listOf(0f, Float.NaN), gap = 0f)
        assertEquals(2 * unit, plan.height, 0.01f)
        assertEquals(0.5f, CollagePlanner.scaleFor(plan, 1000), 0.0001f)
        assertTrue(CollagePlanner.plan(CollageLayout.GRID, emptyList(), 0.05f).cells.isEmpty())
    }
}

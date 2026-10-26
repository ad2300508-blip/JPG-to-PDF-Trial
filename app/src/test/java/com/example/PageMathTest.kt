package com.example

import com.example.util.NotesSide
import com.example.util.PageMath
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageMathTest {

    private fun assertPoint(expected: Pair<Float, Float>, actual: Pair<Float, Float>) {
        assertEquals(expected.first, actual.first, 0.01f)
        assertEquals(expected.second, actual.second, 0.01f)
    }

    @Test
    fun placementMapsCornersForEveryRotation() {
        // A 100×200 page whose box starts at (10, 20), drawn into a slot at (5, 7).
        val llx = 10f
        val lly = 20f
        for (rotation in listOf(0, 90, 180, 270)) {
            val (dw, dh) = PageMath.displayedSize(rotation, 100f, 200f)
            val target = PageMath.Box(5f, 7f, dw * 2f, dh * 2f)
            val m = PageMath.placement(rotation, llx, lly, 100f, 200f, target)
            val bottomLeft = PageMath.apply(m, llx, lly)
            val topRight = PageMath.apply(m, llx + 100f, lly + 200f)
            when (rotation) {
                0 -> { assertPoint(5f to 7f, bottomLeft); assertPoint(205f to 407f, topRight) }
                // Rotated clockwise: the page's bottom-left is shown top-left, its top-right bottom-right.
                90 -> { assertPoint(5f to 207f, bottomLeft); assertPoint(405f to 7f, topRight) }
                180 -> { assertPoint(205f to 407f, bottomLeft); assertPoint(5f to 7f, topRight) }
                270 -> { assertPoint(405f to 7f, bottomLeft); assertPoint(5f to 207f, topRight) }
            }
        }
    }

    @Test
    fun nUpSlotsAndFit() {
        val (w, h) = PageMath.nUpSheet(2)
        assertTrue(w > h)
        val two = PageMath.nUpSlots(2, w, h, margin = 20f, gap = 10f)
        assertEquals(2, two.size)
        assertTrue(two[0].x < two[1].x)
        val four = PageMath.nUpSlots(4, 600f, 800f, margin = 0f, gap = 0f)
        assertEquals(listOf(0f, 300f, 0f, 300f), four.map { it.x })
        assertEquals(listOf(400f, 400f, 0f, 0f), four.map { it.y })
        val fitted = PageMath.fit(100f, 200f, PageMath.Box(0f, 0f, 400f, 400f))
        assertEquals(PageMath.Box(100f, 0f, 200f, 400f), fitted)
    }

    @Test
    fun duplexInterleavesReversedBacks() {
        assertEquals(listOf(0 to 0, 1 to 2, 0 to 1, 1 to 1, 0 to 2, 1 to 0), PageMath.duplexOrder(3, 3, backsReversed = true))
        assertEquals(listOf(0 to 0, 1 to 0, 0 to 1), PageMath.duplexOrder(2, 1, backsReversed = false))
    }

    @Test
    fun cropMarginsFollowRotation() {
        assertArrayEquals(floatArrayOf(0.1f, 0.4f, 0.3f, 0.2f), PageMath.cropInBoxSpace(0, 0.1f, 0.2f, 0.3f, 0.4f), 0f)
        assertArrayEquals(floatArrayOf(0.2f, 0.1f, 0.4f, 0.3f), PageMath.cropInBoxSpace(90, 0.1f, 0.2f, 0.3f, 0.4f), 0f)
        assertArrayEquals(floatArrayOf(0.3f, 0.2f, 0.1f, 0.4f), PageMath.cropInBoxSpace(180, 0.1f, 0.2f, 0.3f, 0.4f), 0f)
        assertArrayEquals(floatArrayOf(0.4f, 0.3f, 0.2f, 0.1f), PageMath.cropInBoxSpace(-90, 0.1f, 0.2f, 0.3f, 0.4f), 0f)
    }

    @Test
    fun inkAndContentMargins() {
        val w = 50
        val h = 50
        val white = IntArray(w * h) { 0xFFFFFFFF.toInt() }
        assertEquals(0f, PageMath.inkRatio(white, w, h), 0f)
        assertNull(PageMath.contentMargins(white, w, h))
        val page = white.copyOf()
        for (y in 10..19) for (x in 20..29) page[y * w + x] = 0xFF000000.toInt()
        assertTrue(PageMath.inkRatio(page, w, h) > 0.03f)
        assertArrayEquals(floatArrayOf(0.4f, 0.2f, 0.4f, 0.6f), PageMath.contentMargins(page, w, h)!!, 0.001f)
    }

    @Test
    fun notesLeaveRoomBesideOrBelow() {
        val (page, notes) = PageMath.notesLayout(595f, 842f, NotesSide.RIGHT, margin = 20f)
        assertTrue(notes.x >= page.x + page.w)
        assertTrue(notes.w > 150f)
        val (pageB, notesB) = PageMath.notesLayout(595f, 842f, NotesSide.BOTTOM, margin = 20f)
        assertTrue(notesB.y + notesB.h <= pageB.y)
        assertTrue(notesB.h > 200f)
    }
}

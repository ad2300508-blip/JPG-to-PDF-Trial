package com.example

import com.example.util.PhotoFilter
import com.example.util.PhotoFilterMath
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoFiltersTest {

    @Test
    fun everyFilterIsAFullColourMatrix() {
        PhotoFilter.entries.forEach { assertEquals(it.label, 20, it.matrix.size) }
        assertEquals(Triple(10, 120, 250), PhotoFilterMath.apply(PhotoFilter.ORIGINAL.matrix, 10, 120, 250))
    }

    @Test
    fun monoTurnsColoursIntoTheirLuma() {
        val (r, g, b) = PhotoFilterMath.apply(PhotoFilter.MONO.matrix, 255, 0, 0)
        assertEquals(54, r)
        assertEquals(r, g)
        assertEquals(r, b)
    }

    @Test
    fun sepiaWarmsGreyAndFadeLiftsBlacks() {
        val (r, g, b) = PhotoFilterMath.apply(PhotoFilter.SEPIA.matrix, 128, 128, 128)
        assertTrue(r > g && g > b)
        val black = PhotoFilterMath.apply(PhotoFilter.FADE.matrix, 0, 0, 0)
        assertTrue(black.first > 20 && black.third > black.first)
    }

    @Test
    fun lerpBlendsBetweenMatrices() {
        val to = PhotoFilter.NOIR.matrix
        assertArrayEquals(PhotoFilterMath.IDENTITY, PhotoFilterMath.lerp(PhotoFilterMath.IDENTITY, to, 0f), 0f)
        assertArrayEquals(to, PhotoFilterMath.lerp(PhotoFilterMath.IDENTITY, to, 1f), 0.0001f)
        val half = PhotoFilterMath.lerp(PhotoFilterMath.IDENTITY, to, 0.5f)
        assertEquals((1f + to[0]) / 2f, half[0], 0.0001f)
        assertArrayEquals(to, PhotoFilterMath.lerp(PhotoFilterMath.IDENTITY, to, 7f), 0.0001f)
    }
}

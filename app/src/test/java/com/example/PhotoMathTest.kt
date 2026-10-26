package com.example

import com.example.util.PhotoMath
import com.example.util.SocialAspect
import com.example.util.WatermarkPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoMathTest {

    @Test
    fun socialCanvasesKeepTheirShape() {
        assertEquals(1080 to 1080, PhotoMath.canvasFor(SocialAspect.SQUARE, 1080))
        assertEquals(864 to 1080, PhotoMath.canvasFor(SocialAspect.PORTRAIT, 1080))
        assertEquals(1920 to 1080, PhotoMath.canvasFor(SocialAspect.WIDE, 1920))
        assertEquals(1080 to 1920, PhotoMath.canvasFor(SocialAspect.STORY, 1920))
    }

    @Test
    fun containAndCover() {
        assertEquals(PhotoMath.Rect(0f, 25f, 100f, 50f), PhotoMath.contain(200f, 100f, 100f, 100f))
        assertEquals(PhotoMath.Rect(10f, 30f, 80f, 40f), PhotoMath.contain(200f, 100f, 100f, 100f, inset = 10f))
        assertEquals(PhotoMath.Rect(-50f, 0f, 200f, 100f), PhotoMath.cover(200f, 100f, 100f, 100f))
        assertEquals(30 to 20, PhotoMath.rotatedSize(20, 30, 90))
        assertEquals(20 to 30, PhotoMath.rotatedSize(20, 30, -180))
    }

    @Test
    fun watermarkCorners() {
        assertEquals(10f to 10f, PhotoMath.watermarkOrigin(WatermarkPosition.TOP_LEFT, 200f, 100f, 50f, 20f, 10f))
        assertEquals(140f to 70f, PhotoMath.watermarkOrigin(WatermarkPosition.BOTTOM_RIGHT, 200f, 100f, 50f, 20f, 10f))
        assertEquals(75f to 40f, PhotoMath.watermarkOrigin(WatermarkPosition.CENTER, 200f, 100f, 50f, 20f, 10f))
    }

    @Test
    fun paletteFindsTheMainColoursBySize() {
        val red = 0xFFE53935.toInt()
        val blue = 0xFF1E88E5.toInt()
        val pixels = IntArray(300) { if (it < 200) red else blue }
        val palette = PhotoMath.palette(pixels, count = 2)
        assertEquals(2, palette.size)
        assertEquals("#E53935", PhotoMath.hex(palette[0].first))
        assertEquals(2f / 3f, palette[0].second, 0.01f)
        assertEquals("#1E88E5", PhotoMath.hex(palette[1].first))
        assertTrue(PhotoMath.palette(IntArray(0)).isEmpty())
        assertTrue(PhotoMath.luma(0xFFFFFFFF.toInt()) > 250)
    }
}

package com.example

import com.example.service.GifSequence
import com.example.util.GifPalette
import com.example.util.GifWriter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class GifEncoderTest {

    @Test
    fun framesRoundTripThroughAReferenceDecoder() {
        val w = 300
        val h = 200
        val random = Random(1)
        val frames = listOf(
            // Noise fills the 4096-code table and forces a clear code mid-stream.
            ByteArray(w * h) { random.nextInt(252).toByte() },
            ByteArray(w * h) { i -> (((i % w) / 7 + (i / w) / 5) % 252).toByte() },
            ByteArray(w * h) { 5 }
        )
        val bytes = ByteArrayOutputStream().also { out ->
            val writer = GifWriter(out, w, h, loopForever = true)
            frames.forEach { writer.addFrame(it, 500) }
            writer.finish()
        }.toByteArray()

        val decoded = decodeFrames(bytes)
        assertEquals(frames.size, decoded.size)
        frames.indices.forEach { assertArrayEquals("frame $it", frames[it], decoded[it]) }
        assertEquals("GIF89a", String(bytes, 0, 6, Charsets.US_ASCII))
        assertEquals(0x3B, bytes.last().toInt())
    }

    @Test
    fun paletteMapsPrimaryColoursExactly() {
        val px = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF0000.toInt(), 0xFF0000FF.toInt())
        val idx = GifPalette.quantize(px, width = 4, dither = false)
        assertEquals(GifPalette.index(0, 0, 0), idx[0].toInt() and 0xFF)
        assertEquals(GifPalette.index(5, 6, 5), idx[1].toInt() and 0xFF)
        assertEquals(GifPalette.index(5, 0, 0), idx[2].toInt() and 0xFF)
        assertEquals(GifPalette.index(0, 0, 5), idx[3].toInt() and 0xFF)
        assertTrue(GifPalette.index(5, 6, 5) < 256)
    }

    @Test
    fun boomerangOrderAndCanvasSize() {
        assertEquals(listOf(0, 1, 2, 3, 2, 1), GifSequence.order(4, boomerang = true))
        assertEquals(listOf(0, 1), GifSequence.order(2, boomerang = true))
        assertEquals(listOf(0, 1, 2), GifSequence.order(3, boomerang = false))
        assertEquals(480 to 320, GifSequence.canvas(1.5f, 480))
        assertEquals(240 to 480, GifSequence.canvas(0.5f, 480))
        assertEquals(480 to 480, GifSequence.canvas(Float.NaN, 480))
    }

    /** Small GIF decoder (global palette only), enough to read back what GifWriter writes. */
    private fun decodeFrames(b: ByteArray): List<ByteArray> {
        fun u8(i: Int) = b[i].toInt() and 0xFF
        fun u16(i: Int) = u8(i) or (u8(i + 1) shl 8)
        val w = u16(6)
        val h = u16(8)
        var p = 13 + 3 * (2 shl (u8(10) and 7))
        val frames = mutableListOf<ByteArray>()
        while (u8(p) != 0x3B) {
            when (u8(p)) {
                0x21 -> {
                    p += 2
                    while (u8(p) != 0) p += u8(p) + 1
                    p++
                }
                0x2C -> {
                    p += 10
                    val minCode = u8(p++)
                    val data = ByteArrayOutputStream()
                    while (u8(p) != 0) {
                        data.write(b, p + 1, u8(p))
                        p += u8(p) + 1
                    }
                    p++
                    frames += lzwDecode(data.toByteArray(), minCode, w * h)
                }
                else -> error("Blocco sconosciuto ${u8(p)}")
            }
        }
        return frames
    }

    private fun lzwDecode(data: ByteArray, minCode: Int, size: Int): ByteArray {
        val clear = 1 shl minCode
        val end = clear + 1
        val out = ByteArrayOutputStream()
        val dict = ArrayList<ByteArray>()
        fun reset() {
            dict.clear()
            for (i in 0 until clear) dict.add(byteArrayOf(i.toByte()))
            dict.add(ByteArray(0))
            dict.add(ByteArray(0))
        }
        reset()
        var codeSize = minCode + 1
        var bitPos = 0
        var prev: ByteArray? = null
        while (true) {
            var code = 0
            for (i in 0 until codeSize) {
                val byte = bitPos / 8
                if (byte >= data.size) return out.toByteArray().copyOf(size)
                if ((data[byte].toInt() shr (bitPos % 8)) and 1 == 1) code = code or (1 shl i)
                bitPos++
            }
            if (code == clear) {
                reset()
                codeSize = minCode + 1
                prev = null
                continue
            }
            if (code == end) break
            val last = prev
            val entry = when {
                code < dict.size -> dict[code]
                last != null -> last + last[0]
                else -> error("Codice non valido")
            }
            out.write(entry)
            if (last != null && dict.size < 4096) dict.add(last + entry[0])
            prev = entry
            if (dict.size == (1 shl codeSize) && codeSize < 12) codeSize++
        }
        return out.toByteArray().copyOf(size)
    }
}

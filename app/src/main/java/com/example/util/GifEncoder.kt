package com.example.util

import java.io.OutputStream

/**
 * Minimal animated GIF89a writer in pure Kotlin (no Android types, so it is unit-tested on the JVM).
 *
 * Every frame shares one fixed 6×7×6 colour palette (252 colours) and is mapped to it with a 4×4
 * ordered dither, which keeps photos smooth without per-frame palettes and keeps memory flat:
 * frames are written one at a time.
 */
class GifWriter(
    private val out: OutputStream,
    private val width: Int,
    private val height: Int,
    loopForever: Boolean
) {
    init {
        require(width in 1..65535 && height in 1..65535) { "Dimensioni GIF non valide" }
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
        writeShort(width)
        writeShort(height)
        out.write(0xF7) // global colour table, 8 bits per channel, 256 entries
        out.write(0) // background colour index
        out.write(0) // pixel aspect ratio
        out.write(GifPalette.table)
        if (loopForever) {
            out.write(byteArrayOf(0x21, 0xFF.toByte(), 0x0B))
            out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
            out.write(byteArrayOf(0x03, 0x01, 0x00, 0x00, 0x00)) // loop count 0 = forever
        }
    }

    /** Adds one frame of palette indices (see [GifPalette.quantize]) shown for [delayMs]. */
    fun addFrame(indices: ByteArray, delayMs: Int) {
        require(indices.size == width * height) { "Frame di dimensioni diverse" }
        // Graphic control extension: no transparency, frame delay in 1/100 s.
        out.write(byteArrayOf(0x21, 0xF9.toByte(), 0x04, 0x04))
        writeShort((delayMs / 10).coerceIn(2, 65535))
        out.write(byteArrayOf(0x00, 0x00))
        // Image descriptor covering the whole canvas, no local colour table.
        out.write(0x2C)
        writeShort(0)
        writeShort(0)
        writeShort(width)
        writeShort(height)
        out.write(0)
        out.write(GifLzw.MIN_CODE_SIZE)
        GifLzw.encode(indices, out)
        out.write(0) // block terminator
    }

    fun finish() {
        out.write(0x3B)
        out.flush()
    }

    private fun writeShort(value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }
}

object GifPalette {
    private const val R_LEVELS = 6
    private const val G_LEVELS = 7
    private const val B_LEVELS = 6

    /** 256 RGB triplets; entries past the 252 real colours are black. */
    val table: ByteArray = ByteArray(256 * 3).also { t ->
        for (r in 0 until R_LEVELS) for (g in 0 until G_LEVELS) for (b in 0 until B_LEVELS) {
            val i = index(r, g, b) * 3
            t[i] = (r * 255 / (R_LEVELS - 1)).toByte()
            t[i + 1] = (g * 255 / (G_LEVELS - 1)).toByte()
            t[i + 2] = (b * 255 / (B_LEVELS - 1)).toByte()
        }
    }

    private val bayer = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

    fun index(r: Int, g: Int, b: Int): Int = r * G_LEVELS * B_LEVELS + g * B_LEVELS + b

    /** Maps ARGB pixels (row-major, [width] per row) to palette indices with ordered dithering. */
    fun quantize(argb: IntArray, width: Int, dither: Boolean = true): ByteArray {
        val outIdx = ByteArray(argb.size)
        for (p in argb.indices) {
            val c = argb[p]
            // Threshold in 0..1, centred so the average brightness is unchanged.
            val t = if (dither) (bayer[((p / width) and 3) * 4 + ((p % width) and 3)] + 0.5f) / 16f else 0.5f
            val r = level((c shr 16) and 0xFF, R_LEVELS, t)
            val g = level((c shr 8) and 0xFF, G_LEVELS, t)
            val b = level(c and 0xFF, B_LEVELS, t)
            outIdx[p] = index(r, g, b).toByte()
        }
        return outIdx
    }

    private fun level(channel: Int, levels: Int, threshold: Float): Int {
        val scaled = channel * (levels - 1) / 255f
        val base = scaled.toInt()
        return if (scaled - base > threshold) (base + 1).coerceAtMost(levels - 1) else base
    }
}

/** GIF flavour of LZW (variable code size 9..12 bits, LSB-first, 255-byte sub-blocks). */
object GifLzw {
    const val MIN_CODE_SIZE = 8
    private const val CLEAR = 1 shl MIN_CODE_SIZE
    private const val END = CLEAR + 1
    private const val MAX_CODES = 4096

    fun encode(indices: ByteArray, out: OutputStream) {
        val bits = BitPacker(out)
        val table = HashMap<Int, Int>(MAX_CODES * 2)
        var codeSize = MIN_CODE_SIZE + 1
        var nextCode = END + 1

        // After writing a code, grow the width once the table has outgrown it (decoders lag one code).
        fun emit(code: Int, reset: Boolean = false) {
            bits.write(code, codeSize)
            if (reset) {
                codeSize = MIN_CODE_SIZE + 1
            } else if (nextCode > (1 shl codeSize) - 1 && codeSize < 12) {
                codeSize++
            }
        }

        emit(CLEAR)
        if (indices.isEmpty()) {
            emit(END)
            bits.flush()
            return
        }
        var prefix = indices[0].toInt() and 0xFF
        for (i in 1 until indices.size) {
            val k = indices[i].toInt() and 0xFF
            val key = (prefix shl 8) or k
            val known = table[key]
            if (known != null) {
                prefix = known
            } else {
                emit(prefix)
                if (nextCode < MAX_CODES) {
                    table[key] = nextCode++
                } else {
                    table.clear()
                    nextCode = END + 1
                    emit(CLEAR, reset = true)
                }
                prefix = k
            }
        }
        emit(prefix)
        emit(END)
        bits.flush()
    }

    private class BitPacker(private val out: OutputStream) {
        private val block = ByteArray(255)
        private var blockSize = 0
        private var acc = 0
        private var accBits = 0

        fun write(code: Int, size: Int) {
            acc = acc or (code shl accBits)
            accBits += size
            while (accBits >= 8) {
                push(acc and 0xFF)
                acc = acc ushr 8
                accBits -= 8
            }
        }

        fun flush() {
            if (accBits > 0) push(acc and 0xFF)
            acc = 0
            accBits = 0
            if (blockSize > 0) {
                out.write(blockSize)
                out.write(block, 0, blockSize)
                blockSize = 0
            }
        }

        private fun push(byte: Int) {
            block[blockSize++] = byte.toByte()
            if (blockSize == 255) {
                out.write(255)
                out.write(block, 0, 255)
                blockSize = 0
            }
        }
    }
}

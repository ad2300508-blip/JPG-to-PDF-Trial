package com.example.util

/**
 * Photo looks as 4×5 colour matrices (row-major, offsets in 0..255), the format shared by
 * android.graphics.ColorMatrix and Compose's ColorMatrix, so preview and saved file match.
 */
enum class PhotoFilter(val label: String, val matrix: FloatArray) {
    ORIGINAL("Originale", PhotoFilterMath.IDENTITY),
    VIVID("Vivace", PhotoFilterMath.saturation(1.45f, contrast = 1.08f)),
    WARM("Caldo", PhotoFilterMath.tint(1.08f, 1.0f, 0.86f, offset = 6f)),
    COOL("Freddo", PhotoFilterMath.tint(0.9f, 1.0f, 1.12f, offset = 0f)),
    FADE("Pastello", PhotoFilterMath.fade()),
    SEPIA("Seppia", PhotoFilterMath.sepia()),
    MONO("B/N", PhotoFilterMath.saturation(0f, contrast = 1f)),
    NOIR("Noir", PhotoFilterMath.saturation(0f, contrast = 1.45f))
}

object PhotoFilterMath {
    val IDENTITY = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    // Rec. 709 luma weights.
    private const val LR = 0.2126f
    private const val LG = 0.7152f
    private const val LB = 0.0722f

    /** Saturation (0 = grey) followed by contrast around mid-grey. */
    fun saturation(s: Float, contrast: Float): FloatArray {
        val sr = (1 - s) * LR
        val sg = (1 - s) * LG
        val sb = (1 - s) * LB
        val c = contrast
        val t = 128f * (1 - c)
        return floatArrayOf(
            c * (sr + s), c * sg, c * sb, 0f, t,
            c * sr, c * (sg + s), c * sb, 0f, t,
            c * sr, c * sg, c * (sb + s), 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun tint(r: Float, g: Float, b: Float, offset: Float): FloatArray = floatArrayOf(
        r, 0f, 0f, 0f, offset,
        0f, g, 0f, 0f, offset / 2,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    fun sepia(): FloatArray = floatArrayOf(
        0.393f, 0.769f, 0.189f, 0f, 0f,
        0.349f, 0.686f, 0.168f, 0f, 0f,
        0.272f, 0.534f, 0.131f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    /** Lower contrast and saturation with lifted blacks: a soft, washed look. */
    fun fade(): FloatArray {
        val base = saturation(0.7f, contrast = 0.82f)
        return base.copyOf().also { m ->
            m[4] += 18f
            m[9] += 16f
            m[14] += 22f
        }
    }

    /** Linear blend between two matrices: t = 0 gives [from], t = 1 gives [to]. */
    fun lerp(from: FloatArray, to: FloatArray, t: Float): FloatArray {
        val k = t.coerceIn(0f, 1f)
        return FloatArray(20) { i -> from[i] + (to[i] - from[i]) * k }
    }

    /** Applies [m] to one opaque RGB colour, clamped to 0..255. */
    fun apply(m: FloatArray, r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
        fun row(o: Int) = (m[o] * r + m[o + 1] * g + m[o + 2] * b + m[o + 3] * 255 + m[o + 4]).toInt().coerceIn(0, 255)
        return Triple(row(0), row(5), row(10))
    }
}

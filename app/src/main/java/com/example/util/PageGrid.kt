package com.example.util

/** Layout of several photos on one PDF page ("Foto per pagina"), in PDF points (origin bottom-left). */
object PageGrid {

    data class Cell(val x: Float, val y: Float, val width: Float, val height: Float)

    val options = listOf(1, 2, 4)

    fun pageCount(images: Int, perPage: Int): Int = if (images <= 0) 0 else (images + perPage - 1) / perPage.coerceAtLeast(1)

    /**
     * Cells in reading order: 2 per page are stacked (top, bottom), 4 per page form a 2×2 grid
     * (top-left, top-right, bottom-left, bottom-right).
     */
    fun cells(perPage: Int, left: Float, bottom: Float, width: Float, height: Float, gap: Float): List<Cell> = when (perPage) {
        2 -> {
            val h = (height - gap) / 2f
            listOf(Cell(left, bottom + h + gap, width, h), Cell(left, bottom, width, h))
        }
        4 -> {
            val w = (width - gap) / 2f
            val h = (height - gap) / 2f
            listOf(
                Cell(left, bottom + h + gap, w, h),
                Cell(left + w + gap, bottom + h + gap, w, h),
                Cell(left, bottom, w, h),
                Cell(left + w + gap, bottom, w, h)
            )
        }
        else -> listOf(Cell(left, bottom, width, height))
    }
}

package com.example.model

import android.graphics.Rect

data class OcrBlock(
  val text: String,
  val boundingBox: Rect? = null,
)

data class OcrResult(
  val fullText: String,
  val blocks: List<OcrBlock> = emptyList(),
)

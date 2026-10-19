package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import com.example.model.OcrBlock
import com.example.model.OcrResult
import com.example.model.PdfSettings
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OcrUtils {

  private val recognizer by lazy {
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
  }

  /**
   * Processes the image using ML Kit on-device text recognition.
   * Runs asynchronously on Dispatchers.IO without blocking the main UI thread.
   */
  suspend fun processImageOcr(
    context: Context,
    uri: Uri,
    rotationDegrees: Int = 0,
  ): OcrResult = withContext(Dispatchers.IO) {
    try {
      if (rotationDegrees == 0) {
        val inputImage = InputImage.fromFilePath(context, uri)
        val visionText = recognizer.process(inputImage).await()
        val fullText = visionText.text.trim()
        val blocks = visionText.textBlocks.map { block ->
          OcrBlock(
            text = block.text,
            boundingBox = block.boundingBox
          )
        }
        OcrResult(fullText = fullText, blocks = blocks)
      } else {
        // When user rotated image in app, decode rotated bitmap for accurate OCR coordinates
        val processed = ImageUtils.processImage(
          context = context,
          uri = uri,
          userRotation = rotationDegrees,
          settings = PdfSettings(enableCompression = false)
        )
        val inputImage = InputImage.fromBitmap(processed.bitmap, 0)
        val visionText = recognizer.process(inputImage).await()
        processed.bitmap.recycle()

        val fullText = visionText.text.trim()
        val blocks = visionText.textBlocks.map { block ->
          OcrBlock(
            text = block.text,
            boundingBox = block.boundingBox
          )
        }
        OcrResult(fullText = fullText, blocks = blocks)
      }
    } catch (_: Exception) {
      OcrResult(fullText = "", blocks = emptyList())
    }
  }

  private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { exception -> cont.resumeWithException(exception) }
    addOnCanceledListener { cont.cancel() }
  }
}

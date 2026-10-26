package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.example.model.ImageConversionSettings
import com.example.model.ImageConvertItem
import com.example.model.ImageFormat
import com.example.util.BitmapLoader
import com.example.util.FileNames
import com.example.util.OutputFolder
import com.example.util.OutputStorage
import com.example.util.resultOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

class ImageConverterService(private val context: Context) {

    suspend fun convertImage(
        item: ImageConvertItem,
        settings: ImageConversionSettings
    ): Result<Pair<Uri, Long>> = withContext(Dispatchers.IO) {
        resultOf {
            val bounds = BitmapLoader.readBounds(context, item.uri)
                ?: throw IllegalStateException("Formato immagine non supportato")
            val longest = max(bounds.width, bounds.height)
            var target = (longest * settings.resolutionScalePercent / 100f).roundToInt().coerceAtLeast(1)
            if (settings.maxSidePx > 0) target = minOf(target, settings.maxSidePx)
            target = target.coerceAtMost(MAX_DECODE_SIDE)

            var bitmap = BitmapLoader.decode(context, item.uri, target)
                ?: throw IllegalStateException("Impossibile leggere l'immagine")
            if (!settings.targetFormat.isLosslessAlphaCapable()) {
                bitmap = BitmapLoader.flattenOnWhite(bitmap)
            }

            val quality = if (settings.targetFormat.isLossless) 100 else settings.qualityPercent.coerceIn(1, 100)
            val outName = "${FileNames.sanitize(FileNames.baseName(item.name), "Immagine")}_convertita.${settings.targetFormat.extension}"

            // Encode into a temp file first: EXIF can only be written to seekable files.
            val temp = File(context.cacheDir, "img_${System.nanoTime()}.${settings.targetFormat.extension}")
            try {
                temp.outputStream().buffered().use { out ->
                    if (!bitmap.compress(settings.targetFormat.compressFormat(), quality, out)) {
                        throw IllegalStateException("Codifica ${settings.targetFormat.extension.uppercase()} non riuscita")
                    }
                }
                bitmap.recycle()

                if (!settings.stripExif && settings.targetFormat == ImageFormat.JPEG) {
                    copyExif(item.uri, temp)
                }

                val size = temp.length()
                val uri = OutputStorage.save(context, outName, settings.targetFormat.mimeType, OutputFolder.IMAGES) { out ->
                    temp.inputStream().use { it.copyTo(out) }
                }
                uri to size
            } finally {
                temp.delete()
            }
        }
    }

    /** Keeps date, camera and GPS info when the user turns off "Rimuovi metadati EXIF". */
    private fun copyExif(source: Uri, target: File) {
        runCatching {
            val src = context.contentResolver.openInputStream(source)?.use { ExifInterface(it) } ?: return
            val dst = ExifInterface(target.absolutePath)
            EXIF_TAGS.forEach { tag -> src.getAttribute(tag)?.let { dst.setAttribute(tag, it) } }
            // Pixels are already rotated upright.
            dst.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            dst.saveAttributes()
        }
    }

    private fun ImageFormat.isLosslessAlphaCapable() = this != ImageFormat.JPEG

    private fun ImageFormat.compressFormat(): Bitmap.CompressFormat = when (this) {
        ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
        ImageFormat.PNG -> Bitmap.CompressFormat.PNG
        ImageFormat.WEBP_LOSSY -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        ImageFormat.WEBP_LOSSLESS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSLESS
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
    }

    private companion object {
        const val MAX_DECODE_SIDE = 8000
        val EXIF_TAGS = listOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_DATETIME_ORIGINAL,
            ExifInterface.TAG_DATETIME_DIGITIZED,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_F_NUMBER,
            ExifInterface.TAG_EXPOSURE_TIME,
            ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
            ExifInterface.TAG_FOCAL_LENGTH,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_WHITE_BALANCE,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE,
            ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP,
            ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_ARTIST,
            ExifInterface.TAG_COPYRIGHT,
            ExifInterface.TAG_IMAGE_DESCRIPTION
        )
    }
}

package com.example.model

import android.net.Uri
import java.io.File
import java.util.Locale

enum class AudioOutputMode(val displayName: String, val extension: String, val mimeType: String) {
  LOSSY_M4A_AAC("M4A / AAC-LC (Lossy)", "m4a", "audio/mp4"),
  LOSSLESS_WAV_PCM("WAV Uncompressed (Lossless PCM 16-bit)", "wav", "audio/wav")
}

enum class AudioBitrate(val label: String, val bps: Int) {
  KBPS_64("64 kbps (Voice - Max Savings)", 64_000),
  KBPS_128("128 kbps (Standard)", 128_000),
  KBPS_192("192 kbps (High Quality)", 192_000),
  KBPS_256("256 kbps (Studio)", 256_000),
  KBPS_320("320 kbps (Extreme)", 320_000);
}

data class AudioLabSettings(
  val mode: AudioOutputMode = AudioOutputMode.LOSSY_M4A_AAC,
  val bitrate: AudioBitrate = AudioBitrate.KBPS_128,
  val forceMono: Boolean = false
)

data class AudioLabItem(
  val id: String,
  val uri: Uri,
  val fileName: String,
  val durationMs: Long,
  val sizeBytes: Long,
  val sampleRate: Int,
  val channels: Int,
  val mimeType: String,
  val convertedFile: File? = null,
  val convertedSizeBytes: Long = 0L,
  val isProcessing: Boolean = false,
  val error: String? = null
) {
  val displayOriginalSize: String
    get() = formatBytes(sizeBytes)

  val displayConvertedSize: String
    get() = if (convertedSizeBytes > 0) formatBytes(convertedSizeBytes) else ""

  val displayDuration: String
    get() = formatDuration(durationMs)

  val savingsPercentage: Int
    get() = if (sizeBytes > 0 && convertedSizeBytes > 0) {
      val diff = sizeBytes - convertedSizeBytes
      ((diff.toDouble() / sizeBytes.toDouble()) * 100).toInt()
    } else 0

  companion object {
    fun formatDuration(ms: Long): String {
      if (ms <= 0) return "--:--"
      val totalSeconds = ms / 1000
      val minutes = totalSeconds / 60
      val seconds = totalSeconds % 60
      return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun formatBytes(bytes: Long): String {
      if (bytes <= 0) return "0 B"
      val kb = bytes / 1024.0
      val mb = kb / 1024.0
      val gb = mb / 1024.0
      return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
      }
    }
  }
}

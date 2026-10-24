package com.example.model

import android.net.Uri

enum class AudioOutputFormat(val displayName: String, val extension: String, val isLossless: Boolean) {
    WAV_PCM_16BIT("WAV (Lossless PCM 16-bit)", "wav", true),
    M4A_AAC("M4A / AAC-LC (Lossy)", "m4a", false)
}

data class AudioConvertItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val originalSizeBytes: Long = 0,
    val durationMs: Long = 0,
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val mimeType: String = "",
    val convertedSizeBytes: Long? = null,
    val isProcessing: Boolean = false,
    val error: String? = null
)

data class AudioConversionSettings(
    val outputFormat: AudioOutputFormat = AudioOutputFormat.M4A_AAC,
    val bitrate: AudioBitrate = AudioBitrate.KBPS_128,
    val forceMono: Boolean = false
)

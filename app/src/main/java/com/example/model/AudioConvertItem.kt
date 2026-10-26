package com.example.model

import android.net.Uri

enum class AudioOutputFormat(val displayName: String, val extension: String, val isLossless: Boolean) {
    WAV_PCM_16BIT("WAV (PCM 16 bit, senza perdita)", "wav", true),
    M4A_AAC("M4A / AAC-LC (compresso)", "m4a", false)
}

data class AudioConvertItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val originalSizeBytes: Long = 0,
    val durationMs: Long = 0,
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val bitrateKbps: Int = 0,
    val mimeType: String = "",
    /** Trim window in milliseconds; null means from the start / until the end. */
    val trimStartMs: Long? = null,
    val trimEndMs: Long? = null,
    val convertedSizeBytes: Long? = null,
    val outputUri: Uri? = null,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null
) {
    val isTrimmed: Boolean get() = (trimStartMs ?: 0L) > 0L || (trimEndMs != null && trimEndMs < durationMs)
    val effectiveDurationMs: Long
        get() = ((trimEndMs ?: durationMs) - (trimStartMs ?: 0L)).coerceAtLeast(0L)
}

data class AudioConversionSettings(
    val outputFormat: AudioOutputFormat = AudioOutputFormat.M4A_AAC,
    val bitrate: AudioBitrate = AudioBitrate.KBPS_128,
    val forceMono: Boolean = false,
    /** Raises the volume so the loudest peak reaches ~-1 dBFS (great for quiet lecture recordings). */
    val normalizeVolume: Boolean = false
)

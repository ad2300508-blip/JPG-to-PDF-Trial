package com.example.model

enum class AudioBitrate(val kbps: Int, val label: String, val description: String = "") {
    KBPS_64(64, "64 kbps", "Voice / Lecture"),
    KBPS_128(128, "128 kbps", "Standard Quality"),
    KBPS_192(192, "192 kbps", "High Quality"),
    KBPS_256(256, "256 kbps", "Studio Quality"),
    KBPS_320(320, "320 kbps", "Extreme Quality")
}

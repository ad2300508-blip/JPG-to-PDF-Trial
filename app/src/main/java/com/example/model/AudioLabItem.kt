package com.example.model

enum class AudioBitrate(val kbps: Int, val label: String, val description: String = "") {
    KBPS_64(64, "64 kbps", "Voce e lezioni"),
    KBPS_128(128, "128 kbps", "Qualità standard"),
    KBPS_192(192, "192 kbps", "Alta qualità"),
    KBPS_256(256, "256 kbps", "Qualità studio"),
    KBPS_320(320, "320 kbps", "Qualità massima")
}

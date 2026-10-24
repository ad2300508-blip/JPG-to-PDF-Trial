package com.example.model

import android.net.Uri

data class ArchiveItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val sizeBytes: Long = 0,
    val mimeType: String = ""
)

data class ArchiveConfig(
    val zipFileName: String = "OmniArchive",
    val compressionLevel: Int = 6 // 0 (Store) to 9 (Maximum Deflate)
)

package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** Display name and size of a content Uri (from the picker, the camera or another app). */
data class UriInfo(val name: String, val sizeBytes: Long, val mimeType: String)

fun Context.queryUriInfo(uri: Uri, fallbackName: String): UriInfo {
    var name = fallbackName
    var size = 0L
    runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { name = it }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }
    }
    if (name == fallbackName) uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.contains('.') }?.let { name = it }
    val mime = runCatching { contentResolver.getType(uri) }.getOrNull() ?: "application/octet-stream"
    return UriInfo(name, size, mime)
}

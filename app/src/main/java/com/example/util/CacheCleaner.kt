package com.example.util

import java.io.File

/** Start-up housekeeping: temporary files the app forgot (crashes, killed jobs) don't pile up. */
object CacheCleaner {

    const val DAY_MS = 24L * 60 * 60 * 1000

    /**
     * Deletes files directly or deeply inside [dir] not modified for [maxAgeMs], skipping the
     * top-level folders in [keep] (e.g. the image loader's own disk cache). Returns bytes freed.
     */
    fun clean(dir: File, now: Long, maxAgeMs: Long = DAY_MS, keep: Set<String> = setOf("image_cache")): Long {
        val children = dir.listFiles() ?: return 0L
        var freed = 0L
        for (child in children) {
            if (child.isDirectory && child.name in keep) continue
            freed += deleteOld(child, now - maxAgeMs)
        }
        return freed
    }

    private fun deleteOld(file: File, cutoff: Long): Long {
        if (file.isDirectory) {
            var freed = 0L
            file.listFiles()?.forEach { freed += deleteOld(it, cutoff) }
            if (file.listFiles()?.isEmpty() == true) file.delete()
            return freed
        }
        if (file.lastModified() >= cutoff) return 0L
        val size = file.length()
        return if (file.delete()) size else 0L
    }
}

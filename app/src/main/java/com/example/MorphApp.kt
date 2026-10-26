package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import com.example.data.AppPreferences
import com.example.util.CacheCleaner

/**
 * App-wide setup. The shared Coil image loader keeps decoded thumbnails in memory (a quarter of
 * the app's heap) so grids and previews scroll without re-decoding, and fades pictures in.
 */
class MorphApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(this)
        // Leftover temp files from interrupted jobs: removed off the main thread so start-up stays fast.
        if (AppPreferences.cacheCleanup.value) {
            Thread({ runCatching { CacheCleaner.clean(cacheDir, System.currentTimeMillis()) } }, "morph-cache-cleanup").apply {
                priority = Thread.MIN_PRIORITY
                start()
            }
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        .crossfade(160)
        .build()
}

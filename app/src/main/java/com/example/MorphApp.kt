package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache

/**
 * App-wide setup. The shared Coil image loader keeps decoded thumbnails in memory (a quarter of
 * the app's heap) so grids and previews scroll without re-decoding, and fades pictures in.
 */
class MorphApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.25)
                .build()
        }
        .crossfade(160)
        .build()
}

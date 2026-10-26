package com.example

import com.example.util.CacheCleaner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class CacheCleanerTest {

    @Test
    fun removesOnlyOldFilesAndKeepsProtectedFolders() {
        val root = Files.createTempDirectory("cache_test").toFile()
        try {
            val now = System.currentTimeMillis()
            val old = root.resolve("old.pdf").apply { writeBytes(ByteArray(10)); setLastModified(now - 3 * CacheCleaner.DAY_MS) }
            val fresh = root.resolve("fresh.jpg").apply { writeBytes(ByteArray(5)) }
            val nested = root.resolve("tmp").apply { mkdirs() }
            val nestedOld = nested.resolve("a.bin").apply { writeBytes(ByteArray(7)); setLastModified(now - 2 * CacheCleaner.DAY_MS) }
            val kept = root.resolve("image_cache").apply { mkdirs() }
            val keptOld = kept.resolve("thumb").apply { writeBytes(ByteArray(3)); setLastModified(now - 5 * CacheCleaner.DAY_MS) }

            val freed = CacheCleaner.clean(root, now)

            assertEquals(17L, freed)
            assertFalse(old.exists())
            assertFalse(nestedOld.exists())
            assertFalse(nested.exists())
            assertTrue(fresh.exists())
            assertTrue(keptOld.exists())
        } finally {
            root.deleteRecursively()
        }
    }
}

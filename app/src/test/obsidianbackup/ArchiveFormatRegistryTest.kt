// test/java/com.titanbackup/ArchiveFormatRegistryTest.kt
package com.titanbackup

import com.titanbackup.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ArchiveFormatRegistryTest {

    @Test
    fun `registry detects zstd tar format`() {
        val registry = ArchiveFormatRegistry()
        val testFile = File("test.tar.zst")

        val format = registry.detect(testFile)
        assertNotNull("Should detect zstd tar format", format)
        assertEquals(".tar.zst", format?.extension)
        assertEquals(0.65f, format?.compressionRatio)
    }

    @Test
    fun `registry detects squashfs format`() {
        val registry = ArchiveFormatRegistry()
        val testFile = File("test.sqfs")

        val format = registry.detect(testFile)
        assertNotNull("Should detect squashfs format", format)
        assertEquals(".sqfs", format?.extension)
        assertEquals(0.55f, format?.compressionRatio)
    }

    @Test
    fun `registry returns null for unknown format`() {
        val registry = ArchiveFormatRegistry()
        val testFile = File("test.unknown")

        val format = registry.detect(testFile)
        assertNull("Should return null for unknown format", format)
    }

    @Test
    fun `can register custom format`() {
        val registry = ArchiveFormatRegistry()

        // Create custom format
        val customFormat = object : ArchiveFormat {
            override val extension = ".custom"
            override val compressionRatio = 0.8f
            override suspend fun create(sourceDir: File, destFile: File) {}
            override suspend fun extract(archiveFile: File, destDir: File) {}
        }

        registry.register(customFormat)

        val testFile = File("test.custom")
        val format = registry.detect(testFile)
        assertNotNull("Should detect custom format", format)
        assertEquals(".custom", format?.extension)
        assertEquals(0.8f, format?.compressionRatio)
    }
}

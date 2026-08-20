package xyz.jekyllex

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import xyz.jekyllex.utils.unzipTo
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipRestoreTest {
    @Test
    fun unzipToRejectsPathEscape() {
        val dest = File.createTempFile("dest", "").apply {
            delete()
            mkdir()
            deleteOnExit()
        }
        val zip = File.createTempFile("backup", ".zip").apply { deleteOnExit() }
        ZipOutputStream(zip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("../evil.txt"))
            zos.write("nope".toByteArray())
            zos.closeEntry()
        }

        try {
            zip.unzipTo(dest)
            fail("expected SecurityException")
        } catch (_: SecurityException) {
        }

        assertFalse(File(dest.parentFile, "evil.txt").exists())
    }

    @Test
    fun unzipToWritesSafeEntries() {
        val dest = File.createTempFile("dest", "").apply {
            delete()
            mkdir()
            deleteOnExit()
        }
        val zip = File.createTempFile("backup", ".zip").apply { deleteOnExit() }
        ZipOutputStream(zip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("ok.txt"))
            zos.write("yes".toByteArray())
            zos.closeEntry()
        }

        zip.unzipTo(dest)
        val out = File(dest, "ok.txt")
        assertTrue(out.exists())
        assertTrue(out.readText() == "yes")
    }
}

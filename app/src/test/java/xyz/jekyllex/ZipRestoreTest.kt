/*
 * MIT License
 *
 * Copyright (c) 2026 Gourav Khunger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

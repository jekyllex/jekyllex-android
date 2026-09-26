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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.jekyllex.data.FilesRepository
import xyz.jekyllex.models.File
import xyz.jekyllex.utils.matchRanges
import xyz.jekyllex.utils.matchesQuery
import xyz.jekyllex.utils.mergeSearchResults
import java.io.File as JFile
import kotlin.io.path.createTempDirectory

class SearchTest {
    @Test
    fun matchRangesFindsEveryCaseInsensitiveHit() {
        assertEquals(listOf(0..1), matchRanges("ABC", "ab"))
        assertEquals(listOf(0..1, 3..4), matchRanges("ab ab", "AB"))
        assertEquals(listOf(0..1), matchRanges("aaa", "aa"))
        assertEquals(emptyList<IntRange>(), matchRanges("abc", ""))
        assertEquals(emptyList<IntRange>(), matchRanges("abc", "z"))
    }

    @Test
    fun matchesQueryUsesCardMetadata() {
        val file = File(
            name = "blog",
            path = "/home/blog",
            isDir = true,
            title = "Needle",
            size = "12K",
        )
        assertTrue(file.matchesQuery(""))
        assertTrue(file.matchesQuery("needle"))
        assertTrue(file.matchesQuery("12k"))
        assertTrue(!file.matchesQuery("missing"))
    }

    @Test
    fun mergeKeepsVisibleCardAndAddsContentHit() {
        val direct = listOf(
            File(name = "blog", path = "/home/blog", isDir = true, title = "Needle", description = "yaml"),
            File(name = "a.md", path = "/home/a.md", isDir = false, description = "yaml"),
        )
        val hits = listOf(
            File(name = "a.md", path = "/home/a.md", isDir = false, description = "needle in body"),
            File(name = "blog/b.md", path = "/home/blog/b.md", isDir = false, description = "needle too"),
        )

        val byTitle = mergeSearchResults(direct, emptyList(), "needle").map { it.name }
        assertEquals(listOf("blog"), byTitle)

        val merged = mergeSearchResults(direct, hits, "needle")
        assertEquals(listOf("a.md", "blog", "blog/b.md"), merged.map { it.name })
        assertEquals("needle in body", merged.first { it.name == "a.md" }.description)
        assertEquals("yaml", merged.first { it.name == "blog" }.description)
    }

    @Test
    fun searchFindsNestedTextAndSkipsGeneratedTrees() {
        val root = createTempDirectory("jekyllex-search").toFile()
        try {
            JFile(root, "blog/_posts").mkdirs()
            JFile(root, "blog/_site").mkdirs()
            JFile(root, "blog/.git").mkdirs()
            JFile(root, "blog/_posts/a.md").writeText("hello\nneedle in the post\n")
            JFile(root, "blog/_site/out.html").writeText("needle generated")
            JFile(root, "blog/.git/config").writeText("needle")
            JFile(root, "blog/readme.md").writeText("hello")
            JFile(root, "blog/pic.png").writeText("needle")
            JFile(root, "blog/bin.dat").writeBytes(byteArrayOf(0, 110, 101, 101, 100, 108, 101))
            val long = "x".repeat(180) + "needle" + "y".repeat(180)
            JFile(root, "blog/long.md").writeText(long)
            JFile(root, ".hidden").mkdirs()
            JFile(root, ".hidden/secret.md").writeText("needle")
            JFile(root, "notes.txt").writeText("needle")

            val repo = FilesRepository()
            val inProject = repo.search(JFile(root, "blog").path, "needle")
            assertEquals(
                listOf("_posts/a.md", "long.md"),
                inProject.map { it.name },
            )
            assertTrue(inProject.single { it.name == "_posts/a.md" }.description!!.contains("needle"))
            assertTrue(inProject.single { it.name == "long.md" }.description!!.length <= 160)
            assertNull(repo.search(JFile(root, "blog").path, "readme").single().description)

            val posts = repo.search(JFile(root, "blog").path, "posts").map { it.name }
            assertEquals(listOf("_posts"), posts)

            val across = repo.search(root.path, "needle", projectsOnly = true).map { it.name }
            assertEquals(listOf("blog/_posts/a.md", "blog/long.md"), across)
            assertTrue(repo.search(root.path, "   ", projectsOnly = true).isEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}

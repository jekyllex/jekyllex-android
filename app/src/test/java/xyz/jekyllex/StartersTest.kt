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

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.jekyllex.utils.cloneTemplateCommand
import xyz.jekyllex.utils.uniqueProjectName

class StartersTest {
    @Test
    fun uniqueProjectNameAppendsNumberInParens() {
        assertEquals("Chirpy", uniqueProjectName("Chirpy", emptyList()))
        assertEquals("Chirpy (1)", uniqueProjectName("Chirpy", listOf("Chirpy")))
        assertEquals(
            "Chirpy (2)",
            uniqueProjectName("Chirpy", listOf("Chirpy", "Chirpy (1)")),
        )
        assertEquals("site", uniqueProjectName("  ", emptyList()))
        assertEquals("foo-bar", uniqueProjectName("foo/bar", emptyList()))
    }

    @Test
    fun cloneTemplateCommandPinsTagWhenPresent() {
        assertArrayEquals(
            arrayOf("git", "clone", "--depth", "1", "--", "https://example.com/r.git", "Chirpy"),
            cloneTemplateCommand("https://example.com/r.git", "Chirpy", null),
        )
        assertArrayEquals(
            arrayOf(
                "git", "clone", "-b", "v7.6.0", "--single-branch", "--depth", "1",
                "--", "https://example.com/r.git", "Chirpy (1)",
            ),
            cloneTemplateCommand("https://example.com/r.git", "Chirpy (1)", " v7.6.0 "),
        )
        assertArrayEquals(
            arrayOf("git", "clone", "--depth", "1", "--", "https://example.com/r.git", "Minima"),
            cloneTemplateCommand("https://example.com/r.git", "Minima", "  "),
        )
    }
}

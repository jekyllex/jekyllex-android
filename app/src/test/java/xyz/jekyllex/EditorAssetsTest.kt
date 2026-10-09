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
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.jekyllex.utils.Constants
import xyz.jekyllex.utils.buildEditorURL
import java.io.File

class EditorAssetsTest {
    @Test
    fun editorUrlKeepsThemeQueryOnLocalHost() {
        assertEquals(
            "https://appassets.androidplatform.net/index.html?lang=md&timeout=1000&theme=3",
            "/tmp/post.md".buildEditorURL(theme = 3, timeout = 1000),
        )
    }

    @Test
    fun bundledThemeIdsMatchEditorAndCssFiles() {
        val index = File("src/main/assets/index.html")
        assertTrue("git submodule update --init", index.exists())
        val block = index.readText().substringAfter("const themeMap = {").substringBefore("}")
        val themes = Regex("""(\d+)\s*:\s*"([^"]+)"""")
            .findAll(block)
            .associate { it.groupValues[1].toInt() to it.groupValues[2] }

        assertEquals(Constants.themeMap.keys, themes.keys)
        themes.forEach { (id, slug) ->
            val css = File("src/main/assets/assets/css/prism/$slug.css")
            assertTrue("theme $id missing $slug.css", css.exists())
        }
    }
}

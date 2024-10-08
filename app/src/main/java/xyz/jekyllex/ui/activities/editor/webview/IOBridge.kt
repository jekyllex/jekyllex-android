/*
 * MIT License
 *
 * Copyright (c) 2024 Gourav Khunger
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

package xyz.jekyllex.ui.activities.editor.webview

import java.io.File
import android.webkit.JavascriptInterface
import androidx.compose.runtime.MutableState
import xyz.jekyllex.utils.fromBase64

class IOBridge(path: String, private val isLoading: MutableState<Boolean>) {
    private val file = File(path)
    private val isSymlink
        get() = file.let { it.canonicalPath != it.absolutePath }

    @JavascriptInterface
    fun setLoaded() {
        isLoading.value = false
    }

    @JavascriptInterface
    fun saveText(content: String) {
        file.apply {
            if (isSymlink) { delete(); createNewFile(); }
            writeText(content.fromBase64())
        }
    }
}

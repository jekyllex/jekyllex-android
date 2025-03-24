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

package xyz.jekyllex.ui.activities.editor.components

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import xyz.jekyllex.ui.activities.editor.webview.IOBridge
import xyz.jekyllex.ui.activities.editor.webview.WebViewClient
import xyz.jekyllex.utils.buildEditorURL

@Composable
fun Editor(
    viewCache: SnapshotStateMap<Int, WebView>,
    file: String,
    theme: Int,
    timeout: Int,
    padding: PaddingValues,
    isLoading: MutableState<Boolean>
) {
    Surface {
        Box(
            Modifier.consumeWindowInsets(padding).imePadding()
        ) {
            if (isLoading.value) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).zIndex(1f)
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    viewCache.getOrPut(0) {
                        WebView(it).apply {
                            this.layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            webViewClient = WebViewClient(file)
                            settings.javaScriptEnabled = true

                            val bridge = IOBridge(file, isLoading)
                            addJavascriptInterface(bridge, "IOBridge")

                            loadUrl(file.buildEditorURL(theme, timeout))
                        }
                    }
                }
            )
        }
    }
}

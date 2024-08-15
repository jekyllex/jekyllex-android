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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import xyz.jekyllex.ui.activities.editor.webview.WebViewChromeClient
import xyz.jekyllex.ui.activities.editor.webview.WebViewClient
import xyz.jekyllex.utils.Commands.rmDir
import xyz.jekyllex.utils.Constants.WEBVIEW_CACHE
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.buildPreviewURL

@Composable
fun Preview(
    viewCache: SnapshotStateMap<Int, WebView>,
    file: String,
    guessedUrl: String,
    canPreview: Boolean,
    padding: PaddingValues,
    runServer: () -> Unit = {}
) {
    if (!canPreview)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Server idle",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Can't preview without starting the jekyll server",
                style = MaterialTheme.typography.labelSmall
            )
            Button(
                onClick = { runServer() },
                modifier = Modifier.padding(top = 16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Text(text = "Run")
            }
        }
    else
        Surface {
            var canGoBack by remember { mutableStateOf(false) }
            val defaultUrl by rememberUpdatedState(guessedUrl)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(padding)
                    .imePadding()
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    factory = {
                        viewCache.getOrPut(1) {
                            WebView(it).apply {
                                this.layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )

                                webViewClient = WebViewClient(file) { url ->
                                    if (
                                        defaultUrl.isNotBlank() &&
                                        url.contains(defaultUrl) &&
                                        !canGoBack
                                    ) {
                                        viewCache[1]?.clearHistory()
                                        return@WebViewClient
                                    }
                                    canGoBack = viewCache[1]?.canGoBack() ?: false
                                }

                                webChromeClient = WebViewChromeClient()
                                settings.javaScriptEnabled = true

                                NativeUtils.exec(rmDir(WEBVIEW_CACHE))
                                loadUrl(defaultUrl.buildPreviewURL())
                            }
                        }
                    },
                )
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    IconButton(
                        onClick = { viewCache[1]?.goBack() },
                        modifier = Modifier.weight(1f),
                        enabled = canGoBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go Back")
                    }

                    if (defaultUrl.isNotBlank())
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                viewCache[1]?.loadUrl(defaultUrl.buildPreviewURL())
                                canGoBack = false
                            },
                        ) { Text(text = "Load default") }

                    IconButton(onClick = { viewCache[1]?.reload() }, Modifier.weight(1f)) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            }
        }
}

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

package xyz.jekyllex.ui.editor.webview

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.jekyllex.utils.Commands.cat
import xyz.jekyllex.utils.Constants.EDITOR_HOST
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.toBase64

class WebViewClient(
    private val file: String,
    private val bridge: IOBridge? = null,
    private val previewLoadCallback: (url: String) -> Unit = {},
): WebViewClient() {
    private var assets: WebViewAssetLoader? = null
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val uri = request.url
        if (isEditor(uri) || isPreview(uri)) return false

        try {
            view.context.startActivity(
                Intent(Intent.ACTION_VIEW, uri)
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                view.context,
                "No app found to open this link",
                Toast.LENGTH_SHORT
            ).show()
        }

        return true
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        val uri = Uri.parse(url)
        bridge?.setTrusted(isEditor(uri))
        if (isPreview(uri)) previewLoadCallback(url)
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val loader = assets ?: WebViewAssetLoader.Builder()
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(view.context.applicationContext))
            .build()
            .also { assets = it }
        return loader.shouldInterceptRequest(request.url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        if (!isEditor(Uri.parse(url))) return

        NativeUtils.exec(cat(file), CoroutineScope(Dispatchers.IO)) { content ->
            withContext(Dispatchers.Main) {
                view.evaluateJavascript("setText('${content.toBase64()}')", null)
            }
        }
    }

    private fun isEditor(uri: Uri) = uri.scheme == "https" && uri.host == EDITOR_HOST

    private fun isPreview(uri: Uri) = uri.scheme == "http" && (uri.host == "localhost" || uri.host == "127.0.0.1")
}

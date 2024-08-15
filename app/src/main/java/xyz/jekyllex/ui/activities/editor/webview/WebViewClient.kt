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

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import xyz.jekyllex.utils.Commands.cat
import xyz.jekyllex.utils.Constants.EDITOR_URL
import xyz.jekyllex.utils.Constants.PREVIEW_URL
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.encodeURIComponent
import xyz.jekyllex.utils.toBase64

class WebViewClient(
    private val file: String,
    private val previewLoadCallback: (url: String) -> Unit = {}
): WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val url = request.url.toString()
        if (url.contains(EDITOR_URL) || url.contains(PREVIEW_URL)) return false

        view.context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        )

        return true
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)

        if (url.contains(PREVIEW_URL)) previewLoadCallback(url)
        if (!url.contains(EDITOR_URL)) return

        view.loadUrl(
            "javascript:setText('${
                NativeUtils.exec(cat(file)).toBase64().encodeURIComponent()
            }')"
        )
    }
}

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

package xyz.jekyllex.utils

object Constants {
    const val APP_PACKAGE = "xyz.jekyllex"
    const val PREFIX = "/data/data/$APP_PACKAGE"
    const val FILES_DIR = "$PREFIX/files"
    const val HOME_DIR = "$FILES_DIR/home"
    const val USR_DIR = "$FILES_DIR/usr"
    const val BIN_DIR = "$USR_DIR/bin"
    const val LIB_DIR = "$USR_DIR/lib"
    const val GEM_DIR = "$LIB_DIR/ruby/gems/3.3.0"
    const val WEBVIEW_CACHE = "$PREFIX/app_webview"
    const val DOMAIN = "jekyllex.xyz"
    const val HOME_PAGE = "https://$DOMAIN"
    const val LICENSES = "$HOME_PAGE/licenses"
    const val PRIVACY = "$HOME_PAGE/privacy-policy"
    const val TERMS = "$HOME_PAGE/terms-and-conditions"
    const val EDITOR_URL = "https://editor.jekyllex.xyz"
    const val PREVIEW_URL = "http://localhost:4000"
    const val PAT_SETTINGS_URL = "https://github.com/settings/tokens/new"

    val requiredBinaries = arrayOf("ruby", "gem", "bundler", "jekyll")

    val defaultExtensions = mapOf(
        "Gemfile" to "rb",
        "Rakefile" to "rb",
    )

    val extensionAliases = mapOf(
        "htm" to "md",
        "html" to "md",
        "lock" to "rb",
    )
}

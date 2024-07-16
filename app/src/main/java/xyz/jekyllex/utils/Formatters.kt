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

import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.util.Base64
import xyz.jekyllex.utils.Constants.Companion.HOME_DIR
import xyz.jekyllex.utils.Constants.Companion.aliasExtensions
import xyz.jekyllex.utils.Constants.Companion.defaultExtensions
import java.net.URLEncoder
import java.util.Locale

fun String.getExtension(): String = this
    .substringAfterLast("/")
    .let {
        if (defaultExtensions.contains(it)) defaultExtensions[it]!!
        else ""
    }
    .ifBlank { this.substringAfterLast(".") }
    .let { aliasExtensions[it] ?: it.ifBlank { "txt" } }

fun String.trimQuotes(level: Int): String = this.drop(level).dropLast(level)

fun String.toBase64(): String = Base64.encodeToString(
    this.toByteArray(charset("UTF-8")),
    Base64.NO_WRAP
)

fun String.encodeURIComponent(): String = URLEncoder.encode(this, "UTF-8")

fun mergeCommands(vararg commands: Array<String>): String =
    commands.joinToString(";") { cmd -> cmd.joinToString(" ") }

fun String.formatDir(separator: String): String =
    this.replace(HOME_DIR, "~").replace("/", separator)

fun String.extractProject(): String? =
    if (this.contains("$HOME_DIR/"))
        this.replace("$HOME_DIR/", "").substringBefore("/")
    else null

fun String.getFilesInDir(dir: String): List<String> = this.split("\n").map {
        it.replace(dir, "").replace("/", "")
    }.filter { it.isNotBlank() }

fun String.toDate(): String {
    val dateFormat = SimpleDateFormat("hh:mm a yyyy-MM-dd", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    return dateFormat.format(this.toLong() * 1000)
}

fun buildStatsString(size: String?, lastMod: String?): String? =
    if (size == null || lastMod == null) null
    else "Size: $size  •  Last modified: $lastMod"

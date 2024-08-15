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
import xyz.jekyllex.utils.Constants.EDITOR_URL
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.Constants.PREVIEW_URL
import xyz.jekyllex.utils.Constants.extensionAliases
import xyz.jekyllex.utils.Constants.defaultExtensions
import xyz.jekyllex.utils.Setting.DEBOUNCE_DELAY
import java.net.URLEncoder
import java.util.Locale

fun String.getExtension(): String = this
    .substringAfterLast("/")
    .let {
        defaultExtensions[it] ?:
        if (it.contains(".")) it.substringAfterLast(".") else "txt"
    }
    .let { extensionAliases[it] ?: it}

fun String.trimQuotes(): String =
    if (this.length < 2) this
    else if (this[0] == '"' && this[this.length - 1] == '"') this.drop(1).dropLast(1)
    else this

fun String.toBase64(): String = Base64.encodeToString(
    this.toByteArray(charset("UTF-8")),
    Base64.NO_WRAP
)

fun String.fromBase64(): String = String(Base64.decode(this, Base64.NO_WRAP))

fun String.encodeURIComponent(): String = URLEncoder.encode(this, "UTF-8")

fun mergeCommands(vararg commands: Array<String>): String =
    commands.joinToString(";") { cmd -> cmd.joinToString(" ") }

fun String.formatDir(separator: String): String =
    this.replace(HOME_DIR, "~").replace("/", separator)

fun String.getProjectDir(): String? =
    if (this.contains("$HOME_DIR/"))
        "$HOME_DIR/" + this.substringAfter("$HOME_DIR/").substringBefore("/")
    else null

fun String.pathInProject() = this.substringAfter("$HOME_DIR/")
    .substringAfter("/")

fun String.getFilesInDir(dir: String): List<String> = this.split("\n").map {
        it.replace(dir, "").replace("/", "")
    }.filter { it.isNotBlank() }

fun String.toDate(): String {
    val dateFormat = SimpleDateFormat("hh:mm a yyyy-MM-dd", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getDefault()
    return dateFormat.format(this.toLong() * 1000)
}

fun buildStatsString(isDir: Boolean?, size: String?, lastMod: String?): String? {
    if (size == null && lastMod == null) return null
    val dirTag = isDir?.let { if (it) "Folder" else "File" } ?: ""
    val sizeTag = (isDir?.let { "  •  " } ?: "") + (size?.let { "Size: $size" } ?: "")
    val lastModTag = lastMod?.let { "  •  Modified: $lastMod" } ?: ""
    return "$dirTag$sizeTag$lastModTag"
}

fun String.buildEditorURL(timeout: Int = DEBOUNCE_DELAY.defaultValue.get()): String =
    "$EDITOR_URL/?lang=${this.getExtension()}&timeout=$timeout"

fun String.buildPreviewURL(): String =
    PREVIEW_URL + this.let { if ((it.getOrNull(0) ?: "") == '/') it else "/$it" }

fun String.toCommand(): Array<String> {
    val command = mutableListOf<String>()
    val currentArg = StringBuilder()
    var inDoubleQuotes = false
    var inSingleQuotes = false

    for (char in this) {
        when {
            char == ' ' && !inDoubleQuotes && !inSingleQuotes -> {
                if (currentArg.isNotEmpty()) {
                    command.add(currentArg.toString())
                    currentArg.clear()
                }
            }
            char == '"' -> inDoubleQuotes = !inDoubleQuotes
            char == '\'' -> inSingleQuotes = !inSingleQuotes
            else -> currentArg.append(char)
        }
    }

    if (currentArg.isNotEmpty()) {
        command.add(currentArg.toString())
    }

    return command.toTypedArray()
}

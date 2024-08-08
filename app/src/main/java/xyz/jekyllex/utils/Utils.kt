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

import java.io.File
import android.content.Context
import xyz.jekyllex.utils.Commands.Companion.git
import xyz.jekyllex.utils.Constants.Companion.BIN_DIR

private val denyList = arrayOf("ls", "ln", "cd")
fun Array<String>.isDenied(): Boolean = this.any { it in denyList }
fun Array<String>.drop(n: Int): Array<String> = this.toList().drop(n).toTypedArray()

fun Array<String>.transform(context: Context): Array<String> = this.let {
    val settings = Settings(context)
    val command = when (this.getOrNull(0)) {
        "git" -> {
            val enableProgress = settings.get<Boolean>(Setting.LOG_PROGRESS)
            if (enableProgress && this.any {
                    it in arrayOf(
                        "clone", "fetch", "pull", "push", "archive", "repack"
                    )
                }) {
                git(true, *this.drop(1))
            } else this
        }

        else -> this
    }
    arrayOf("$BIN_DIR/${command.getOrNull(0)}", *command.drop(1))
}

fun File.removeSymlinks() {
    if (this.isDirectory) {
        this.listFiles()?.forEach { it.removeSymlinks() }
    } else {
        if (this.canonicalPath != this.absolutePath) {
            this.apply {
                val text = readText()
                delete()
                createNewFile()
                writeText(text)
            }
        }
    }
}

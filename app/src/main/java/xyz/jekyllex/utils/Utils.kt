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
import android.content.Intent
import android.content.ActivityNotFoundException
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import xyz.jekyllex.BuildConfig
import xyz.jekyllex.utils.Commands.git
import xyz.jekyllex.utils.Commands.bundle
import xyz.jekyllex.utils.Commands.jekyll
import xyz.jekyllex.utils.Constants.editorMimes
import xyz.jekyllex.utils.Constants.editorExtensions
import xyz.jekyllex.models.File as FileModel
import xyz.jekyllex.ui.activities.editor.EditorActivity

fun Array<String>.isDenied(): Boolean = Constants.denyList.any { this.getOrNull(0) == it }
fun Array<String>.drop(n: Int): Array<String> = this.toList().drop(n).toTypedArray()

fun Array<String>.transform(context: Context): Array<String> = this.let {
    val settings = Settings(context)
    when (this.getOrNull(0)) {
        "bundle" -> {
            val localGems = settings.get<Boolean>(Setting.LOCAL_GEMS)
            if (localGems && this.any {
                    it in arrayOf("install", "update")
                }) {
                bundle(*this.drop(1), "--prefer-local")
            } else this
        }

        "git" -> {
            val enableProgress = settings.get<Boolean>(Setting.LOG_PROGRESS)
            if (enableProgress && this.any {
                    it in arrayOf("clone", "fetch", "pull", "push")
                }) {
                git( *this.drop(1), "--progress")
            } else this
        }

        "jekyll" -> {
            if (this.getOrNull(1) == "new") {
                val skipBundle = settings.get<Boolean>(Setting.SKIP_BUNDLER)
                if (skipBundle) jekyll(*this.drop(1), "--skip-bundle")
                else this
            }
            else if (this.getOrNull(1) == "serve") {
                val liveReload = settings.get<Boolean>(Setting.LIVERELOAD)
                val prefixBundler = settings.get<Boolean>(Setting.PREFIX_BUNDLER)
                val flags = settings.get<String>(Setting.JEKYLL_FLAGS).split(" ")

                val command = prefixBundler.let {
                    if (it) bundle("exec", *jekyll("serve"))
                    else jekyll("serve")
                }.toMutableList()

                if (liveReload) command.add("-l")
                command.addAll(flags)

                command.addAll(this.drop(2))
                command.toTypedArray()
            }
            else this
        }

        else -> this
    }
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

fun FileModel.open(context: Context) {
    val defaultAction = {
        context.startActivity(
            Intent(context, EditorActivity::class.java)
                .putExtra("file", this.path)
        )
    }
    val file = File(this.path)
    val uri = FileProvider.getUriForFile(
        context,
        BuildConfig.APPLICATION_ID + ".provider",
        file
    )

    val ext = this.path.getExtension()
    val mime = ext.mimeType()

    if (
        this.name.startsWith(".") ||
        editorExtensions.contains(ext) ||
        editorMimes.any { mime.contains(it) }
    ) defaultAction()
    else {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app found to open this file!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Can't open file!", Toast.LENGTH_SHORT).show()
        }
    }
}

fun String.mimeType(): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(this) ?: "text/*"

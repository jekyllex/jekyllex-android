/*
 * MIT License
 *
 * Copyright (c) 2026 Gourav Khunger
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

package xyz.jekyllex.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import xyz.jekyllex.models.Starter
import xyz.jekyllex.utils.Commands.curl
import xyz.jekyllex.utils.Constants.TEMPLATES_URL
import xyz.jekyllex.utils.NativeUtils
import java.io.File

object StartersRepository {
    fun fetch(url: String = TEMPLATES_URL): List<Starter> {
        val body = NativeUtils.exec(curl("-sS", "-L", "--", url))
        if (body.isEmpty()) error("empty catalog")
        return parse(body)
    }

    fun loadImage(cacheDir: File, url: String): Bitmap? {
        val dir = File(cacheDir, "templates").apply { mkdirs() }
        val dest = File(dir, "%08x".format(url.hashCode()))
        if (!dest.exists() || dest.length() == 0L) {
            NativeUtils.exec(curl("-sS", "-L", "-o", dest.absolutePath, "--", url))
        }
        if (!dest.exists() || dest.length() == 0L) return null
        return BitmapFactory.decodeFile(dest.absolutePath)
    }

    fun parse(json: String): List<Starter> {
        val items = JSONArray(json)
        return buildList {
            for (i in 0 until items.length()) {
                val obj = items.getJSONObject(i)
                val name = obj.optString("name").trim()
                val git = obj.optString("git").trim()
                if (name.isEmpty() || git.isEmpty()) continue
                add(
                    Starter(
                        git = git,
                        name = name,
                        url = obj.optString("url").trim(),
                        image = obj.optString("image").trim(),
                        description = obj.optString("description").trim(),
                        version = obj.optString("version").trim().ifEmpty { null },
                    )
                )
            }
        }
    }
}

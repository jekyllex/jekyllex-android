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

import android.content.ContentResolver
import android.net.Uri
import xyz.jekyllex.models.File
import xyz.jekyllex.utils.Commands.diskUsage
import xyz.jekyllex.utils.Commands.getFromYAML
import xyz.jekyllex.utils.Commands.shell
import xyz.jekyllex.utils.Commands.stat
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.mergeCommands
import xyz.jekyllex.utils.parseOutput
import xyz.jekyllex.utils.toDate
import java.io.File as JFile
import java.io.IOException

class FilesRepository {
    fun list(dirPath: String): List<File> {
        val children = JFile(dirPath).listFiles()?.sortedBy { it.name } ?: emptyList()
        val listed = if (dirPath == HOME_DIR) {
            children.filter { it.isDirectory && !it.name.startsWith(".") }
        } else {
            children.filter { it.name != ".git" }
        }
        return listed.map {
            File(
                name = it.name,
                path = "$dirPath/${it.name}",
                isDir = it.isDirectory
            )
        }
    }

    fun search(
        dirPath: String,
        query: String,
        projectsOnly: Boolean = dirPath == HOME_DIR,
        active: () -> Boolean = { true },
    ): List<File> {
        if (query.isBlank() || !active()) return emptyList()
        val rootCanon = try {
            JFile(dirPath).canonicalPath
        } catch (_: IOException) {
            return emptyList()
        }
        val out = ArrayList<File>()
        val seen = HashSet<String>()
        fun walk(parentPath: String, parent: JFile, depth: Int): Boolean {
            if (!active()) return false
            val canon = try {
                parent.canonicalPath
            } catch (_: IOException) {
                return true
            }
            if (canon != rootCanon && !canon.startsWith("$rootCanon/")) return true
            if (!seen.add(canon)) return true
            val children = parent.listFiles() ?: return true
            for (child in children) {
                if (!active()) return false
                if (child.name == ".git") continue
                if (projectsOnly && depth == 0 && (!child.isDirectory || child.name.startsWith("."))) {
                    continue
                }
                val path = "$parentPath/${child.name}"
                val displayName = if (depth == 0) child.name else path.removePrefix("$dirPath/")
                val nameHit = child.name.contains(query, ignoreCase = true)
                val excerpt = if (!nameHit && !child.isDirectory) contentExcerpt(child, query) else null
                if (nameHit || excerpt != null) {
                    out.add(
                        File(
                            name = displayName,
                            path = path,
                            isDir = child.isDirectory,
                            description = excerpt,
                        )
                    )
                }
                if (child.isDirectory && child.name !in SKIP_DIRS && depth < MAX_SEARCH_DEPTH) {
                    if (!walk(path, child, depth + 1)) return false
                }
            }
            return true
        }
        if (!walk(dirPath, JFile(dirPath), 0)) return emptyList()
        return out.sortedBy { it.name }
    }

    fun withStats(file: File, cwd: String): File {
        val stats = NativeUtils.exec(
            shell(
                mergeCommands(
                    diskUsage("-sh", file.path),
                    stat("-c", "%Y", file.path)
                )
            )
        ).split("\n")

        val properties =
            if (cwd == HOME_DIR)
                NativeUtils.exec(
                    getFromYAML(
                        "${file.path}/_config.yml",
                        "title", "description", "url", "baseurl"
                    )
                ).parseOutput()
            else if (!file.isDir && cwd.contains("/_") && !cwd.contains("/_site"))
                NativeUtils.exec(
                    getFromYAML(file.path, "title", "description")
                ).parseOutput()
            else listOf()

        return file.copy(
            title = properties.getOrNull(0),
            description = properties.getOrNull(1),
            lastModified = stats.getOrNull(1)?.toDate(),
            size = stats.getOrNull(0)?.split("\t")?.first(),
            url = properties.getOrNull(2)?.let { url ->
                url + (properties.getOrNull(3) ?: "")
            }
        )
    }

    fun copyUri(resolver: ContentResolver, uri: Uri, destDir: String, name: String) {
        val dest = JFile(destDir, name)
        resolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        } ?: throw java.io.IOException("Unable to open $uri")
    }

    private fun contentExcerpt(file: JFile, query: String): String? {
        if (!file.isFile) return null
        if (file.extension.lowercase() in BINARY_EXT) return null
        val length = file.length()
        if (length <= 0L || length > MAX_SEARCH_BYTES) return null
        val text = try {
            file.inputStream().use { input ->
                val buf = ByteArray(length.toInt())
                var off = 0
                while (off < buf.size) {
                    val n = input.read(buf, off, buf.size - off)
                    if (n < 0) break
                    off += n
                }
                if (off <= 0) return null
                if ((0 until off).any { buf[it] == 0.toByte() }) return null
                String(buf, 0, off, Charsets.UTF_8)
            }
        } catch (_: IOException) {
            return null
        }
        val index = text.indexOf(query, ignoreCase = true)
        if (index < 0) return null
        val lineStart = text.lastIndexOf('\n', index).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', index).let { if (it < 0) text.length else it }
        var line = text.substring(lineStart, lineEnd).trim()
        if (line.length > 160) {
            val local = line.indexOf(query, ignoreCase = true)
            if (local >= 0) {
                val start = (local - 50).coerceAtLeast(0)
                val end = (local + query.length + 50).coerceAtMost(line.length)
                line = line.substring(start, end).trim()
            }
        }
        return line.ifBlank { null }
    }

    private companion object {
        const val MAX_SEARCH_DEPTH = 20
        const val MAX_SEARCH_BYTES = 1_048_576
        val SKIP_DIRS = setOf("_site", "vendor", "node_modules", ".bundle", ".jekyll-cache")
        val BINARY_EXT = setOf(
            "png", "jpg", "jpeg", "gif", "webp", "ico", "pdf",
            "zip", "gz", "woff", "woff2", "ttf", "otf", "mp4", "mp3",
            "so", "jar", "class",
        )
    }
}

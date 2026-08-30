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
}

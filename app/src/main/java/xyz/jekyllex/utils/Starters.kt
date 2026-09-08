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

package xyz.jekyllex.utils

import xyz.jekyllex.utils.Commands.git

fun uniqueProjectName(base: String, existing: Collection<String>): String {
    val name = base.trim().ifEmpty { "site" }
        .replace('/', '-')
        .replace('\\', '-')
    if (existing.none { it == name }) return name
    var n = 1
    while (existing.any { it == "$name ($n)" }) n++
    return "$name ($n)"
}

fun cloneTemplateCommand(url: String, dest: String, version: String?): Array<String> {
    val tag = version?.trim().orEmpty()
    return if (tag.isEmpty()) {
        git("clone", "--depth", "1", "--", url, dest)
    } else {
        git("clone", "-b", tag, "--single-branch", "--depth", "1", "--", url, dest)
    }
}

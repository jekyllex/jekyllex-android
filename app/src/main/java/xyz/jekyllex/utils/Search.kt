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

import xyz.jekyllex.models.File

fun matchRanges(text: String, query: String): List<IntRange> {
    if (query.isBlank()) return emptyList()
    val ranges = ArrayList<IntRange>()
    var index = 0
    while (index < text.length) {
        val match = text.indexOf(query, index, ignoreCase = true)
        if (match < 0) break
        val end = match + query.length
        ranges.add(match until end)
        index = end
    }
    return ranges
}

fun File.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return name.contains(query, ignoreCase = true) ||
        url.orEmpty().contains(query, ignoreCase = true) ||
        size.orEmpty().contains(query, ignoreCase = true) ||
        title.orEmpty().contains(query, ignoreCase = true) ||
        description.orEmpty().contains(query, ignoreCase = true) ||
        lastModified.orEmpty().contains(query, ignoreCase = true)
}

fun mergeSearchResults(direct: List<File>, hits: List<File>, query: String): List<File> {
    val byPath = direct.associateBy { it.path }
    val visible = direct.filter { it.matchesQuery(query) }
    return (visible + hits)
        .distinctBy { it.path }
        .map { hit ->
            val known = byPath[hit.path]
            when {
                known == null -> hit
                known.matchesQuery(query) -> known
                hit.description != null -> known.copy(description = hit.description)
                else -> known
            }
        }
        .sortedBy { it.name }
}

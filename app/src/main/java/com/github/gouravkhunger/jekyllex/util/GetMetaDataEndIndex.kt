/*
 * MIT License
 *
 * Copyright (c) 2021 Gourav Khunger
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

package com.github.gouravkhunger.jekyllex.util

/*
 * Jekyll posts are made up of two components:
 * 1. The post meta-data at the beginning of the file
 * 2. The actual post content below the post meta data.
 *
 * This function is is used to return the index where the
 * post meta-data ends, so that it can be differentiated
 * from the post content.
 */

fun getMetaDataEndIndex(content: String): Int {
    // The "cnt" variable stores how many "---" it has seen.
    // It is sure that the meta data will be enclosed by only 2 of them,
    var cnt = 0

    // idx is the position where the meta data ends.
    // We keep on increasing the idx until we are sure that meta data has ended.
    var idx = 0

    while (content[idx].isWhitespace()) idx++
    while (cnt != 2) {
        if (content.substring(idx, idx + 3) == "---") cnt++
        idx++
    }

    // Increase idx by 2 because it is still at the
    // first index of the second "---" exist
    idx += 2

    // ignore further whitespaces as they will be added later if needed.
    while (content[idx].isWhitespace()) idx++

    return idx
}

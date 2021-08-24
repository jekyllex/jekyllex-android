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

fun isValidFileName(name: String?): Boolean {
    if (name.isNullOrEmpty()) return false

    name.forEach {
        if (it.isWhitespace()) return false
    }

    val digitIndexes = arrayListOf(0, 1, 2, 3, 5, 6, 8, 9)

    digitIndexes.forEach { i ->
        if (!name[i].isDigit()) return false
    }

    val hyphenIndexes = arrayListOf(4, 7, 10)

    hyphenIndexes.forEach { i ->
        if (name[i] != '-') return false
    }

    if (name[11] == '.') return false

    val size = name.length

    if (name.substring(size - 3, size) != ".md") return false

    return true
}

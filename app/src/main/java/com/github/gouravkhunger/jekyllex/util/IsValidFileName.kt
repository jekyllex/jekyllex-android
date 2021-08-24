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

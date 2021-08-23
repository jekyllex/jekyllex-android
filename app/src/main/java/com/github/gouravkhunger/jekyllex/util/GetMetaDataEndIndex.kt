package com.github.gouravkhunger.jekyllex.util

fun getMetaDataEndIndex(content: String): Int {
    var cnt = 0
    var idx = 0
    while (content[idx].isWhitespace()) idx++
    while (cnt != 2) {
        if (content.substring(idx, idx + 3) == "---") cnt++
        idx++
    }
    idx += 2;
    while (content[idx].isWhitespace()) idx++
    return idx
}

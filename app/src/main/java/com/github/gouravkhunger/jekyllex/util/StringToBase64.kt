package com.github.gouravkhunger.jekyllex.util

import android.util.Base64

fun stringToBase64(metaData: String, content: String): String {
    var combined = metaData
    if(!metaData[metaData.length-1].isWhitespace()) {
        combined += "\n\n"
    }
    combined += content
    return Base64.encodeToString(combined.toByteArray(), Base64.DEFAULT)
}
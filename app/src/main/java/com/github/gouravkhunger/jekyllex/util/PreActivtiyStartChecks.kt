package com.github.gouravkhunger.jekyllex.util

import android.content.Context

fun preActivityStartChecks(ctx: Context): Int {
    if (!checkCredentials(ctx)) return 1
    if (!hasInternetConnection(ctx)) return 2
    return 0
}

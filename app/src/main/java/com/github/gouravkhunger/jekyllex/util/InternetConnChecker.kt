package com.github.gouravkhunger.jekyllex.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.*

// function to check all the possible conditons when the device can have
// an active internet connection or not
fun hasInternetConnection(ctx: Context): Boolean {
    val connectivityManager = ctx.getSystemService(
        Context.CONNECTIVITY_SERVICE
    ) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities =
        connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return when {
        capabilities.hasTransport(TRANSPORT_WIFI) -> true
        capabilities.hasTransport(TRANSPORT_CELLULAR) -> true
        capabilities.hasTransport(TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

package com.github.gouravkhunger.fontize

import android.content.Context
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar

fun Snackbar.changeFont(context: Context) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val fontId = prefs.getInt("fontFamily", ResourcesCompat.ID_NULL)
    val tv = view.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
    val font = ResourcesCompat.getFont(context, fontId)
    tv.typeface = font
}

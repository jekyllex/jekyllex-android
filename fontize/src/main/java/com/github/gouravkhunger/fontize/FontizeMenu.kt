package com.github.gouravkhunger.fontize

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.TypefaceSpan
import android.view.Menu
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.preference.PreferenceManager

class FontizeMenu(context: Context, menu: Menu) {
    init {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val fontId = prefs.getInt("fontFamily", ResourcesCompat.ID_NULL)

        if (fontId != ResourcesCompat.ID_NULL) {
            menu.children.forEach {
                val newTitle = SpannableString(it.title)

                newTitle.setSpan(
                    object : TypefaceSpan(null) {
                        override fun updateDrawState(ds: TextPaint) {
                            ds.typeface = ResourcesCompat.getFont(context, fontId)
                        }
                    },
                    0,
                    newTitle.length,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )

                it.title = newTitle
            }
        }
    }
}

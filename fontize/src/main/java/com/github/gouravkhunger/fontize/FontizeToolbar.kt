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

package com.github.gouravkhunger.fontize

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.preference.PreferenceManager

class FontizeToolbar(
    context: Context,
    attrs: AttributeSet
) : Toolbar(context, attrs) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val bgColor = prefs.getString("primaryAppColor", "#000000")
    private val color = prefs.getString("primaryTextColor", "#ffffff")

    init {

        this.setBackgroundColor(Color.parseColor(bgColor))
    }

    fun applyFont() {
        val fontId = prefs.getInt("fontFamily", ResourcesCompat.ID_NULL)

        if (fontId != ResourcesCompat.ID_NULL) {
            val typeface = ResourcesCompat.getFont(context, fontId)

            for (i in 0 until this.childCount) {
                if (this.getChildAt(i) is TextView) {
                    val textView = (this.getChildAt(i) as TextView)
                    textView.typeface = typeface
                    textView.setTextColor(Color.parseColor(color))
                    textView.invalidate()
                }
            }
        }

        this.navigationIcon?.setTint(Color.parseColor(color))
        this.overflowIcon?.setTint(Color.parseColor(color))
    }
}

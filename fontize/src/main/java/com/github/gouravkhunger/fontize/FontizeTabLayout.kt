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
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.allViews
import androidx.preference.PreferenceManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textview.MaterialTextView

class FontizeTabLayout(
    context: Context,
    attrs: AttributeSet
) : TabLayout(context, attrs) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val bgColor = prefs.getString("primaryAppColor", "#000000")
    private val fontId = prefs.getInt("fontFamily", ResourcesCompat.ID_NULL)
    private val fontColor = prefs.getString("primaryTextColor", "#ffffff")

    init {
        this.setBackgroundColor(Color.parseColor(bgColor))
    }

    fun formatTextViews() {
        for (i in 0 until this.tabCount) {
            this.getTabAt(i)?.view?.allViews?.forEach {
                if (it is MaterialTextView) {
                    val typeface = ResourcesCompat.getFont(context, fontId)
                    it.typeface = typeface
                    it.setTextColor(Color.parseColor(fontColor))
                }
            }
        }
    }
}

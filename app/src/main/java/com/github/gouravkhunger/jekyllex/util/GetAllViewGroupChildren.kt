package com.github.gouravkhunger.jekyllex.util

import android.view.View
import android.view.ViewGroup

fun ViewGroup.getAllChildren(): List<View> {
    val children = ArrayList<View>()
    for (i in 0 until this.childCount) {
        if (getChildAt(i) is ViewGroup) {
            children.addAll((getChildAt(i) as ViewGroup).getAllChildren())
        }
        children.add(this.getChildAt(i))
    }
    return children
}

package com.github.gouravkhunger.jekyllex.ui.editor.pagerAdapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.github.gouravkhunger.jekyllex.ui.editor.fragments.EditingFragment
import com.github.gouravkhunger.jekyllex.ui.editor.fragments.PreviewFragment

class PageAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    companion object {
        internal const val EDITOR_SCREEN_POSITION = 0
        internal const val PREVIEW_SCREEN_POSITION = 1
    }

    override fun getItemCount(): Int {
        return 2
    }

    /*override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> EditingFragment()
            1 -> PreviewFragment()
            else -> EditingFragment()
        }
    }*/

    override fun createFragment(position: Int): Fragment = when(position) {
        EDITOR_SCREEN_POSITION -> EditingFragment()
        PREVIEW_SCREEN_POSITION -> PreviewFragment()
        else -> throw IllegalStateException("Invalid adapter position")
    }

    /*override fun getPageTitle(position: Int): CharSequence? {
        when (position) {
            0 -> {
                return "Edit File"
            }
            1 -> {
                return "Preview Changes"
            }
        }
        return super.getPageTitle(position)
    }*/

}
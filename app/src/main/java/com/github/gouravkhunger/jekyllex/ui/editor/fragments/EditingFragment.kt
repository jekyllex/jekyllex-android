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

package com.github.gouravkhunger.jekyllex.ui.editor.fragments

import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.gouravkhunger.jekyllex.databinding.FragmentEditingBinding
import com.github.gouravkhunger.jekyllex.ui.editor.EditorViewModel
import com.github.gouravkhunger.jekyllex.ui.editor.MarkdownEditor

// The fragment where one can write markdown
class EditingFragment : Fragment() {

    // View Binding variables.
    private var _editorBinding: FragmentEditingBinding? = null
    private val editorBinding get() = _editorBinding!!

    lateinit var viewModel: EditorViewModel

    // Initialise the view bindings and inflate the root view.
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _editorBinding = FragmentEditingBinding.inflate(inflater, container, false)
        return editorBinding.root
    }

    // Once the views are initialised, observe view model values and more.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = (activity as MarkdownEditor).viewModel

        // If the post is an existing one, set the initial
        // content on the editing area.
        viewModel.originalContent.observe(viewLifecycleOwner, {
            editorBinding.markdownEt.text = SpannableStringBuilder(it)
            editorBinding.previewBtnParent.visibility = View.GONE
        })

        // If the text gets updated, show the preview button.
        viewModel.isTextUpdated.observe(viewLifecycleOwner, {
            if (it) {
                editorBinding.previewBtnParent.visibility = View.VISIBLE
                editorBinding.previewBtn.changeDrawableTint(true)
            } else {
                editorBinding.previewBtnParent.visibility = View.GONE
                editorBinding.previewBtn.changeDrawableTint(false)
            }
        })

        // Synchronize scroll distance with the preview tab.
        editorBinding.editorScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            viewModel.setScrollDist(scrollY)
        }

        // Observe the scroll dist of the editor area and scroll to that distance.
        viewModel.scrollDist.observe(viewLifecycleOwner, {
            editorBinding.editorScrollView.smoothScrollTo(0, it)
        })

        // Save the text as it gets changed.
        editorBinding.markdownEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setNewText(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        // Clicking preview button should open the preview page and hide the
        // button's root parent view.
        editorBinding.previewBtn.setOnClickListener {
            (activity as MarkdownEditor).setCurrentPage(1)
            editorBinding.previewBtnParent.visibility = View.GONE
        }
    }

    // When the fragment destroys, destroy the view bindings too.
    override fun onDestroyView() {
        super.onDestroyView()
        _editorBinding = null
    }
}

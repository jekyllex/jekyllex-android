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

class EditingFragment : Fragment() {

    private var _editorBinding: FragmentEditingBinding? = null
    private val editorBinding get() = _editorBinding!!

    lateinit var viewModel: EditorViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _editorBinding = FragmentEditingBinding.inflate(inflater, container, false)
        return editorBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = (activity as MarkdownEditor).viewModel

        viewModel.originalContent.observe(viewLifecycleOwner, {
            editorBinding.markdownEt.text = SpannableStringBuilder(it)
            editorBinding.previewBtnParent.visibility = View.GONE
        })

        viewModel.isTextUpdated.observe(viewLifecycleOwner, {
            if (it) {
                editorBinding.previewBtnParent.visibility = View.VISIBLE
            } else {
                editorBinding.previewBtnParent.visibility = View.GONE
            }
        })

        editorBinding.editorScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            viewModel.setScrollDist(scrollY)
        }

        editorBinding.markdownEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setNewText(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        editorBinding.previewBtn.setOnClickListener {
            (activity as MarkdownEditor).setCurrentPage(1)
            editorBinding.previewBtnParent.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _editorBinding = null
    }
}

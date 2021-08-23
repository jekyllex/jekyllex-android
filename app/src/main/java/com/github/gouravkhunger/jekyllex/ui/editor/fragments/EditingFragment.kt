package com.github.gouravkhunger.jekyllex.ui.editor.fragments

import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.ui.editor.EditorViewModel
import com.github.gouravkhunger.jekyllex.ui.editor.MarkdownEditor
import kotlinx.android.synthetic.main.fragment_editing.*

class EditingFragment : Fragment(R.layout.fragment_editing) {

    lateinit var viewModel: EditorViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = (activity as MarkdownEditor).viewModel

        viewModel.originalContent.observe(viewLifecycleOwner, {
            markdownEt.text = SpannableStringBuilder(it)
            previewBtnParent.visibility = View.GONE
        })

        viewModel.isTextUpdated.observe(viewLifecycleOwner, {
            if(it){
                previewBtnParent.visibility = View.VISIBLE
            } else {
                previewBtnParent.visibility = View.GONE
            }
        })

        editorScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            viewModel.setScrollDist(scrollY)
        }

        markdownEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setNewText(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        previewBtn.setOnClickListener {
            (activity as MarkdownEditor).setCurrentPage(1)
            previewBtnParent.visibility = View.GONE
        }

    }
}
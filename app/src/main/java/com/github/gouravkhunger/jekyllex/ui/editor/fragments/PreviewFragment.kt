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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.gouravkhunger.jekyllex.databinding.FragmentPeviewBinding
import com.github.gouravkhunger.jekyllex.ui.editor.EditorViewModel
import com.github.gouravkhunger.jekyllex.ui.editor.MarkdownEditor
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin

class PreviewFragment : Fragment() {

    private var _previewBinding: FragmentPeviewBinding? = null
    private val previewBinding get() = _previewBinding!!
    lateinit var viewModel: EditorViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _previewBinding = FragmentPeviewBinding.inflate(inflater, container, false)
        return previewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val act = (activity as MarkdownEditor)
        viewModel = act.viewModel

        val markwon = Markwon.builder(act.baseContext)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(act.baseContext))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(GlideImagesPlugin.create(act.baseContext))
            .build()

        viewModel.text.observe(viewLifecycleOwner, {
            markwon.setMarkdown(previewBinding.previewTv, it)
        })

        viewModel.scrollDist.observe(viewLifecycleOwner, {
            previewBinding.previewScrollView.smoothScrollTo(0, it)
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _previewBinding = null
    }
}

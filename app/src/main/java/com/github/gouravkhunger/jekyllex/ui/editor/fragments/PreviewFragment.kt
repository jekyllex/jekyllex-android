package com.github.gouravkhunger.jekyllex.ui.editor.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.ui.editor.EditorViewModel
import com.github.gouravkhunger.jekyllex.ui.editor.MarkdownEditor
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import kotlinx.android.synthetic.main.fragment_peview.*

class PreviewFragment : Fragment(R.layout.fragment_peview) {

    lateinit var viewModel: EditorViewModel

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
            markwon.setMarkdown(previewTv, it)
        })

        viewModel.scrollDist.observe(viewLifecycleOwner, {
            previewScrollView.smoothScrollTo(0, it)
        })

    }

}
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

package com.github.gouravkhunger.jekyllex.ui.posts.adapter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.databinding.OtherPostItemBinding
import com.github.gouravkhunger.jekyllex.models.repo_content.RepoContentItemModel
import com.github.gouravkhunger.jekyllex.ui.editor.MarkdownEditor
import com.github.gouravkhunger.jekyllex.ui.posts.PostsActivity

// Adapter of RecyclerView present in Posts Activity
class PostsAdapter(private val activity: Activity) :
    RecyclerView.Adapter<PostsAdapter.PostsViewHolder>() {

    inner class PostsViewHolder(val binding: OtherPostItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    // differ callback that checks if elements are same of not
    private val differCallback = object : DiffUtil.ItemCallback<RepoContentItemModel>() {
        override fun areItemsTheSame(
            oldItem: RepoContentItemModel,
            newItem: RepoContentItemModel
        ): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(
            oldItem: RepoContentItemModel,
            newItem: RepoContentItemModel
        ): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    // inflate layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostsViewHolder {
        val binding =
            OtherPostItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostsViewHolder(binding)
    }

    // gets the size of the current list
    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    // sets content on the items.
    override fun onBindViewHolder(holder: PostsViewHolder, position: Int) {
        val post = differ.currentList[position]

        holder.binding.apply {
            val date = post.name.substring(0, 10)
            val postName = post.name.substring(11)

            rvPostName.text = postName
            rvPostPath.text = post.path

            rvPostDate.text =
                HtmlCompat.fromHtml(
                    activity.getString(R.string.written_on, date),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )

            rvPostFileSize.text =
                HtmlCompat.fromHtml(
                    activity.getString(
                        R.string.file_size,
                        Formatter.formatShortFileSize(activity, post.size)
                    ),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )

            openPostInBrowser.setOnClickListener {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(post.html_url)))
            }

            deletePost.setOnClickListener {
                val dialog = AlertDialog.Builder(activity)
                    .setTitle("Attention!")
                    .setMessage("Are you sure you want to delete this post?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { dialog, _ ->
                        (activity as PostsActivity).deletePost(
                            position,
                            post.name,
                            post.path,
                            post.sha
                        )
                        dialog.dismiss()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }

                val alert: AlertDialog = dialog.create()
                alert.window?.setBackgroundDrawableResource(R.drawable.rounded_corners)
                alert.show()
            }

            rvPostCard.setOnClickListener {
                activity.startActivity(
                    Intent(it.context, MarkdownEditor::class.java)
                        .putExtra("path", post.path)
                        .putExtra("sha", post.sha)
                )
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }
}

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

package com.github.gouravkhunger.jekyllex.ui.home.adapter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.databinding.OtherRepositoryItemBinding
import com.github.gouravkhunger.jekyllex.models.repository.RepoItemModel
import com.github.gouravkhunger.jekyllex.ui.posts.PostsActivity

// Adapter of RecyclerView present in Home Activity
class RepositoriesAdapter(private val activity: Activity) :
    RecyclerView.Adapter<RepositoriesAdapter.RepositoryViewHolder>() {

    inner class RepositoryViewHolder(val binding: OtherRepositoryItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    // differ callback that checks if elements are same of not
    private val differCallback = object : DiffUtil.ItemCallback<RepoItemModel>() {
        override fun areItemsTheSame(
            oldItem: RepoItemModel,
            newItem: RepoItemModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: RepoItemModel,
            newItem: RepoItemModel
        ): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    // inflate layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RepositoryViewHolder {
        val binding =
            OtherRepositoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RepositoryViewHolder(binding)
    }

    // gets the size of the current list
    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    // sets content on the items.
    override fun onBindViewHolder(holder: RepositoryViewHolder, position: Int) {
        val repo = differ.currentList[position]
        holder.binding.apply {
            rvRepoTitle.text = repo.name
            rvRepoDescription.text = repo.description
            rvRepoDescription.visibility =
                if (rvRepoDescription.text.isEmpty()) View.GONE else View.VISIBLE

            rvRepoLanguage.visibility =
                if (repo.language.isNullOrEmpty()) View.GONE else View.VISIBLE
            rvRepoLanguage.text = HtmlCompat.fromHtml(
                activity.getString(R.string.built_with, repo.language),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )

            rvRepoStars.visibility =
                if (repo.stargazers_count == 0) View.GONE else View.VISIBLE
            rvRepoStars.text = repo.stargazers_count.toString()

            rvRepoForks.visibility =
                if (repo.forks_count == 0) View.GONE else View.VISIBLE
            rvRepoForks.text = repo.forks_count.toString()

            if (
                rvRepoStars.visibility == View.GONE &&
                rvRepoForks.visibility == View.GONE &&
                rvRepoLanguage.visibility == View.GONE
            ) repoInfoNumbersParent.visibility = View.GONE

            rvRepoIsPrivate.visibility = if (repo.private) View.VISIBLE else View.GONE
            rvRepoIsArchived.visibility = if (repo.archived) View.VISIBLE else View.GONE

            rvRepositoryCard.setOnClickListener {
                activity.startActivity(
                    Intent(it.context, PostsActivity::class.java)
                        .putExtra("repo_name", repo.full_name)
                )
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

            openRepoInBrowser.setOnClickListener {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(repo.html_url)))
            }
        }
    }
}

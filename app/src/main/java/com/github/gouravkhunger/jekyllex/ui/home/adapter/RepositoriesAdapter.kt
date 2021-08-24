package com.github.gouravkhunger.jekyllex.ui.home.adapter

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.models.repository.RepoItemModel
import com.github.gouravkhunger.jekyllex.ui.posts.PostsActivity
import kotlinx.android.synthetic.main.other_repository_item.view.*
import androidx.core.text.HtmlCompat

// Adapter of RecyclerView present in Bookmarked Quotes Fragment
class RepositoriesAdapter(private val activity: Activity) :
    RecyclerView.Adapter<RepositoriesAdapter.RepositoryViewHolder>() {

    inner class RepositoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

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
        return RepositoryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.other_repository_item,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: RepositoryViewHolder, position: Int) {
        val repo = differ.currentList[position]
        holder.itemView.apply {
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

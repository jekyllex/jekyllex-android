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

package com.github.gouravkhunger.jekyllex.ui.posts

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.models.CommitModel
import com.github.gouravkhunger.jekyllex.models.repo_content.RepoContentItemModel
import com.github.gouravkhunger.jekyllex.repositories.GithubContentRepository
import com.github.gouravkhunger.jekyllex.ui.auth.AuthActivity
import com.github.gouravkhunger.jekyllex.ui.editor.MarkdownEditor
import com.github.gouravkhunger.jekyllex.ui.home.HomeActivity
import com.github.gouravkhunger.jekyllex.ui.posts.adapter.PostsAdapter
import com.github.gouravkhunger.jekyllex.util.isValidFileName
import com.github.gouravkhunger.jekyllex.util.preActivityStartChecks
import com.google.android.material.textfield.TextInputLayout
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_posts.*
import kotlinx.android.synthetic.main.other_newpost_alert.*
import kotlinx.android.synthetic.main.other_no_internet.*
import kotlinx.android.synthetic.main.other_no_posts.*

class PostsActivity : AppCompatActivity() {

    private lateinit var accessToken: String
    private lateinit var currentRepo: String
    private lateinit var viewModel: PostsViewModel
    private lateinit var repositoriesAdapter: PostsAdapter
    private var list: ArrayList<RepoContentItemModel>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (preActivityStartChecks(this)) {
            0 -> Unit
            1 -> {
                Toast.makeText(
                    this,
                    "Your session has expired...\nPlease log in again",
                    Toast.LENGTH_SHORT
                ).show()
                startActivity(Intent(this, AuthActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                finishAffinity()
                return
            }
            2 -> {
                Toast.makeText(this, "No Internet Connection...", Toast.LENGTH_SHORT).show()
                setContentView(R.layout.other_no_internet)
                retry.setOnClickListener {
                    startActivity(
                        Intent(this, HomeActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out)
                    finish()
                }
                return
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        accessToken = prefs.getString("access_token", "") ?: ""

        val repository = GithubContentRepository()
        val factory = PostsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(PostsViewModel::class.java)

        setTheme(R.style.Theme_JekyllEx)
        setContentView(R.layout.activity_posts)
        setSupportActionBar(toolbar_posts)
        supportActionBar?.setHomeButtonEnabled(true)
        toolbar_posts.setNavigationIcon(R.drawable.ic_back)
        toolbar_posts.setNavigationOnClickListener {
            onBackPressed()
        }

        val extras = intent.extras

        if (extras != null && accessToken.isNotEmpty()) {
            currentRepo = extras.getString("repo_name") ?: ""
            postToolbarTv.text = currentRepo
            viewModel.getRepoRootContent(currentRepo, "Bearer $accessToken")
            prefs.edit()
                .putString("current_repo", currentRepo)
                .apply()
        } else {
            Toast.makeText(this, "Unexpected error!", Toast.LENGTH_SHORT).show()
            onBackPressed()
        }

        viewModel.hasPosts.observe(this, {
            if (it) {
                rvPosts.visibility = View.VISIBLE
                postsProgressParent.visibility = View.VISIBLE
                noPosts.visibility = View.GONE
                val repoName = extras!!.getString("repo_name") ?: ""
                viewModel.getContentFromPath(true, repoName, "_posts", "Bearer $accessToken")
            } else {
                rvPosts.visibility = View.GONE
                postsProgressParent.visibility = View.GONE
                noPosts.visibility = View.VISIBLE
                goBack.setOnClickListener {
                    onBackPressed()
                }
            }
        })

        viewModel.posts.observe(this, {
            if (!it.isNullOrEmpty()) {
                list = it
                repositoriesAdapter.differ.submitList(list)
                rvPosts.visibility = View.VISIBLE
                postsProgressParent.visibility = View.GONE
            } else {
                rvPosts.visibility = View.GONE
                postsProgressParent.visibility = View.VISIBLE
                Toast.makeText(this, "An unexpected error occured!", Toast.LENGTH_LONG).show()
            }
        })

        setupRecyclerView()
    }

    // set adapter and layout manager on the recycler view
    private fun setupRecyclerView() {
        repositoriesAdapter = PostsAdapter(this)
        postsList.apply {
            adapter = repositoriesAdapter
            layoutManager = LinearLayoutManager(this@PostsActivity)
        }
    }

    fun deletePost(pos: Int, name: String, path: String, sha: String) {
        if (list != null && currentRepo.isNotEmpty() && accessToken.isNotEmpty()) {
            val commit = CommitModel("Deleted $name", null, sha)
            viewModel.deletePost(commit, currentRepo, path, "Bearer $accessToken")
            list!!.removeAt(pos)
            repositoriesAdapter.differ.submitList(list)
            repositoriesAdapter.notifyItemRemoved(pos)
            repositoriesAdapter.notifyItemRangeChanged(pos, repositoriesAdapter.differ.currentList.size)
        } else {
            Toast.makeText(this, "Current Repository not found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_posts, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.newPostMenuItem) {
            val dialogBuilder = AlertDialog.Builder(this)
            val inflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.other_newpost_alert, null)
            dialogBuilder.setView(dialogView)

            val alertDialog: AlertDialog = dialogBuilder.create()
            alertDialog.setCancelable(false)
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()

            val exampleTv = dialogView.findViewById<TextView>(R.id.exampleNewPost)
            val cancelDialog = dialogView.findViewById<Button>(R.id.cancelNewPostDialog)
            val createPost = dialogView.findViewById<Button>(R.id.createNewPostBtn)
            val fileNameEt = dialogView.findViewById<EditText>(R.id.newPostEt)
            val etParent = dialogView.findViewById<TextInputLayout>(R.id.newPostEtLayout)

            exampleTv.text = HtmlCompat.fromHtml(
                getString(R.string.file_name_example),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )

            cancelDialog.setOnClickListener {
                alertDialog.dismiss()
            }
            createPost.setOnClickListener {
                if (isValidFileName(fileNameEt.text.toString())) {
                    etParent.error = ""
                    startActivity(
                        Intent(it.context, MarkdownEditor::class.java)
                            .putExtra("path", "_posts/${fileNameEt.text}")
                            .putExtra("isNew", true)
                    )
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    alertDialog.dismiss()
                } else {
                    etParent.error = "Invalid File Name!"
                }
            }
            fileNameEt.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    etParent.error = ""
                }
            })
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        viewModel.getRepoRootContent(currentRepo, "Bearer $accessToken")
        super.onResume()
    }

    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

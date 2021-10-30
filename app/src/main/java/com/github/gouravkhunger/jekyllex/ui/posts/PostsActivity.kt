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
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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
import com.github.gouravkhunger.jekyllex.databinding.ActivityPostsBinding
import com.github.gouravkhunger.jekyllex.databinding.OtherNoInternetBinding
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

class PostsActivity : AppCompatActivity() {

    // View binding variables
    private lateinit var postsBinding: ActivityPostsBinding
    private lateinit var noInternetBinding: OtherNoInternetBinding

    // Other variables
    private lateinit var prefs: SharedPreferences
    private lateinit var accessToken: String
    private lateinit var currentRepo: String
    private lateinit var viewModel: PostsViewModel
    private lateinit var repositoriesAdapter: PostsAdapter
    private var list: ArrayList<RepoContentItemModel>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise view bindings
        postsBinding = ActivityPostsBinding.inflate(layoutInflater)
        noInternetBinding = OtherNoInternetBinding.inflate(layoutInflater)

        // Activity start pre-checks
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
                setContentView(noInternetBinding.root)
                noInternetBinding.retry.setOnClickListener {
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

        // Get saved access token
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        accessToken = prefs.getString("access_token", "") ?: ""

        // Initialise the view-model with the required dependencies.
        val repository = GithubContentRepository()
        val factory = PostsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[PostsViewModel::class.java]

        // Once all variables are set, show the UI to the user.
        setTheme(R.style.Theme_JekyllEx)
        setContentView(postsBinding.root)
        setSupportActionBar(postsBinding.toolbarPosts)
        supportActionBar?.setHomeButtonEnabled(true)
        postsBinding.toolbarPosts.setNavigationIcon(R.drawable.ic_back)
        postsBinding.toolbarPosts.setNavigationOnClickListener {
            onBackPressed()
        }
        postsBinding.toolbarPosts.applyFont()

        val extras = intent.extras

        if (extras != null && accessToken.isNotEmpty()) {
            // Try to extract the passed values.
            currentRepo = extras.getString("repo_name") ?: ""
            postsBinding.postToolbarTv.text = currentRepo
            viewModel.getRepoRootContent(currentRepo, "Bearer $accessToken")
            prefs.edit()
                .putString("current_repo", currentRepo)
                .apply()
        } else {
            // If nothing is passed to the activity, show an error.
            Toast
                .makeText(this, "Required parameters not passed!", Toast.LENGTH_SHORT)
                .show()
            onBackPressed()
        }

        // hasPosts denotes if the current repository is a jekyll blog or not.
        viewModel.hasPosts.observe(this, {
            if (it) {
                // If it is a jekyll blog, get the posts in it
                postsBinding.rvPosts.visibility = View.VISIBLE
                postsBinding.postsProgressParent.visibility = View.VISIBLE
                postsBinding.noPosts.visibility = View.GONE
                val repoName = extras!!.getString("repo_name") ?: ""
                viewModel.getContentFromPath(true, repoName, "_posts", "Bearer $accessToken")
            } else {
                // If it is not a jekyll blog, inform the user.
                postsBinding.rvPosts.visibility = View.GONE
                postsBinding.postsProgressParent.visibility = View.GONE
                postsBinding.noPosts.visibility = View.VISIBLE
                postsBinding.goBack.setOnClickListener {
                    onBackPressed()
                }
            }
            invalidateOptionsMenu()
        })

        // once the github api returns the post inside the "_post" folder of
        // the repository, show them to the user.
        viewModel.posts.observe(this, {
            if (!it.isNullOrEmpty()) {
                list = it
                repositoriesAdapter.differ.submitList(list)
                postsBinding.rvPosts.visibility = View.VISIBLE
                postsBinding.postsProgressParent.visibility = View.GONE
            } else {
                postsBinding.rvPosts.visibility = View.GONE
                postsBinding.postsProgressParent.visibility = View.VISIBLE
                Toast.makeText(this, "An unexpected error occured!", Toast.LENGTH_LONG).show()
            }
        })

        // Initialise up the recycler view.
        setupRecyclerView()
    }

    // set adapter and layout manager on the recycler view
    private fun setupRecyclerView() {
        repositoriesAdapter = PostsAdapter(this)
        postsBinding.postsList.apply {
            adapter = repositoriesAdapter
            layoutManager = LinearLayoutManager(this@PostsActivity)
        }
    }

    // function that will be called by the adapter to delete a specific post.
    fun deletePost(pos: Int, name: String, path: String, sha: String) {
        if (list != null && currentRepo.isNotEmpty() && accessToken.isNotEmpty()) {
            val commit = CommitModel("Deleted $name", null, sha)
            viewModel.deletePost(commit, currentRepo, path, "Bearer $accessToken")
            list!!.removeAt(pos)
            repositoriesAdapter.differ.submitList(list)
            repositoriesAdapter.notifyItemRemoved(pos)
            repositoriesAdapter.notifyItemRangeChanged(
                pos,
                repositoriesAdapter.differ.currentList.size
            )
        } else {
            Toast.makeText(
                this,
                "Required variables not passed to the Activity!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Inflate the menu.
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_posts, menu)

        val newPostItem = menu?.findItem(R.id.newPostMenuItem)
        if (newPostItem != null) {
            val tint = prefs.getString("primaryTextColor", "#ffffff")
            newPostItem.icon.colorFilter = PorterDuffColorFilter(Color.parseColor(tint), PorterDuff.Mode.SRC_IN)
        }

        newPostItem?.isVisible = viewModel.hasPosts.value ?: false

        return super.onCreateOptionsMenu(menu)
    }

    // Handle menu item clicks.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.newPostMenuItem) {
            // Show the make a new post dialog.
            val dialogBuilder = AlertDialog.Builder(this)
            val inflater = this.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.other_newpost_alert, null)
            dialogBuilder.setView(dialogView)

            val alertDialog: AlertDialog = dialogBuilder.create()
            alertDialog.setCancelable(false)
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()

            // Initialise views present in the dialog.
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
                // validate file name
                if (isValidFileName(fileNameEt.text.toString())) {
                    // if filename is correct, take the user to the editor activity
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
            // once the text is edited, remove the errors on the edittext.
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

    // Get the root content on activity resume.
    override fun onResume() {
        viewModel.getRepoRootContent(currentRepo, "Bearer $accessToken")
        super.onResume()
    }

    // Override the default behaviour on back button pressed.
    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

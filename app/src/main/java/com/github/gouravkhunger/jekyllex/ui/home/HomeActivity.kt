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

package com.github.gouravkhunger.jekyllex.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.databinding.ActivityHomeBinding
import com.github.gouravkhunger.jekyllex.databinding.OtherNoInternetBinding
import com.github.gouravkhunger.jekyllex.repositories.GithubContentRepository
import com.github.gouravkhunger.jekyllex.ui.auth.AuthActivity
import com.github.gouravkhunger.jekyllex.ui.content.ContentActivity
import com.github.gouravkhunger.jekyllex.ui.home.adapter.RepositoriesAdapter
import com.github.gouravkhunger.jekyllex.ui.profile.ProfileActivity
import com.github.gouravkhunger.jekyllex.util.preActivityStartChecks
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.enums.UpdateFrom

class HomeActivity : AppCompatActivity() {

    // view binding variables
    private lateinit var homeBinding: ActivityHomeBinding
    private lateinit var noInternetBinding: OtherNoInternetBinding

    // Variables used by this activity
    private lateinit var userId: String
    private lateinit var picUrl: String
    private lateinit var accessToken: String
    private lateinit var repositoriesAdapter: RepositoriesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise view binding
        homeBinding = ActivityHomeBinding.inflate(layoutInflater)
        noInternetBinding = OtherNoInternetBinding.inflate(layoutInflater)

        // Activity start pre checks
        when (preActivityStartChecks(this)) {
            0 -> Unit
            1 -> {
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

        // get simple user data from sharedpreferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        userId = prefs.getString("user_id", "") ?: ""
        picUrl = prefs.getString("pic_url", "") ?: ""
        accessToken = prefs.getString("access_token", "") ?: ""

        // Initialise view model with the required dependencies.
        val repository = GithubContentRepository()
        val factory = HomeViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)

        // All variables are set, set the theme and layout now
        setTheme(R.style.Theme_JekyllEx)
        setContentView(homeBinding.root)
        setSupportActionBar(homeBinding.toolbarHome)
        supportActionBar?.title = ""

        // set up the recycler view that will hold 4
        setupRecyclerView()

        // load user's profile picture into the image view.
        Glide
            .with(this)
            .load(picUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_user)
            .into(homeBinding.profileIcon)

        homeBinding.profileIcon.setOnClickListener {
            // Open the profile page if profile icon is clicked
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        viewModel.userRepos.observe(this, {
            if (it.isNotEmpty()) {
                // if user's repositories are not empty, show the list.
                repositoriesAdapter.differ.submitList(it)
                homeBinding.loadingMessageParent.visibility = View.GONE
                homeBinding.repositoriesParent.visibility = View.VISIBLE
                homeBinding.selectRepoTv.visibility = View.VISIBLE
            } else {
                homeBinding.loadingMessageParent.visibility = View.VISIBLE
                homeBinding.repositoriesParent.visibility = View.GONE
                homeBinding.selectRepoTv.visibility = View.GONE
            }
        })

        // fetch the user's repositories.
        viewModel.getUserRepositories("Bearer $accessToken")

        // Show what the floating action button does, when it is long pressed.
        homeBinding.fabReport.setOnLongClickListener {
            Toast.makeText(this, getString(R.string.report_msg), Toast.LENGTH_SHORT).show()
            true
        }

        // Open github issues when it is clicked.
        homeBinding.fabReport.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.issues_url))
                )
            )
        }

        // At the end, look for app updates, if available...
        AppUpdater(this).setUpdateFrom(UpdateFrom.GITHUB)
            .setGitHubUserAndRepo("gouravkhunger", "jekyllex-andriod")
            .start()
    }

    // inflates the options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_home, menu)

        return super.onCreateOptionsMenu(menu)
    }

    // Handle general item clicks in the menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.website -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.website_url))
                    )
                )
            }
            R.id.gh_repo -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.gh_repo_url))
                    )
                )
            }
            R.id.documentation -> {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.docs_url))
                    )
                )
            }
            R.id.share -> {
                val sendIntent = Intent()
                sendIntent.apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        getString(R.string.share_text)
                    )
                }
                sendIntent.type = "text/plain"
                startActivity(sendIntent)
            }
            else -> {
                // These items open the content activity,
                // but pass different IDs and title for the content to be shown.
                // https://genicsblog.com/how-to-make-efficient-content-applications-in-android
                val id = when (item.itemId) {
                    R.id.privacyPolicy -> R.string.privacy_policy_text
                    R.id.tnc -> R.string.tnc_text
                    else -> -1
                }
                val title = when (item.itemId) {
                    R.id.privacyPolicy -> "Privacy Policy"
                    R.id.tnc -> "Terms and Conditions"
                    else -> ""
                }
                startActivity(
                    Intent(this, ContentActivity::class.java)
                        .putExtra("id", id)
                        .putExtra("title", title)
                )
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // function to set adapter and layout manager on the recycler view
    private fun setupRecyclerView() {
        repositoriesAdapter = RepositoriesAdapter(this)
        homeBinding.repositoryList.apply {
            adapter = repositoriesAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
        }
    }
}

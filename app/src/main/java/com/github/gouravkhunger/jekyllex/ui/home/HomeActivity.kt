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
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.repositories.UserReposRepository
import com.github.gouravkhunger.jekyllex.ui.auth.AuthActivity
import com.github.gouravkhunger.jekyllex.ui.home.adapter.RepositoriesAdapter
import com.github.gouravkhunger.jekyllex.ui.profile.ProfileActivity
import com.github.gouravkhunger.jekyllex.util.preActivityStartChecks
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.other_no_internet.*

class HomeActivity : AppCompatActivity() {

    private lateinit var userId: String
    private lateinit var picUrl: String
    private lateinit var accessToken: String
    private lateinit var repositoriesAdapter: RepositoriesAdapter

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
                finish()
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
        userId = prefs.getString("user_id", "") ?: ""
        picUrl = prefs.getString("pic_url", "") ?: ""
        accessToken = prefs.getString("access_token", "") ?: ""

        val repository = UserReposRepository()
        val factory = HomeViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)

        setTheme(R.style.Theme_JekyllEx)

        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar_home)
        supportActionBar?.title = ""

        setupRecyclerView()

        Glide.with(this).load(picUrl).circleCrop().into(profileIcon)

        profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        viewModel.userRepos.observe(this, {
            if (it.isNotEmpty()) {
                repositoriesAdapter.differ.submitList(it)
                loadingMessageParent.visibility = View.GONE
                repositoriesParent.visibility = View.VISIBLE
                selectRepoTv.visibility = View.VISIBLE
            } else {
                loadingMessageParent.visibility = View.VISIBLE
                repositoriesParent.visibility = View.GONE
                selectRepoTv.visibility = View.GONE
            }
        })

        viewModel.getUserRepositories("Bearer $accessToken")

        fabReport.setOnLongClickListener {
            Toast.makeText(this, getString(R.string.report_msg), Toast.LENGTH_SHORT).show()
            true
        }

        fabReport.setOnClickListener {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.app_repository_url))
                )
            )
        }
    }

    // function to set adapter and layout manager on the recycler view
    private fun setupRecyclerView() {
        repositoriesAdapter = RepositoriesAdapter(this)
        repositoryList.apply {
            adapter = repositoriesAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
        }
    }
}

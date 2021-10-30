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

package com.github.gouravkhunger.jekyllex.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.SecureCredentialsManager
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.callback.Callback
import com.auth0.android.provider.WebAuthProvider
import com.bumptech.glide.Glide
import com.github.gouravkhunger.jekyllex.BuildConfig
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.databinding.ActivityProfileBinding
import com.github.gouravkhunger.jekyllex.databinding.OtherNoInternetBinding
import com.github.gouravkhunger.jekyllex.db.userdb.UserDataBase
import com.github.gouravkhunger.jekyllex.models.user.UserModel
import com.github.gouravkhunger.jekyllex.repositories.UserRepository
import com.github.gouravkhunger.jekyllex.ui.auth.AuthActivity
import com.github.gouravkhunger.jekyllex.ui.home.HomeActivity
import com.github.gouravkhunger.jekyllex.util.preActivityStartChecks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    // View binding
    private lateinit var profileBinding: ActivityProfileBinding
    private lateinit var noInternetBinding: OtherNoInternetBinding

    // Other variables
    private lateinit var viewModel: ProfileViewModel
    private lateinit var accessToken: String
    private lateinit var username: String
    private lateinit var account: Auth0
    private lateinit var manager: SecureCredentialsManager
    private lateinit var apiClient: AuthenticationAPIClient
    private var user: UserModel? = null
    private var canGoBack = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise view bindings
        profileBinding = ActivityProfileBinding.inflate(layoutInflater)
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
                finish()
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

        // get required values from shared preference storage.
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        username = prefs.getString("user_id", "") ?: ""
        accessToken = prefs.getString("access_token", "") ?: ""

        // Initialise the auth0 account
        account = Auth0(
            BuildConfig.Auth0ClientId,
            getString(R.string.com_auth0_domain)
        )
        apiClient = AuthenticationAPIClient(account)
        manager = SecureCredentialsManager(this, apiClient, SharedPreferencesStorage(this))

        // Initialise values
        val repository = UserRepository(UserDataBase(this))
        val factory = ProfileViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[ProfileViewModel::class.java]

        // Set the theme and UI elements
        setTheme(R.style.Theme_JekyllEx)
        setContentView(profileBinding.root)
        setSupportActionBar(profileBinding.toolbarProfile)
        supportActionBar?.setHomeButtonEnabled(true)
        profileBinding.toolbarProfile.setNavigationIcon(R.drawable.ic_back)
        profileBinding.toolbarProfile.setNavigationOnClickListener {
            onBackPressed()
        }
        profileBinding.toolbarProfile.applyFont()

        // Observe and set user profile values into the views from the local db
        viewModel.getUserProfile(username).observe(this, {
            if (it != null) {
                canGoBack = true
                user = it
                profileBinding.apply {
                    Glide
                        .with(this@ProfileActivity)
                        .load(it.picture)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user)
                        .into(profilePicImgView)

                    userName.text = it.name
                    userBio.text = it.bio

                    userGithub.text = it.nickname
                    githubParent.setOnClickListener {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/${userGithub.text}")
                            )
                        )
                    }

                    if (it.email.isNullOrEmpty()) {
                        emailParent.visibility = View.GONE
                    } else {
                        userEmail.text = it.email
                        emailParent.setOnClickListener {
                            val mailIntent = Intent(Intent.ACTION_SENDTO)
                            mailIntent.data = Uri.parse("mailto:")
                            mailIntent.putExtra(
                                Intent.EXTRA_EMAIL,
                                arrayOf(userEmail.text.toString())
                            )
                            startActivity(mailIntent)
                        }
                    }

                    if (it.location.isNullOrEmpty()) {
                        locationParent.visibility = View.GONE
                    } else {
                        userLocation.text = it.location
                    }

                    userId.text = getString(R.string.uid, it.user_id)

                    followersTv.text = it.followers.toString()
                    followingTv.text = it.following.toString()

                    if (it.blog.isNullOrEmpty()) {
                        blogParent.visibility = View.GONE
                    } else {
                        userBlog.text = it.blog.toString()
                        blogParent.setOnClickListener {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(userBlog.text.toString())
                                )
                            )
                        }
                    }

                    if (it.twitter_username.isNullOrEmpty()) {
                        twitterParent.visibility = View.GONE
                    } else {
                        userTwitter.text = it.twitter_username
                        twitterParent.setOnClickListener {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://twitter.com/${userTwitter.text}")
                                )
                            )
                        }
                    }

                    userProfileParentView.visibility = View.VISIBLE
                    profileLoadingProgress.visibility = View.GONE
                }
            } else {
                // If the profile is still loading/deleted, show progress bar
                profileBinding.apply {
                    userProfileParentView.visibility = View.GONE
                    profileLoadingProgress.visibility = View.VISIBLE
                }
            }
        })
    }

    // Inflate the options menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_profile, menu)

        return super.onCreateOptionsMenu(menu)
    }

    // Handle menu itmes pressed.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                // Confirm logout selection.
                val dialog = AlertDialog.Builder(this)
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to logout?")
                    .setCancelable(false)
                    .setPositiveButton("Log Out") { _, _ ->
                        // logout the user.
                        logout()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }

                val alert: AlertDialog = dialog.create()
                alert.window?.setBackgroundDrawableResource(R.drawable.rounded_corners)
                alert.show()
            }
            R.id.refreshProfile -> {
                Toast.makeText(
                    this,
                    "Refreshing profile is discontinued.\n" +
                        "Login again to trigger refresh.",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> Unit
        }
        return super.onOptionsItemSelected(item)
    }

    // override back button pressed.
    // The user shouldn't be able to go back if the profile is refreshing, else
    // app may crash.
    override fun onBackPressed() {
        if (canGoBack) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            Toast.makeText(this, "Please wait, refreshing profile...", Toast.LENGTH_SHORT).show()
        }
    }

    // Function that clears the local stored values and logouts the user.
    private fun logout() {
        PreferenceManager
            .getDefaultSharedPreferences(this)
            .edit()
            .clear()
            .apply()

        if (user != null) {
            CoroutineScope(Dispatchers.Default).launch {
                viewModel.deleteUser(user!!)
            }
        }

        WebAuthProvider.logout(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .start(
                this,
                object : Callback<Void?, AuthenticationException> {
                    override fun onSuccess(result: Void?) {
                        // The user has been logged out!
                        Toast.makeText(
                            this@ProfileActivity,
                            "Successfully Logged Out",
                            Toast.LENGTH_SHORT
                        ).show()

                        manager.clearCredentials()

                        finishAffinity()

                        val intent = Intent(this@ProfileActivity, AuthActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

                        startActivity(intent)
                    }

                    override fun onFailure(error: AuthenticationException) {
                        // Something went wrong!
                        Toast.makeText(
                            this@ProfileActivity,
                            "An error occurred!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
    }
}

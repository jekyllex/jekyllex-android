package com.github.gouravkhunger.jekyllex.ui.profile

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.github.gouravkhunger.jekyllex.db.userdb.UserDataBase
import com.github.gouravkhunger.jekyllex.repositories.UserRepository
import com.github.gouravkhunger.jekyllex.ui.auth.AuthActivity
import com.github.gouravkhunger.jekyllex.ui.home.HomeActivity
import com.github.gouravkhunger.jekyllex.util.preActivityStartChecks
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.other_no_internet.*

class ProfileActivity : AppCompatActivity() {
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
        val uid = prefs.getString("user_id", "") ?: ""

        val repository = UserRepository(UserDataBase(this))
        val factory = ProfileViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory).get(ProfileViewModel::class.java)

        setTheme(R.style.Theme_JekyllEx)
        setContentView(R.layout.activity_profile)
        setSupportActionBar(toolbar_profile)
        supportActionBar?.setHomeButtonEnabled(true)
        toolbar_profile.setNavigationIcon(R.drawable.ic_back)
        toolbar_profile.setNavigationOnClickListener {
            onBackPressed()
        }

        viewModel.getUserProfile(uid).observe(this, {
            if (it != null) {
                Glide.with(this).load(it.picture).circleCrop().into(profilePicImgView)

                userName.text = it.name
                userEmail.text = it.email
                userId.text = it.user_id

                userProfileParentView.visibility = View.VISIBLE
                profileLoadingProgress.visibility = View.GONE
            } else {
                userProfileParentView.visibility = View.GONE
                profileLoadingProgress.visibility = View.VISIBLE
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_profile, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.logout) {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to logout?")
                .setCancelable(false)
                .setPositiveButton("Log Out") { _, _ ->
                    logout()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }

            val alert: AlertDialog = dialog.create()
            alert.window?.setBackgroundDrawableResource(R.drawable.rounded_corners)
            alert.show()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun logout() {
        PreferenceManager
            .getDefaultSharedPreferences(this)
            .edit()
            .clear()
            .apply()

        val account = Auth0(
            BuildConfig.Auth0ClientId,
            getString(R.string.com_auth0_domain)
        )

        val apiClient = AuthenticationAPIClient(account)
        val manager = SecureCredentialsManager(this, apiClient, SharedPreferencesStorage(this))

        WebAuthProvider.logout(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .start(this, object : Callback<Void?, AuthenticationException> {
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
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

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
            })
    }
}

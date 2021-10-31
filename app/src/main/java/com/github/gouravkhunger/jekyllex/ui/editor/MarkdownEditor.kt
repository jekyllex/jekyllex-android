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

package com.github.gouravkhunger.jekyllex.ui.editor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.databinding.ActivityEditorBinding
import com.github.gouravkhunger.jekyllex.databinding.OtherNoInternetBinding
import com.github.gouravkhunger.jekyllex.models.CommitModel
import com.github.gouravkhunger.jekyllex.repositories.GithubContentRepository
import com.github.gouravkhunger.jekyllex.ui.auth.AuthActivity
import com.github.gouravkhunger.jekyllex.ui.editor.pagerAdapter.ViewPagerAdapter
import com.github.gouravkhunger.jekyllex.ui.home.HomeActivity
import com.github.gouravkhunger.jekyllex.util.preActivityStartChecks
import com.github.gouravkhunger.jekyllex.util.stringToBase64
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText

class MarkdownEditor : AppCompatActivity() {

    // Variables
    private lateinit var mdEditorBinding: ActivityEditorBinding
    private lateinit var noInternetBinding: OtherNoInternetBinding
    private lateinit var fileSha: String
    private lateinit var currentRepo: String
    private lateinit var path: String
    private lateinit var prefs: SharedPreferences
    lateinit var viewModel: EditorViewModel
    private var isNew = false
    private var accessToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise view bindings
        mdEditorBinding = ActivityEditorBinding.inflate(layoutInflater)
        noInternetBinding = OtherNoInternetBinding.inflate(layoutInflater)

        // activity start pre-checks.
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

        // Initialise view-model
        val repository = GithubContentRepository()
        val factory = EditorViewModelFactory(repository)
        viewModel =
            ViewModelProvider(this, factory)[EditorViewModel::class.java]

        // get required data from the share-preferences.
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        currentRepo = prefs.getString("current_repo", "") ?: ""
        accessToken = prefs.getString("access_token", "") ?: ""

        // Once all the variables are set, show the main content to the user.
        setTheme(R.style.Theme_JekyllEx)
        setContentView(mdEditorBinding.root)

        // set the custom toolbar as the action bar.
        setSupportActionBar(mdEditorBinding.toolbarEditor)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        mdEditorBinding.toolbarEditor.setNavigationIcon(R.drawable.ic_back)
        mdEditorBinding.toolbarEditor.setNavigationOnClickListener {
            onBackPressed()
        }
        mdEditorBinding.toolbarEditor.applyFont()

        val extras = intent.extras

        // try to get data about the post which is to be edited.
        if (extras != null && accessToken.isNotEmpty()) {
            path = extras.getString("path", "")
            fileSha = extras.getString("sha", "")
            isNew = extras.getBoolean("isNew")
            if (!isNew) viewModel.getContent(currentRepo, path, "Bearer $accessToken")
            else viewModel.setOriginalText("")
        } else {
            Toast.makeText(this, "No post to be edited!", Toast.LENGTH_SHORT).show()
            onBackPressed()
        }

        // if an existing post is edited, get its content and set it to the edittest.
        // and the preview text view.
        viewModel.originalContent.observe(this, {
            if (it != null) {
                showEditorArea()
                viewModel.setNewText(it)
            } else {
                hideEditorArea()
            }
        })

        // If post meta data or the content of the post is updated,
        // show the upload post button on the menu.
        viewModel.postMetaData.observe(this, {
            invalidateOptionsMenu()
        })
        viewModel.isTextUpdated.observe(this, {
            invalidateOptionsMenu()
        })

        // Set view pager's attributes
        mdEditorBinding.editorViewPager.apply {
            adapter = ViewPagerAdapter(this@MarkdownEditor)
            currentItem = 0
        }

        // Set titles on the Tab Layout items.
        TabLayoutMediator(
            mdEditorBinding.editorTabLayout,
            mdEditorBinding.editorViewPager
        ) { currentTab, currentPosition ->
            currentTab.text = when (currentPosition) {
                0 -> getString(R.string.edit_file)
                1 -> getString(R.string.preview_changes)
                else -> ""
            }
        }.attach()

        mdEditorBinding.editorTabLayout.formatTextViews()

        if (isNew) showEditorArea()
    }

    // public function that hides the keyboard and sets the current page
    // of the view pager to whatever is passed in.
    fun setCurrentPage(id: Int) {
        this.currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }

        mdEditorBinding.editorViewPager.currentItem = id
    }

    // private functions to show or hide the editing area on demand.
    private fun showEditorArea() {
        mdEditorBinding.editorProgressBarParent.visibility = View.GONE
        mdEditorBinding.editorTabLayout.visibility = View.VISIBLE
        mdEditorBinding.editorViewPager.visibility = View.VISIBLE
    }
    private fun hideEditorArea() {
        mdEditorBinding.editorProgressBarParent.visibility = View.GONE
        mdEditorBinding.editorTabLayout.visibility = View.GONE
        mdEditorBinding.editorViewPager.visibility = View.GONE
    }

    // Handle the Menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_editor, menu)

        val tint = prefs.getString("primaryTextColor", "#ffffff")

        val uploadPostMenuItem = menu.findItem(R.id.uploadPostMenuItem)
        uploadPostMenuItem.isVisible = viewModel.isTextUpdated.value ?: false
        uploadPostMenuItem.icon.colorFilter = PorterDuffColorFilter(Color.parseColor(tint), PorterDuff.Mode.SRC_IN)

        val editMetaDataMenuItem = menu.findItem(R.id.editMetaData)
        editMetaDataMenuItem.icon.colorFilter = PorterDuffColorFilter(Color.parseColor(tint), PorterDuff.Mode.SRC_IN)

        return super.onCreateOptionsMenu(menu)
    }

    // Handle item clicks on the menu.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.editMetaData -> {
                // show meta data editor dialog
                val dialogBuilder = AlertDialog.Builder(this)
                val inflater = this.layoutInflater
                val dialogView: View = inflater.inflate(R.layout.other_metadata_alert, null)
                dialogBuilder.setView(dialogView)

                val alertDialog: AlertDialog = dialogBuilder.create()
                alertDialog.setCancelable(false)
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alertDialog.show()

                // get reference to the views in the dialog
                val saveMetaDataBtn = dialogView.findViewById<View>(R.id.saveMetaData) as Button
                val closeMetaDataBtn =
                    dialogView.findViewById<View>(R.id.closeMetaDataDialog) as Button
                val metaDataEt = dialogView.findViewById<View>(R.id.metaDataEt) as EditText

                metaDataEt.setOnFocusChangeListener { view, hasFocus ->
                    if (!hasFocus) {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                }

                // if meta data is already present, set it to the edit text
                if (!viewModel.postMetaData.value.isNullOrEmpty()) {
                    metaDataEt.text = SpannableStringBuilder(viewModel.postMetaData.value)
                }

                saveMetaDataBtn.setOnClickListener {
                    viewModel.saveMetaData(metaDataEt.text.toString())
                    alertDialog.dismiss()
                }
                closeMetaDataBtn.setOnClickListener {
                    alertDialog.dismiss()
                }
            }
            R.id.uploadPostMenuItem -> {
                // show the commit message dialog
                val dialogBuilder = AlertDialog.Builder(this)
                val inflater = this.layoutInflater
                val dialogView: View = inflater.inflate(R.layout.other_commitmessage_alert, null)
                dialogBuilder.setView(dialogView)

                var alertDialog: AlertDialog = dialogBuilder.create()
                alertDialog.setCancelable(false)
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alertDialog.show()

                // get reference to the views in the dialog
                val uploadPostBtn = dialogView.findViewById<Button>(R.id.uploadPostBtn)
                val closeCommitMsgBtn =
                    dialogView.findViewById<Button>(R.id.closeCommitMessageDialog)
                val commitMessageEt = dialogView.findViewById<TextInputEditText>(R.id.commitMessageEt)
                val commitMsgParent =
                    dialogView.findViewById<LinearLayout>(R.id.commitMessageParent)
                val progressGroup =
                    dialogView.findViewById<LinearLayout>(R.id.postUploadingProgressParent)

                commitMessageEt.setOnFocusChangeListener { view, hasFocus ->
                    if (!hasFocus) {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                }

                uploadPostBtn.setOnClickListener {
                    val commitMessage = commitMessageEt.text.toString()
                    val postContent = viewModel.text.value ?: ""
                    val postMetaData = viewModel.postMetaData.value ?: ""

                    // check if anything is empty
                    if (
                        commitMessage.isEmpty() ||
                        postContent.isEmpty() ||
                        postMetaData.isEmpty() ||
                        currentRepo.isEmpty() ||
                        path.isEmpty()
                    ) {
                        Toast
                            .makeText(this, "Some fields are missing!", Toast.LENGTH_SHORT)
                            .show()
                        alertDialog.dismiss()
                    } else {
                        val encodedContent = stringToBase64(postMetaData, postContent)
                        commitMsgParent.visibility = View.GONE
                        progressGroup.visibility = View.VISIBLE

                        val commit =
                            if (isNew) CommitModel(commitMessage, encodedContent, null)
                            else CommitModel(commitMessage, encodedContent, fileSha)

                        // upload the post to github repo
                        viewModel.uploadPost(commit, currentRepo, path, "Bearer $accessToken")

                        viewModel.isUploaded.observe(this, {
                            if (it) {
                                // ic the post is uploaded
                                alertDialog.dismiss()
                                dialogBuilder
                                    .setView(null)
                                    .setTitle("Post Uploaded!")
                                    .setMessage("Do you wish to continue editing?")
                                    .setPositiveButton("Yes") { dialog, _ ->
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton("No") { dialog, _ ->
                                        dialog.dismiss()
                                        onBackPressed()
                                    }
                                alertDialog = dialogBuilder.create()
                                alertDialog.window?.setBackgroundDrawableResource(R.drawable.rounded_corners)
                                alertDialog.show()
                            } else {
                                Toast
                                    .makeText(
                                        this,
                                        "Some unexpected error occurred!",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                                alertDialog.dismiss()
                            }
                        })
                    }
                }
                closeCommitMsgBtn.setOnClickListener {
                    alertDialog.dismiss()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // Handle different cases on what can happen
    // when the back button is pressed.
    override fun onBackPressed() {
        if (mdEditorBinding.editorViewPager.currentItem == 1) {
            mdEditorBinding.editorViewPager.currentItem = 0
        } else if (!viewModel.isTextUpdated.value!!) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder
                .setTitle("Un-committed changes!")
                .setMessage("Proceed without saving the changes?")
                .setPositiveButton("Yes") { _, _ ->
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }

            val alertDialog: AlertDialog = dialogBuilder.create()
            alertDialog.setCancelable(false)
            alertDialog.window?.setBackgroundDrawableResource(R.drawable.rounded_corners)
            alertDialog.show()
        }
    }
}

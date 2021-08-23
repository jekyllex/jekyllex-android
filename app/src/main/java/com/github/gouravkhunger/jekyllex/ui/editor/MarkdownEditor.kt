package com.github.gouravkhunger.jekyllex.ui.editor

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.ui.editor.pagerAdapter.PageAdapter
import kotlinx.android.synthetic.main.activity_editor.*

import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.viewpager2.widget.ViewPager2
import com.github.gouravkhunger.jekyllex.models.CommitModel
import com.github.gouravkhunger.jekyllex.repositories.GithubContentRepository
import com.github.gouravkhunger.jekyllex.ui.auth.AuthActivity
import com.github.gouravkhunger.jekyllex.ui.home.HomeActivity
import com.github.gouravkhunger.jekyllex.util.preActivityStartChecks
import com.github.gouravkhunger.jekyllex.util.stringToBase64
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.android.synthetic.main.other_no_internet.*

class MarkdownEditor : AppCompatActivity() {

    lateinit var viewModel: EditorViewModel
    private lateinit var fileSha: String
    private lateinit var currentRepo: String
    private lateinit var path: String
    private var isNew = false
    private var accessToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (preActivityStartChecks(this)) {
            0 -> {}
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
            2-> {
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

        val repository = GithubContentRepository()
        val factory = EditorViewModelFactory(repository)
        viewModel =
            ViewModelProvider(this, factory).get(EditorViewModel::class.java)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        currentRepo = prefs.getString("current_repo", "") ?: ""
        accessToken = prefs.getString("access_token", "") ?: ""

        setTheme(R.style.Theme_JekyllEx)
        setContentView(R.layout.activity_editor)

        setSupportActionBar(toolbar_editor)
        supportActionBar?.setHomeButtonEnabled(true)
        toolbar_editor.setNavigationIcon(R.drawable.ic_back)
        toolbar_editor.setNavigationOnClickListener {
            onBackPressed()
        }

        val extras = intent.extras

        if (extras != null && accessToken.isNotEmpty()) {
            path = extras.getString("path", "")
            fileSha = extras.getString("sha", "")
            isNew = extras.getBoolean("isNew")
            if (!isNew) viewModel.getContent(currentRepo, path, "Bearer $accessToken")
        }

        viewModel.originalContent.observe(this, {
            if (!it.isNullOrEmpty()) {
                showEditorArea()
                viewModel.setNewText(it)
            } else {
                hideEditorArea()
            }
        })

        viewModel.postMetaData.observe(this, {
            invalidateOptionsMenu()
        })

        viewModel.isTextUpdated.observe(this, {
            invalidateOptionsMenu()
        })

        editorViewPager.apply {
            adapter = PageAdapter(this@MarkdownEditor)
            currentItem = 0
        }

        TabLayoutMediator(editorTabLayout, editorViewPager) { currentTab, currentPosition ->
            currentTab.text = when (currentPosition) {
                0 -> getString(R.string.edit_file)
                1 -> getString(R.string.preview_changes)
                else -> ""
            }
        }.attach()

        if (isNew) showEditorArea()
    }

    fun setCurrentPage(id: Int) {
        this.currentFocus?.let { view ->
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }

        editorViewPager.currentItem = id
    }

    private fun showEditorArea() {
        editorProgressBarParent.visibility = View.GONE
        editorTabLayout.visibility = View.VISIBLE
        editorViewPager.visibility = View.VISIBLE
    }

    private fun hideEditorArea() {
        editorProgressBarParent.visibility = View.GONE
        editorTabLayout.visibility = View.GONE
        editorViewPager.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_editor, menu)

        val uploadPostMenuItem = menu.findItem(R.id.uploadPostMenuItem)
        uploadPostMenuItem.isVisible = viewModel.isTextUpdated.value ?: false

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.editMetaData -> {
                val dialogBuilder = AlertDialog.Builder(this)
                val inflater = this.layoutInflater
                val dialogView: View = inflater.inflate(R.layout.other_metadata_alert, null)
                dialogBuilder.setView(dialogView)

                val alertDialog: AlertDialog = dialogBuilder.create()
                alertDialog.setCancelable(false)
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alertDialog.show()

                val editText = dialogView.findViewById<View>(R.id.metaDataEt) as EditText

                if (!viewModel.postMetaData.value.isNullOrEmpty()) {
                    editText.text = SpannableStringBuilder(viewModel.postMetaData.value)
                }

                val saveMetaDataBtn = dialogView.findViewById<View>(R.id.saveMetaData) as Button
                val closeMetaDataBtn =
                    dialogView.findViewById<View>(R.id.closeMetaDataDialog) as Button
                saveMetaDataBtn.setOnClickListener {
                    viewModel.saveMetaData(editText.text.toString())
                    alertDialog.dismiss()
                }
                closeMetaDataBtn.setOnClickListener {
                    alertDialog.dismiss()
                }
            }
            R.id.uploadPostMenuItem -> {
                val dialogBuilder = AlertDialog.Builder(this)
                val inflater = this.layoutInflater
                val dialogView: View = inflater.inflate(R.layout.other_commitmessage_alert, null)
                dialogBuilder.setView(dialogView)

                var alertDialog: AlertDialog = dialogBuilder.create()
                alertDialog.setCancelable(false)
                alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                alertDialog.show()

                val uploadPostBtn = dialogView.findViewById<Button>(R.id.uploadPostBtn)
                val closeCommitMsgBtn =
                    dialogView.findViewById<Button>(R.id.closeCommitMessageDialog)
                val metaDataEt = dialogView.findViewById<TextInputEditText>(R.id.commitMessageEt)
                val commitMsgParent =
                    dialogView.findViewById<LinearLayout>(R.id.commitMessageParent)
                val progressGroup =
                    dialogView.findViewById<LinearLayout>(R.id.postUploadingProgressParent)

                uploadPostBtn.setOnClickListener {
                    val commitMessage = metaDataEt.text.toString()
                    val postContent = viewModel.text.value ?: ""
                    val postMetaData = viewModel.postMetaData.value ?: ""
                    val encodedContent = stringToBase64(postMetaData, postContent)

                    if (
                        commitMessage.isEmpty() ||
                        postContent.isEmpty() ||
                        postMetaData.isEmpty() ||
                        encodedContent.isEmpty() ||
                        currentRepo.isEmpty() ||
                        path.isEmpty()
                    ) {
                        Toast
                            .makeText(this, "Some fields are missing!", Toast.LENGTH_SHORT)
                            .show()
                        alertDialog.dismiss()
                    } else {
                        commitMsgParent.visibility = View.GONE
                        progressGroup.visibility = View.VISIBLE

                        val commit =
                            if (isNew) CommitModel(commitMessage, encodedContent, null)
                            else CommitModel(commitMessage, encodedContent, fileSha)

                        viewModel.uploadPost(commit, currentRepo, path, "Bearer $accessToken")

                        viewModel.isUploaded.observe(this, {
                            if (it) {
                                viewModel.changeIsTextUpdated(false)
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
                                        "Some unexpected error occured!",
                                        Toast.LENGTH_SHORT
                                    )
                                    .show()
                                alertDialog.dismiss()
                                onBackPressed()
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

    override fun onBackPressed() {
        if (editorViewPager.currentItem == 1) {
            editorViewPager.currentItem = 0
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
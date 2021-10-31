package com.github.gouravkhunger.jekyllex.ui.content

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.gouravkhunger.jekyllex.R
import com.github.gouravkhunger.jekyllex.databinding.ActivityContentBinding
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin

class ContentActivity : AppCompatActivity() {

    // View binding variables.
    private lateinit var contentBinding: ActivityContentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise view binding.
        contentBinding = ActivityContentBinding.inflate(layoutInflater)

        // Initialise markwon library.
        val markwon = Markwon.builder(this)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(GlideImagesPlugin.create(this))
            .build()

        val extras = intent.extras

        // set the theme and tool bar.
        setTheme(R.style.Theme_JekyllEx)
        setContentView(contentBinding.root)
        setSupportActionBar(contentBinding.toolbarContent)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        contentBinding.toolbarContent.setNavigationIcon(R.drawable.ic_back)
        contentBinding.toolbarContent.setNavigationOnClickListener {
            onBackPressed()
        }
        contentBinding.toolbarContent.applyFont()

        // if there is some data passed to this activity,
        // try extracting the id of the content to be shown.
        // else there has been some error.
        if (extras != null) {
            val id = extras.getInt("id")
            val title = extras.getString("title")
            contentBinding.contentTitle.text = title
            markwon.setMarkdown(contentBinding.contentTv, getString(id))
        } else {
            Toast.makeText(this, "Unexpected error occurred", Toast.LENGTH_SHORT).show()
            onBackPressed()
        }
    }

    // override the back button press so as to show custom activity closing animation.
    override fun onBackPressed() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}

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

package xyz.jekyllex.ui.activities.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.google.firebase.analytics.FirebaseAnalytics
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.footerPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.preferenceTheme
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.switchPreference
import me.zhanghai.compose.preference.textFieldPreference
import xyz.jekyllex.R
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.utils.Commands.git
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.Setting.*
import xyz.jekyllex.utils.trimQuotes
import xyz.jekyllex.BuildConfig
import xyz.jekyllex.ui.activities.viewer.WebPageViewer
import xyz.jekyllex.utils.Constants.DOCS
import xyz.jekyllex.utils.Constants.TERMS
import xyz.jekyllex.utils.Constants.PRIVACY
import xyz.jekyllex.utils.Constants.LICENSES
import xyz.jekyllex.utils.Constants.ISSUES_URL
import xyz.jekyllex.utils.Constants.PAT_SETTINGS_URL

private lateinit var firebaseAnalytics: FirebaseAnalytics

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        setContent {
            JekyllExTheme {
                SettingsView()
            }
        }
    }
}

@Composable
fun SettingsView() {
    val context = LocalContext.current as Activity
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            JekyllExAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { context.finish() }) {
                        Icon(
                            contentDescription = "Go back",
                            painter = painterResource(id = R.drawable.back),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(20.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        ProvidePreferenceLocals(
            theme = preferenceTheme(
                summaryTextStyle = MaterialTheme.typography.labelSmall,
                categoryTextStyle = MaterialTheme.typography.labelMedium,
                categoryPadding = PaddingValues(
                    start = 16.dp, top = 24.dp, end = 16.dp, bottom = 4.dp
                )
            )
        ) {
            LazyColumn(
                modifier = Modifier.padding(padding)
            ) {
                preferenceCategory(
                    key = "general_settings",
                    title = { Text("General") }
                )

                preference(
                    key = "install_bootstrap",
                    onClick = { NativeUtils.launchInstaller(context, true) },
                    title = { Text(context.getString(R.string.bootstrap_setting_title)) },
                    summary = {
                        Text(context.getString(R.string.bootstrap_setting_summary))
                    }
                )

                switchPreference(
                    key = REDUCE_ANIMATIONS.key,
                    defaultValue = REDUCE_ANIMATIONS.defaultValue.get(),
                    title = { Text(context.getString(R.string.reduce_animations_title)) },
                    summary = { Text(context.getString(R.string.reduce_animations_summary)) },
                )

                switchPreference(
                    key = TRIM_LOGS.key,
                    defaultValue = TRIM_LOGS.defaultValue.get(),
                    title = { Text(context.getString(R.string.trim_logs_title)) },
                    summary = { Text(context.getString(R.string.trim_logs_summary)) },
                )

                switchPreference(
                    key = GUESS_URLS.key,
                    defaultValue = GUESS_URLS.defaultValue.get(),
                    title = { Text(context.getString(R.string.guess_urls_title)) },
                    summary = { Text(context.getString(R.string.guess_urls_summary)) },
                )

                sliderPreference(
                    valueSteps = 10,
                    defaultValue = DEBOUNCE_DELAY.defaultValue.get(),
                    valueRange = .25f..3f,
                    key = DEBOUNCE_DELAY.key,
                    valueText = { Text(text = "%.2fs".format(it)) },
                    title = { Text(context.getString(R.string.debounce_setting_title)) },
                    summary = { Text(context.getString(R.string.debounce_setting_summary)) },
                )

                textFieldPreference(
                    key = PREVIEW_PORT.key,
                    textToValue = {
                        val port = it.toIntOrNull()

                        if (port == null || port !in 1024..65535) {
                            Toast.makeText(
                                context,
                                "Invalid port number, must be between 1024 and 65535",
                                Toast.LENGTH_SHORT
                            ).show()

                            PREVIEW_PORT.defaultValue.get()
                        } else port
                    },
                    valueToText = { it.toString() },
                    defaultValue = PREVIEW_PORT.defaultValue.get(),
                    title = { Text(context.getString(R.string.preview_port_title)) },
                    summary = {
                        Text(context.getString(R.string.preview_port_summary))
                        Text(it.toString())
                    },
                )

                preferenceCategory(
                    key = "git_settings",
                    title = { Text("Git") }
                )

                textFieldPreference(
                    key = GIT_NAME.key,
                    defaultValue = GIT_NAME.defaultValue.get(),
                    title = { Text(context.getString(R.string.git_name_title)) },
                    summary = {
                        Text(context.getString(R.string.git_name_summary))
                        Text(it.ifBlank { "Empty or not set" })
                    },
                    textToValue = {
                        NativeUtils.exec(
                            git("config", "--global", "user.name", it)
                        )
                        it
                    },
                    valueToText = {
                        NativeUtils.exec(
                            git("config", "--global", "user.name")
                        ).trimQuotes()
                    },
                )

                textFieldPreference(
                    key = GIT_EMAIL.key,
                    defaultValue = GIT_EMAIL.defaultValue.get(),
                    title = { Text(context.getString(R.string.git_email_title)) },
                    summary = {
                        Text(context.getString(R.string.git_email_summary))
                        Text(it.ifBlank { "Empty or not set" })
                    },
                    textToValue = {
                        NativeUtils.exec(
                            git("config", "--global", "user.email", it)
                        )
                        it
                    },
                    valueToText = {
                        NativeUtils.exec(
                            git("config", "--global", "user.email")
                        ).trimQuotes()
                    },
                )

                textFieldPreference(
                    key = GITHUB_PAT.key,
                    defaultValue = GITHUB_PAT.defaultValue.get(),
                    title = { Text(context.getString(R.string.github_pat_title)) },
                    summary = {
                        Text(
                            "Token to access private repositories and " +
                                    "perform authenticated operations like git push"
                        )
                        ClickableText(
                            text = buildAnnotatedString {
                                pushStringAnnotation(
                                    tag = "token_link",
                                    annotation = PAT_SETTINGS_URL
                                )
                                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append("Generate a new token >")
                                }
                                pop()
                            },
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(PAT_SETTINGS_URL))
                                )
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "\n" +
                                    (it.take(10) + it.drop(10).map { '*' }.joinToString(""))
                                        .ifBlank { "Empty or not set" }
                        )
                    },
                    textToValue = {
                        NativeUtils.exec(
                            git("config", "--global", "credential.username", it)
                        )
                        NativeUtils.exec(
                            git("config", "--global", "credential.password", it)
                        )
                        it
                    },
                )

                switchPreference(
                    key = LOG_PROGRESS.key,
                    defaultValue = LOG_PROGRESS.defaultValue.get(),
                    title = { Text(context.getString(R.string.log_progress_title)) },
                    summary = { Text(context.getString(R.string.log_progress_summary)) },
                )

                preferenceCategory(
                    key = "bundler_settings",
                    title = { Text("Bundler") }
                )

                switchPreference(
                    key = LOCAL_GEMS.key,
                    defaultValue = LOCAL_GEMS.defaultValue.get(),
                    title = { Text(context.getString(R.string.local_gems_title)) },
                    summary = { Text(context.getString(R.string.local_gems_summary)) },
                )

                preferenceCategory(
                    key = "jekyll_settings",
                    title = { Text("Jekyll") }
                )

                textFieldPreference(
                    key = JEKYLL_ENV.key,
                    textToValue = { it },
                    valueToText = { it },
                    defaultValue = JEKYLL_ENV.defaultValue.get(),
                    title = { Text(context.getString(R.string.jekyll_env_title)) },
                    summary = {
                        Text(context.getString(R.string.jekyll_env_summary))
                        Text(it)
                    },
                )

                switchPreference(
                    key = SKIP_BUNDLER.key,
                    defaultValue = SKIP_BUNDLER.defaultValue.get(),
                    title = { Text(context.getString(R.string.skip_bundler_title)) },
                    summary = { Text(context.getString(R.string.skip_bundler_summary)) },
                )

                switchPreference(
                    key = PREFIX_BUNDLER.key,
                    defaultValue = PREFIX_BUNDLER.defaultValue.get(),
                    title = { Text(context.getString(R.string.prefix_bundler_title)) },
                    summary = { Text(context.getString(R.string.prefix_bundler_summary)) },
                )

                switchPreference(
                    key = LIVERELOAD.key,
                    defaultValue = LIVERELOAD.defaultValue.get(),
                    title = { Text(context.getString(R.string.livereload_title)) },
                    summary = {
                        Text(context.getString(R.string.livereload_summary))
                    },
                )

                textFieldPreference(
                    key = JEKYLL_FLAGS.key,
                    textToValue = { it },
                    valueToText = { it },
                    defaultValue = JEKYLL_FLAGS.defaultValue.get(),
                    title = { Text(context.getString(R.string.jekyll_flags_title)) },
                    summary = {
                        Text(context.getString(R.string.jekyll_flags_summary))
                        it.takeIf { it.isNotBlank() }?.let { flag -> Text(flag) }
                    },
                )

                preferenceCategory(
                    key = "telemetry",
                    title = { Text("Telemetry") }
                )

                switchPreference(
                    key = CRASH_REPORTS.key,
                    defaultValue = CRASH_REPORTS.defaultValue.get(),
                    title = { Text(context.getString(R.string.crash_reports_title)) },
                    summary = { Text(context.getString(R.string.crash_reports_summary)) },
                )

                switchPreference(
                    key = LOG_ANALYTICS.key,
                    defaultValue = LOG_ANALYTICS.defaultValue.get(),
                    title = { Text(context.getString(R.string.analytics_title)) },
                    summary = { Text(context.getString(R.string.analytics_summary)) },
                )

                preferenceCategory(
                    key = "other",
                    title = { Text("Other") }
                )

                preference(
                    key = "docs",
                    onClick = {
                        context.startActivity(
                            Intent(context, WebPageViewer::class.java).apply {
                                putExtra("url", DOCS)
                                putExtra("title", context.getString(R.string.docs))
                            }
                        )
                    },
                    title = { Text(context.getString(R.string.docs)) },
                )

                preference(
                    key = "report",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(ISSUES_URL))
                        )
                    },
                    title = { Text(context.getString(R.string.report)) },
                )

                preference(
                    key = "licenses",
                    onClick = {
                        context.startActivity(
                            Intent(context, WebPageViewer::class.java).apply {
                                putExtra("url", LICENSES)
                                putExtra("title", context.getString(R.string.licenses))
                            }
                        )
                    },
                    title = { Text(context.getString(R.string.licenses)) },
                )

                preference(
                    key = "privacy",
                    onClick = {
                        context.startActivity(
                            Intent(context, WebPageViewer::class.java).apply {
                                putExtra("url", PRIVACY)
                                putExtra("title", context.getString(R.string.privacy_policy))
                            }
                        )
                    },
                    title = { Text(context.getString(R.string.privacy_policy)) },
                )

                preference(
                    key = "terms",
                    onClick = {
                        context.startActivity(
                            Intent(context, WebPageViewer::class.java).apply {
                                putExtra("url", TERMS)
                                putExtra("title", context.getString(R.string.terms_and_conditions))
                            }
                        )
                    },
                    title = { Text(context.getString(R.string.terms_and_conditions)) },
                )

                footerPreference(
                    key = "footer",
                    summary = {
                        Text("Bootstrap ${BuildConfig.BOOTSTRAP}")
                        Text("JekyllEx ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        Text("${BuildConfig.APPLICATION_ID} (${BuildConfig.BUILD_TYPE}@${BuildConfig.GIT_HASH})")
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Info",
                            modifier = Modifier.clickable {
                                clipboardManager.setText(
                                    AnnotatedString(
                                        "Bootstrap ${BuildConfig.BOOTSTRAP}\n" +
                                                "JekyllEx ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                                                "${BuildConfig.APPLICATION_ID} (${BuildConfig.BUILD_TYPE}@${BuildConfig.GIT_HASH})"
                                    )
                                )

                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        }
    }
}

/*
 * MIT License
 *
 * Copyright (c) 2024 Gourav Khunger
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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.checkboxPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.preferenceTheme
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.textFieldPreference
import xyz.jekyllex.R
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.utils.Commands.Companion.git
import xyz.jekyllex.utils.Constants.Companion.PAT_SETTINGS_URL
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.Setting.*
import xyz.jekyllex.utils.Settings
import xyz.jekyllex.utils.trimQuotes

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                                pushStringAnnotation(tag = "token_link", annotation = PAT_SETTINGS_URL)
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

                preferenceCategory(
                    key = "jekyll_settings",
                    title = { Text("Jekyll") }
                )

                checkboxPreference(
                    key = PREFIX_BUNDLER.key,
                    defaultValue = PREFIX_BUNDLER.defaultValue.get(),
                    title = { Text(context.getString(R.string.prefix_bundler_title)) },
                    summary = { Text(context.getString(R.string.prefix_bundler_summary)) },
                )

                checkboxPreference(
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
                    key = "editor_settings",
                    title = { Text("Editor") },
                    modifier = Modifier.padding(bottom = 0.dp)
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
            }
        }
    }
}
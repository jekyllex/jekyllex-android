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
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.checkboxPreference
import me.zhanghai.compose.preference.sliderPreference
import xyz.jekyllex.R
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.utils.Setting.DEBOUNCE_DELAY
import xyz.jekyllex.utils.Setting.PREFIX_BUNDLER

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
        ProvidePreferenceLocals {
            LazyColumn(
                modifier = Modifier.padding(padding)
            ) {
                checkboxPreference(
                    key = PREFIX_BUNDLER.key,
                    defaultValue = PREFIX_BUNDLER.defaultValue.get(),
                    title = { Text(context.getString(R.string.prefix_bundler_title)) },
                    summary = {
                        Text(
                            context.getString(R.string.prefix_bundler_summary),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                )

                sliderPreference(
                    valueSteps = 10,
                    defaultValue = DEBOUNCE_DELAY.defaultValue.get(),
                    valueRange = .25f..3f,
                    key = DEBOUNCE_DELAY.key,
                    valueText = { Text(text = "%.2f".format(it)) },
                    title = { Text(context.getString(R.string.debounce_setting_title)) },
                    summary = {
                        Text(
                            context.getString(R.string.debounce_setting_summary),
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                )
            }
        }
    }
}
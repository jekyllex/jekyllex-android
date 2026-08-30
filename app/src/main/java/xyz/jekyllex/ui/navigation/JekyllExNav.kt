/*
 * MIT License
 *
 * Copyright (c) 2026 Gourav Khunger
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

package xyz.jekyllex.ui.navigation

import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import xyz.jekyllex.data.ProcessRepository
import xyz.jekyllex.ui.activities.editor.EditorView
import xyz.jekyllex.ui.activities.home.HomeScreen
import xyz.jekyllex.ui.activities.home.HomeViewModel
import xyz.jekyllex.ui.activities.settings.SettingsView
import xyz.jekyllex.ui.activities.viewer.WebPageScreen

@Composable
fun JekyllExNav(
    homeViewModel: HomeViewModel,
    process: ProcessRepository,
    pickFileLauncher: ActivityResultLauncher<String>,
    requestPermissionLauncher: ActivityResultLauncher<String>,
) {
    val context = LocalContext.current
    val backStack = remember { mutableStateListOf<Any>(HomeDestination) }
    val processService by process.bound.collectAsStateWithLifecycle()
    val pop = { if (backStack.size > 1) backStack.removeLastOrNull() }

    LaunchedEffect(Unit) {
        WebView(context).destroy()
    }

    NavDisplay(
        backStack = backStack,
        onBack = { pop() },
        transitionSpec = {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
        },
        popTransitionSpec = {
            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
        },
        predictivePopTransitionSpec = {
            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
        },
        entryProvider = { key ->
            when (key) {
                is HomeDestination -> NavEntry(key) {
                    HomeScreen(
                        homeViewModel = homeViewModel,
                        pickFileLauncher = pickFileLauncher,
                        requestPermissionLauncher = requestPermissionLauncher,
                        onOpenFile = { path -> backStack.add(EditorDestination(path)) },
                        onOpenSettings = { backStack.add(SettingsDestination) },
                    )
                }

                is EditorDestination -> NavEntry(key) {
                    EditorView(
                        file = key.path,
                        processService = processService,
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                        onRenamed = { path ->
                            if (backStack.size > 1) backStack.removeLastOrNull()
                            backStack.add(EditorDestination(path))
                        },
                    )
                }

                is SettingsDestination -> NavEntry(key) {
                    SettingsView(
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                        onOpenPage = { url, title ->
                            backStack.add(PageDestination(url, title))
                        },
                    )
                }

                is PageDestination -> NavEntry(key) {
                    WebPageScreen(
                        initialUrl = key.url,
                        initialTitle = key.title,
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                    )
                }

                else -> NavEntry(Unit) { }
            }
        }
    )
}

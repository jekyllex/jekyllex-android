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

package xyz.jekyllex.ui.activities.editor

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jekyllex.R
import xyz.jekyllex.services.ProcessService
import xyz.jekyllex.ui.activities.editor.components.DropDownMenu
import xyz.jekyllex.ui.activities.editor.components.Editor
import xyz.jekyllex.ui.activities.editor.components.Preview
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.components.TerminalSheet
import xyz.jekyllex.utils.Commands.guessDestinationUrl
import xyz.jekyllex.utils.Commands.jekyll
import xyz.jekyllex.utils.Commands.mv
import xyz.jekyllex.utils.Commands.rm
import xyz.jekyllex.utils.Constants.ignoreGuessesIn
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.Setting
import xyz.jekyllex.utils.Settings
import xyz.jekyllex.utils.buildPreviewURL
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.getProjectDir
import xyz.jekyllex.utils.pathInProject

private val isBound = mutableStateOf(false)
private lateinit var service: ProcessService

class EditorActivity : ComponentActivity() {
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            service = (binder as ProcessService.LocalBinder).service
            isBound.value = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound.value = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Intent(this, ProcessService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        val file = intent.getStringExtra("file") ?: ""

        setContent {
            JekyllExTheme {
                EditorView(file)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}

@Composable
fun EditorView(file: String = "") {
    val context = LocalContext.current as Activity
    var showTerminalSheet by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf(file.formatDir("/")) }

    val settings = Settings(context)
    val updateDescription: (String?) -> Unit = { url ->
        url?.let { description = it.ifBlank { "/" } } ?: run {
            description = file.formatDir("/")
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            JekyllExAppBar(
                title = {
                    Column {
                        Text(
                            maxLines = 1,
                            fontSize = 20.sp,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(0.dp),
                            text = file.substringAfterLast("/"),
                        )
                        Text(
                            maxLines = 1,
                            fontSize = 14.sp,
                            text = description,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(0.dp).let {
                                if (!URLUtil.isValidUrl(description)) it
                                else it.clickable {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(description))
                                    )
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
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
                },
                actions = {
                    if (isBound.value) {
                        DropDownMenu(
                            serverItemText = if (service.isRunning) "Stop server" else "Start server",
                            runServer = {
                                if (!service.isRunning)
                                    file.getProjectDir()?.let { dir ->
                                        service.exec(jekyll("serve"), dir)
                                        showTerminalSheet = true
                                    }
                                else
                                    service.killProcess()
                            },
                            openTerminal = {
                                showTerminalSheet = true
                            },
                            renameFile = { newName ->
                                val destination = file.substringBeforeLast("/") + "/" + newName

                                NativeUtils.exec(
                                    mv(file, destination),
                                    CoroutineScope(Dispatchers.IO)
                                ) {
                                    withContext(Dispatchers.Main) {
                                        val intent = context.intent
                                        intent.putExtra("file", destination)
                                        context.finish()
                                        context.startActivity(intent)
                                    }
                                }
                            },
                            deleteFile = {
                                NativeUtils.exec(rm(file), CoroutineScope(Dispatchers.IO)) {
                                    withContext(Dispatchers.Main) {
                                        context.finish()
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val tabs = listOf("Editor", "Preview")
        var guessedUrl by remember { mutableStateOf("") }
        var tabIndex by remember { mutableIntStateOf(0) }
        val viewCache = remember { mutableStateMapOf<Int, WebView>() }
        val isEditorLoading = remember { mutableStateOf(true) }
        val canPreview by remember { derivedStateOf { isBound.value && service.isRunning } }

        val theme = settings.get<Int>(Setting.EDITOR_THEME)
        val previewPort = settings.get<Int>(Setting.PREVIEW_PORT)
        val shouldGuessURLs = settings.get<Boolean>(Setting.GUESS_URLS)
        val timeout = settings.get<Float>(Setting.DEBOUNCE_DELAY).times(1000).toInt()

        LaunchedEffect(Unit) effect@{
            CoroutineScope(Dispatchers.IO).launch run@{
                if (!shouldGuessURLs || ignoreGuessesIn.any { file.contains(it) }) return@run

                val dir = file.getProjectDir() ?: return@run
                val path = file.pathInProject()

                val url = NativeUtils.exec(guessDestinationUrl(path), dir)

                if (url.isEmpty()) return@run
                val stripExt = path.split('.')
                    .dropLast(1).joinToString()

                if (
                    !url.contains("404") &&
                    (url == "/${stripExt}" || url == "/$path")
                ) return@run

                guessedUrl = url
                if (tabIndex == 1) updateDescription(url)

                withContext(Dispatchers.Main) {
                    viewCache[1]?.loadUrl(url.buildPreviewURL())
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = {
                            tabIndex = index
                            updateDescription(
                                if (index == 0) file.formatDir("/")
                                else viewCache[1]?.url ?: guessedUrl
                            )
                        }
                    )
                }
            }

            when (tabIndex) {
                0 -> Editor(viewCache, file, theme, timeout, innerPadding, isEditorLoading)
                1 -> Preview(viewCache, file, previewPort, guessedUrl, canPreview, innerPadding, updateDescription) {
                    file.getProjectDir()?.let { dir ->
                        service.exec(jekyll("serve"), dir)
                        showTerminalSheet = true
                    }
                }
            }
        }

        if (showTerminalSheet) {
            TerminalSheet(
                isServiceBound = isBound.value,
                sessionManager = service.sessionManager,
                onDismiss = { showTerminalSheet = false },
            )
        }
    }
}

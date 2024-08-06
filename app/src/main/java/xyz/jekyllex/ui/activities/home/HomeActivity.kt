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

package xyz.jekyllex.ui.activities.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import xyz.jekyllex.R
import xyz.jekyllex.services.ProcessService
import xyz.jekyllex.ui.activities.editor.EditorActivity
import xyz.jekyllex.ui.activities.home.components.DropDownMenu
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.components.FileButton
import xyz.jekyllex.ui.components.TerminalSheet
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.utils.Commands.Companion.echo
import xyz.jekyllex.utils.Commands.Companion.git
import xyz.jekyllex.utils.Commands.Companion.jekyll
import xyz.jekyllex.utils.Commands.Companion.rmDir
import xyz.jekyllex.utils.Constants.Companion.HOME_DIR
import xyz.jekyllex.utils.Constants.Companion.requiredBinaries
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.buildServeCommand
import xyz.jekyllex.utils.getProjectDir
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.removeSymlinks
import java.io.File

private var isBound: Boolean = false
private lateinit var service: ProcessService
private var isCreating = mutableStateOf(false)

class HomeActivity : ComponentActivity() {
    private lateinit var viewModel: HomeViewModel
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            isBound = true
            service = (binder as ProcessService.LocalBinder).service
            viewModel.appendLog = { service.appendLog(it) }
            service.exec(echo("Welcome to JekyllEx!"))
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!NativeUtils.areUsable(requiredBinaries)) NativeUtils.launchInstaller(this)

        Intent(this, ProcessService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        viewModel = viewModels<HomeViewModel>().value

        setContent {
            JekyllExTheme {
                HomeScreen(viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}

private fun create(input: String, callBack: () -> Unit = {}) {
    if (!isBound || isCreating.value) return
    isCreating.value = true

    val command: Array<String> =
        if (input.startsWith("git clone "))
            input.split(" ").toTypedArray()
        else{
            val url = input.let {
                if (it.contains("github.com") && !it.contains("://")) "https://$it"
                else it
            }
            when (URLUtil.isValidUrl(url)) {
                true -> git("clone", url, "--progress")
                false -> jekyll("new", input)
            }
        }

    service.exec(command) {
        if (command.contentEquals(jekyll("new", input))) {
            File(HOME_DIR, input).removeSymlinks()
        }

        isCreating.value = false
        callBack()
    }
}

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var showTerminalSheet by remember { mutableStateOf(false) }

    BackHandler(
        enabled = homeViewModel.cwd.value.contains("$HOME_DIR/")
    ) { homeViewModel.cd("..") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            JekyllExAppBar(
                title = {
                    Text(
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = homeViewModel.cwd.value.substringAfterLast("/"),
                    )
                },
                actions = {
                    DropDownMenu(
                        homeViewModel,
                        isCreating,
                        onCreateConfirmation = { input, isDialogOpen ->
                            if (input.isNotBlank()) create(input) {
                                isDialogOpen.value = false
                                homeViewModel.refresh()
                            }
                        },
                        onDeleteConfirmation = { isDialogOpen ->
                            if (service.isRunning) service.killProcess()
                            val folder = homeViewModel.cwd.value
                            NativeUtils.exec(rmDir(folder))
                            homeViewModel.cd("..")
                            service.appendLog(
                                homeViewModel.cwd.value.formatDir("/") +
                                        "$ rm -rf $folder"
                            )
                            isDialogOpen.value = false
                        },
                        serverIcon = {
                            IconButton(onClick = {
                                if (!isBound) return@IconButton
                                if (!service.isRunning)
                                    service.exec(
                                        buildServeCommand(context),
                                        homeViewModel.cwd.value.let { it.getProjectDir() ?: it }
                                    )
                                else
                                    service.killProcess()
                            }) {
                                if (!service.isRunning)
                                    Icon(Icons.Default.PlayArrow, "Start server")
                                else
                                    Icon(painterResource(R.drawable.stop), "Stop server")
                            }
                        }
                    ) { cmd, dir -> if (isBound) service.exec(cmd, dir) }
                },
                navigationIcon = {
                    if (homeViewModel.cwd.value.contains("$HOME_DIR/"))
                        IconButton(onClick = { homeViewModel.cd("..") }) {
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
        },
        floatingActionButton = {
            FloatingActionButton(
                shape = CircleShape,
                onClick = { showTerminalSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp
                )
            ) {
                Icon(painterResource(R.drawable.terminal), "Open terminal")
            }
        }
    ) { padding ->
        val files = homeViewModel.availableFiles.collectAsState().value

        Column(
            modifier = Modifier.padding(top = padding.calculateTopPadding())
        ) {
            Text(
                text = homeViewModel.cwd.value.formatDir(" / "),
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
            ) {
                if (homeViewModel.cwd.value == HOME_DIR && files.isEmpty())
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No projects found!",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Let's start by creating a 'test' project:",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Button(
                            onClick = { create("test") { homeViewModel.refresh() } },
                            modifier = Modifier.padding(top = 16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp
                            )
                        ) {
                            if (isCreating.value)
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            else
                                Text(text = "Create")
                        }
                    }
                else LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(files.size) {
                        FileButton(
                            file = files[it],
                            modifier = Modifier.padding(8.dp),
                            onClick = {
                                if (files[it].isDir == true)
                                    homeViewModel.cd(files[it].name)
                                else {
                                    val file = "${homeViewModel.cwd.value}/${files[it].name}"
                                    context.startActivity(
                                        Intent(context, EditorActivity::class.java)
                                            .putExtra("file", file)
                                    )
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        if (showTerminalSheet) {
            TerminalSheet(
                isServiceBound = isBound,
                isRunning = service.isRunning,
                onDismiss = { showTerminalSheet = false },
                clearLogs = { service.clearLogs() },
                logs = service.logs.collectAsState().value,
                exec = { command: Array<String> ->
                    service.exec(command, homeViewModel.cwd.value)
                },
            )
        }
    }
}

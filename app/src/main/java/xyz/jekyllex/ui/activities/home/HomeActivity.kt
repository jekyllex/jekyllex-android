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
import android.util.Log
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import xyz.jekyllex.R
import xyz.jekyllex.services.ProcessService
import xyz.jekyllex.ui.activities.editor.EditorActivity
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.components.FileButton
import xyz.jekyllex.ui.components.TerminalSheet
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.utils.Commands.Companion.bundle
import xyz.jekyllex.utils.Commands.Companion.echo
import xyz.jekyllex.utils.Commands.Companion.git
import xyz.jekyllex.utils.Commands.Companion.jekyll
import xyz.jekyllex.utils.Commands.Companion.rmDir
import xyz.jekyllex.utils.Constants.Companion.HOME_DIR
import xyz.jekyllex.utils.Constants.Companion.requiredBinaries
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.formatDir

private var isBound: Boolean = false
private lateinit var service: ProcessService
private var isCreating = mutableStateOf(false)

class HomeActivity : ComponentActivity() {
    companion object {
        private const val LOG_TAG = "HomeActivity"
    }

    private lateinit var viewModel: HomeViewModel
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            isBound = true
            service = (binder as ProcessService.LocalBinder).service
            lifecycleScope.launch {
                service.events.collect {
                    if (::viewModel.isInitialized && it.isNotEmpty())
                        viewModel.appendLog(it)
                }
            }
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
    if (isCreating.value) return

    isCreating.value = true
    Log.d("JekyllEx", isBound.toString())
    if (!isBound) return

    val command: Array<String> =
        if (input.startsWith("git "))
            input.split(" ").toTypedArray()
        else when (URLUtil.isValidUrl(input)) {
            true -> git("clone", input, "--progress")
            false -> jekyll("new", input)
        }

    service.exec(command, callBack = { isCreating.value = false; callBack() })
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
                    DropDownMenu(homeViewModel)
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
                clearLogs = { homeViewModel.clearLogs() },
                logs = homeViewModel.logs.collectAsState().value,
                exec = { command: Array<String> ->
                    service.exec(command, homeViewModel.cwd.value)
                },
            )
        }
    }
}

@Composable
fun DropDownMenu(homeViewModel: HomeViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val openDeleteDialog = remember { mutableStateOf(false) }
    val openCreateDialog = remember { mutableStateOf(false) }

    if (openCreateDialog.value) {
        CreateDialog(
            isCreating = isCreating.value,
            onDismissRequest = { openCreateDialog.value = false },
            onConfirmation = { input ->
                if (input.isNotBlank()) create(input) {
                    openCreateDialog.value = false
                    homeViewModel.refresh()
                }
            }
        )
    }

    if (openDeleteDialog.value) DeleteDialog(
        onDismissRequest = { openDeleteDialog.value = false },
        onConfirmation = {
            if (service.isRunning) service.killProcess()
            val folder = homeViewModel.cwd.value
            NativeUtils.exec(rmDir(folder))
            homeViewModel.cd("..")
            homeViewModel.appendLog(
                "${homeViewModel.cwd.value.formatDir("/")}$ rm -rf ${folder}"
            )
            openDeleteDialog.value = false
        },
        dialogTitle = "Delete",
        dialogText = "Are you sure you want to delete the current directory?",
        icon = Icons.Default.Delete
    )

    if (homeViewModel.cwd.value != HOME_DIR)
        IconButton(onClick = { homeViewModel.cd(HOME_DIR) }) {
            Icon(Icons.Default.Home, "Go back to home")

        }
    if (homeViewModel.cwd.value == HOME_DIR)
        IconButton(
            enabled = !isCreating.value,
            onClick = { openCreateDialog.value = true }
        ) {
            Icon(Icons.Default.AddCircle, "Create new project")
        }
    else if (homeViewModel.cwd.value.contains(HOME_DIR)) {
        IconButton(onClick = {
            if (!isBound) return@IconButton
            if (!service.isRunning)
                service.exec(
                    jekyll("serve"),
                    homeViewModel.project?.let {
                        "$HOME_DIR/$it"
                    } ?: homeViewModel.cwd.value
                )
            else
                service.killProcess()
        }) {
            if (!service.isRunning)
                Icon(Icons.Default.PlayArrow, "Start server")
            else
                Icon(painterResource(R.drawable.stop), "Stop server")
        }

        Box {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Bundler") },
                    onClick = {
                        expanded = !expanded
                        if (!isBound) return@DropdownMenuItem
                        service.exec(bundle("install"), homeViewModel.cwd.value)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete dir") },
                    onClick = {
                        expanded = !expanded
                        openDeleteDialog.value = true
                    },
                )
            }
        }
    }
}

@Composable
fun CreateDialog(
    isCreating: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmation: (input: String) -> Unit,
) {
    BasicAlertDialog(onDismissRequest = { }) {
        var text by remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current

        val onDone: () -> Unit = {
            keyboardController?.hide()
            onConfirmation(text.trim())
            text = ""
        }

        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(text = "Create project", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Enter the name of the project, " +
                            "a remote repository's valid https:// URL or a " +
                            "custom git clone command",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (!isCreating)
                    OutlinedTextField(
                        value = text,
                        singleLine = true,
                        onValueChange = { text = it },
                        label = { Text(text = "Name / URL / git command") },
                        modifier = Modifier.padding(top = 16.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onDone() }),
                    )
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    TextButton(
                        enabled = !isCreating,
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(text = "Cancel")
                    }
                    Button(onClick = onDone) {
                        if (isCreating)
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        else
                            Text(text = "Create")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector,
) {
    AlertDialog(
        icon = {
            Icon(icon, contentDescription = "Example Icon")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}

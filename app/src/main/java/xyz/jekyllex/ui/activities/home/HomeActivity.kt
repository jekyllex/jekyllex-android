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
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.capitalize
import kotlinx.coroutines.launch
import xyz.jekyllex.R
import xyz.jekyllex.services.ProcessService
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.utils.Commands.Companion.bundle
import xyz.jekyllex.utils.Commands.Companion.git
import xyz.jekyllex.utils.Commands.Companion.jekyll
import xyz.jekyllex.utils.Commands.Companion.rmDir
import xyz.jekyllex.utils.Constants.Companion.HOME_DIR
import xyz.jekyllex.utils.NativeUtils

private var isBound: Boolean = false
private lateinit var service: ProcessService

class HomeActivity : ComponentActivity() {
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            service = (binder as ProcessService.LocalBinder).service
            isBound = true
            lifecycleScope.launch {
                service.events.collect {
                    if (it.isNotBlank()) Log.d("HomeActivity", it)
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: HomeViewModel by viewModels()

        if (!NativeUtils.isUsable("jekyll")) NativeUtils.launchInstaller(this)

        Intent(this, ProcessService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

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
    Log.d("JekyllEx", isBound.toString())
    if (!isBound) return

    val command: Array<String> =
        if (input.startsWith("git "))
            input.split(" ").toTypedArray()
        else when (URLUtil.isValidUrl(input)) {
            true -> git("clone", input, "--progress")
            false -> jekyll("new", input)
        }

    service.exec(command, callBack = callBack)
}

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel()
) {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        JekyllExAppBar(title = {
            Text(
                text = homeViewModel.cwd.value.substringAfterLast("/")
            )
        }, actions = {
            DropDownMenu(homeViewModel)
        })
    }) { padding ->
        val folders = homeViewModel.availableFolders.value

        Column(
            modifier = Modifier.padding(top = padding.calculateTopPadding())
        ) {
            Text(
                text = homeViewModel.cwd.value,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                if (homeViewModel.cwd.value == HOME_DIR && folders.isEmpty())
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
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(text = "Create")
                        }
                    }
                else LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Button(onClick = {
                            homeViewModel.cd("..")
                        }) { Text(text = "..") }
                    }

                    items(folders.size) {
                        Button(onClick = {
                            homeViewModel.cd(folders[it])
                        }) { Text(text = folders[it]) }
                    }
                }
            }
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
            onDismissRequest = { openCreateDialog.value = false },
            onConfirmation = { input ->
                if (input.isNotBlank()) create(input) { homeViewModel.refresh() }
                openCreateDialog.value = false
            },
        )
    }

    if (openDeleteDialog.value) DeleteDialog(
        onDismissRequest = { openDeleteDialog.value = false },
        onConfirmation = {
            if (service.isRunning) service.killProcess()
            NativeUtils.exec(rmDir(homeViewModel.cwd.value))
            homeViewModel.cd("..")
            openDeleteDialog.value = false
        },
        dialogTitle = "Delete",
        dialogText = "Are you sure you want to delete the current directory?",
        icon = Icons.Default.Delete
    )

    if (homeViewModel.cwd.value != HOME_DIR)
        IconButton(onClick = { homeViewModel.cd(HOME_DIR) }) {
            Icon(Icons.Default.Home, "Create new project")

        }
    if (homeViewModel.cwd.value == HOME_DIR)
        IconButton(onClick = {
            openCreateDialog.value = true
        }) {
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

        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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

@Composable
fun CreateDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (input: String) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
    ) {
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
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(text = "Name / URL / git command") },
                    modifier = Modifier.padding(top = 16.dp),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = {
                        onConfirmation(text)
                    },
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .align(Alignment.End)
                ) {
                    Text(text = "Create")
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

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

package xyz.jekyllex.ui.activities.home

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import xyz.jekyllex.R
import xyz.jekyllex.services.ProcessService
import xyz.jekyllex.ui.activities.home.components.DropDownMenu
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.components.FileButton
import xyz.jekyllex.ui.components.TerminalSheet
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.utils.Commands.echo
import xyz.jekyllex.utils.Commands.git
import xyz.jekyllex.utils.Commands.jekyll
import xyz.jekyllex.utils.Commands.curl
import xyz.jekyllex.utils.Commands.mkDir
import xyz.jekyllex.utils.Commands.touch
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.Constants.requiredBinaries
import xyz.jekyllex.utils.open
import xyz.jekyllex.utils.Setting
import xyz.jekyllex.utils.Settings
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.toCommand
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.getProjectDir
import xyz.jekyllex.utils.removeSymlinks
import xyz.jekyllex.ui.components.GenericDialog
import java.io.File as JFile

private lateinit var service: ProcessService

class HomeActivity : ComponentActivity() {
    private lateinit var settings: Settings
    private lateinit var viewModel: HomeViewModel
    private lateinit var pickFileLauncher: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, binder: IBinder) {
            viewModel.isBound = true
            service = (binder as ProcessService.LocalBinder).service
            viewModel.appendLog = { service.sessionManager.sessions.value.first().appendLog(it) }
            service.exec(echo("Welcome to JekyllEx!"))
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            viewModel.isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!NativeUtils.areUsable(requiredBinaries)) NativeUtils.launchInstaller(this)

        startService(Intent(this, ProcessService::class.java))
        Intent(this, ProcessService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        settings = Settings(this)
        viewModel = viewModels<HomeViewModel>(
            factoryProducer = {
                HomeViewModel.Factory(settings.get(Setting.REDUCE_ANIMATIONS))
            }
        ).value

        pickFileLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                viewModel.fileUri = uri
                viewModel.copyFileConfirmation = true
            } ?: run {
                Toast.makeText(this, "No file selected!", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted: Boolean ->
            if (isGranted) {
                settings.set(Setting.ASK_NOTIF_PERM, false)
                val message = "Notifications set up successfully!"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } else {
                if (!settings.get<Boolean>(Setting.ASK_NOTIF_PERM)) {
                    Toast.makeText(
                        this,
                        "You will have to enable notifications from the settings!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && settings.get(Setting.ASK_NOTIF_PERM)) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                viewModel.notificationRationale = true
                settings.set(Setting.ASK_NOTIF_PERM, false)
            } else if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            JekyllExTheme {
                HomeScreen(viewModel, pickFileLauncher, requestPermissionLauncher)
            }
        }
    }

    override fun onRestart() {
        super.onRestart()

        if (::viewModel.isInitialized && ::settings.isInitialized)
            viewModel.setSkipAnimation(
                settings.get(Setting.REDUCE_ANIMATIONS)
            )

        if (::viewModel.isInitialized) viewModel.refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }
}

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    pickFileLauncher: ActivityResultLauncher<String>,
    requestPermissionLauncher: ActivityResultLauncher<String>
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var showTerminalSheet by remember { mutableStateOf(false) }

    val create: (input: String, callback: () -> Unit) -> Unit = create@{ input, callback ->
        if (!homeViewModel.isBound || homeViewModel.isCreating) return@create
        homeViewModel.isCreating = true

        val command: Array<String> =
            if (input.startsWith("git clone ")) input.toCommand()
            else {
                val url = input.let {
                    if (it.contains("github.com") && !it.contains("://")) "https://$it"
                    else it
                }
                when (URLUtil.isValidUrl(url)) {
                    true -> git("clone", url)
                    false -> jekyll("new", input)
                }
            }

        service.exec(command) {
            if (command.contentEquals(jekyll("new", input))) {
                JFile(HOME_DIR, input).removeSymlinks()
            }

            homeViewModel.isCreating = false
            callback()
        }
    }

    val resetQuery = {
        query = ""
        focusManager.clearFocus()
        homeViewModel.search(query)
    }

    val onBackPressed = {
        resetQuery()
        homeViewModel.cd("..")
        if (homeViewModel.isBound) service.cd("..")
    }

    BackHandler(
        enabled = homeViewModel.cwd.value.contains("$HOME_DIR/")
    ) { onBackPressed() }

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
                        pickFileLauncher,
                        goHome = {
                            resetQuery()
                            homeViewModel.cd(HOME_DIR)
                            if (homeViewModel.isBound) service.cd(HOME_DIR)
                        },
                        onCreateProjectConfirmation = { input, isDialogOpen ->
                            if (input.isNotBlank()) create(input) {
                                isDialogOpen.value = false
                                homeViewModel.refresh()
                            }
                        },
                        onCreateFileConfirmation = onCreateFileConfirmation@{ input, isFolder, isDialogOpen ->
                            if (homeViewModel.isCreating) return@onCreateFileConfirmation
                            homeViewModel.isCreating = true
                            val cwd = homeViewModel.cwd.value
                            val isValidURL = URLUtil.isValidUrl(input)
                            val command =
                                if (isFolder) mkDir(input)
                                else if (isValidURL) curl("-s", "-O", input)
                                else touch(input)
                            service.exec(command, cwd) {
                                homeViewModel.refresh()
                                homeViewModel.isCreating = false
                                isDialogOpen.value = false
                            }
                        },
                        serverIcon = {
                            IconButton(onClick = {
                                if (!homeViewModel.isBound) return@IconButton
                                if (!service.isRunning)
                                    service.exec(
                                        jekyll("serve"),
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
                    ) exec@{ cmd ->
                        if (!homeViewModel.isBound) return@exec

                        service.exec(cmd)
                        showTerminalSheet = true
                    }
                },
                navigationIcon = {
                    if (homeViewModel.cwd.value.contains("$HOME_DIR/"))
                        IconButton(onClick = { onBackPressed() }) {
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
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
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
                if (homeViewModel.cwd.value == HOME_DIR && homeViewModel.filesCount == 0)
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
                            if (homeViewModel.isCreating)
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
                    if (homeViewModel.filesCount > 1)
                        item {
                            OutlinedTextField(
                                value = query,
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                onValueChange = { query = it; homeViewModel.search(query) },
                                label = {
                                    Text("Search among ${homeViewModel.filesCount} items")
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        homeViewModel.search(query)
                                        focusManager.clearFocus()
                                    }
                                ),
                                trailingIcon = {
                                    if (query.isNotBlank())
                                        IconButton(onClick = { resetQuery() }) {
                                            Icon(Icons.Outlined.Clear, "Reset query")
                                        }
                                }
                            )
                        }

                    if (files.isEmpty()) {
                        item {
                            Text(
                                text = "No item(s) found!",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            )
                        }
                    }

                    items(files.size) {
                        FileButton(
                            file = files[it],
                            modifier = Modifier.padding(8.dp),
                            refresh = { homeViewModel.refresh() },
                            onClick = {
                                if (files[it].isDir) {
                                    resetQuery()
                                    homeViewModel.cd(files[it].name)
                                    if (homeViewModel.isBound) service.cd(files[it].name)
                                } else files[it].open(context)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        if (homeViewModel.copyFileConfirmation) GenericDialog(
            isCancellable = false,
            dialogTitle = "Copy",
            dialogText = "Are you sure you want to copy this file here?",
            onDismissRequest = {
                homeViewModel.fileUri = null
                homeViewModel.copyFileConfirmation = false
            },
            onConfirmation = confirmation@{
                val document = DocumentFile.fromSingleUri(context, homeViewModel.fileUri!!)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val destination = JFile(homeViewModel.cwd.value, document?.name!!)
                        val output = destination.outputStream()

                        val input = context.contentResolver.openInputStream(document.uri)!!
                        input.copyTo(output)

                        input.close()
                        output.close()

                        homeViewModel.refresh()
                    } catch (e: Exception) {
                        Toast.makeText(
                            context, "Error copying file!", Toast.LENGTH_SHORT
                        ).show()
                        e.printStackTrace()
                    } finally {
                        homeViewModel.fileUri = null
                        homeViewModel.copyFileConfirmation = false
                    }
                }
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && homeViewModel.notificationRationale) {
            GenericDialog(
                isCancellable = false,
                dialogTitle = "Permission request",
                dialogText = "This app requires the notification permission to let you know " +
                        "about running background tasks and control them from the notification.",
                confirmText = "OK",
                dismissText = "No thanks",
                onDismissRequest = {
                    homeViewModel.notificationRationale = false

                    Toast.makeText(
                        context,
                        "You will not be asked again!",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onConfirmation = {
                    homeViewModel.notificationRationale = false
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }

        if (showTerminalSheet) {
            TerminalSheet(
                isServiceBound = homeViewModel.isBound,
                sessionManager = service.sessionManager,
                onDismiss = { showTerminalSheet = false }
            )
        }
    }
}

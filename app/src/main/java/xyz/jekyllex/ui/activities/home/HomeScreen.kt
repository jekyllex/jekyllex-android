package xyz.jekyllex.ui.activities.home

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.jekyllex.R
import xyz.jekyllex.ui.activities.home.components.DropDownMenu
import xyz.jekyllex.ui.components.FileButton
import xyz.jekyllex.ui.components.GenericDialog
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.components.TerminalSheet
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.openInExternalApp
import xyz.jekyllex.utils.usesBuiltInEditor

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    pickFileLauncher: ActivityResultLauncher<String>,
    requestPermissionLauncher: ActivityResultLauncher<String>,
    onOpenFile: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val state by homeViewModel.uiState.collectAsStateWithLifecycle()
    var showTerminalSheet by remember { mutableStateOf(false) }

    val resetQuery = {
        focusManager.clearFocus()
        homeViewModel.search("")
    }

    val onBackPressed = {
        resetQuery()
        homeViewModel.goUp()
    }

    BackHandler(
        enabled = state.cwd.contains("$HOME_DIR/")
    ) { onBackPressed() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            JekyllExAppBar(
                title = {
                    Text(
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = state.cwd.substringAfterLast("/"),
                    )
                },
                actions = {
                    DropDownMenu(
                        cwd = state.cwd,
                        isCreating = state.isCreating,
                        picker = pickFileLauncher,
                        onRefresh = homeViewModel::refresh,
                        goHome = {
                            resetQuery()
                            homeViewModel.goHome()
                        },
                        onOpenSettings = onOpenSettings,
                        onCreateProjectConfirmation = { input, isDialogOpen ->
                            if (input.isNotBlank()) homeViewModel.create(input) {
                                isDialogOpen.value = false
                            }
                        },
                        onCreateFileConfirmation = { input, isFolder, isDialogOpen ->
                            homeViewModel.createFile(input, isFolder) {
                                isDialogOpen.value = false
                            }
                        },
                        serverIcon = {
                            IconButton(onClick = {
                                val running = homeViewModel.isServerRunning
                                homeViewModel.toggleServer()
                                if (!running) showTerminalSheet = true
                            }) {
                                if (!homeViewModel.isServerRunning)
                                    Icon(Icons.Default.PlayArrow, "Start server")
                                else
                                    Icon(painterResource(R.drawable.stop), "Stop server")
                            }
                        }
                    ) { cmd ->
                        homeViewModel.exec(cmd)
                        showTerminalSheet = true
                    }
                },
                navigationIcon = {
                    if (state.cwd.contains("$HOME_DIR/"))
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
        val files = state.files

        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {
            Text(
                text = state.cwd.formatDir(" / "),
                modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
            ) {
                if (state.cwd == HOME_DIR && state.filesCount == 0)
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
                            onClick = { homeViewModel.create("test") },
                            modifier = Modifier.padding(top = 16.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp
                            )
                        ) {
                            if (state.isCreating)
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
                    if (state.filesCount > 1)
                        item {
                            OutlinedTextField(
                                value = state.query,
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                onValueChange = homeViewModel::search,
                                label = {
                                    Text("Search among ${state.filesCount} items")
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Search
                                ),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        homeViewModel.search(state.query)
                                        focusManager.clearFocus()
                                    }
                                ),
                                trailingIcon = {
                                    if (state.query.isNotBlank())
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

                    items(files.size, key = { state.cwd + files[it].path }) {
                        FileButton(
                            file = files[it],
                            modifier = Modifier.padding(8.dp),
                            refresh = { homeViewModel.refresh() },
                            onClick = {
                                if (files[it].isDir) {
                                    resetQuery()
                                    homeViewModel.openDir(files[it].name)
                                } else if (!files[it].name.contains(".gitconfig")) {
                                    if (files[it].usesBuiltInEditor()) onOpenFile(files[it].path)
                                    else files[it].openInExternalApp(context, false)
                                }
                            },
                            onLongClick = {
                                if (!files[it].isDir) files[it].openInExternalApp(context, true)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }

        if (state.showCopyConfirm) GenericDialog(
            isCancellable = false,
            dialogTitle = "Copy",
            dialogText = "Are you sure you want to copy this file here?",
            onDismissRequest = { homeViewModel.dismissCopy() },
            onConfirmation = {
                val name = state.pendingCopyUri?.let {
                    DocumentFile.fromSingleUri(context, it)?.name
                }
                if (name != null) {
                    homeViewModel.confirmCopy(name) {
                        Toast.makeText(context, "Error copying file!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    homeViewModel.dismissCopy()
                }
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && state.showNotifRationale) {
            GenericDialog(
                isCancellable = false,
                dialogTitle = "Permission request",
                dialogText = "This app requires the notification permission to let you know " +
                        "about running background tasks and control them from the notification.",
                confirmText = "OK",
                dismissText = "No thanks",
                onDismissRequest = {
                    homeViewModel.setNotifRationale(false)

                    Toast.makeText(
                        context,
                        "You will not be asked again!",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onConfirmation = {
                    homeViewModel.setNotifRationale(false)
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            )
        }

        val sessions = homeViewModel.sessionManager
        if (showTerminalSheet && sessions != null) {
            TerminalSheet(
                isServiceBound = true,
                sessionManager = sessions,
                onDismiss = { showTerminalSheet = false }
            )
        }
    }
}

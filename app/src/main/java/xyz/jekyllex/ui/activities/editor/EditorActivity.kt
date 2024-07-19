package xyz.jekyllex.ui.activities.editor

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.jekyllex.R
import xyz.jekyllex.services.ProcessService
import xyz.jekyllex.ui.activities.editor.components.DropDownMenu
import xyz.jekyllex.ui.activities.editor.components.Editor
import xyz.jekyllex.ui.activities.editor.components.Preview
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.components.TerminalSheet
import xyz.jekyllex.utils.Commands.Companion.echo
import xyz.jekyllex.utils.Commands.Companion.jekyll
import xyz.jekyllex.utils.Commands.Companion.rm
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.getProjectDir

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

        enableEdgeToEdge()

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
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(0.dp),
                            text = file.formatDir("/"),
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
                    if (isBound.value)
                    DropDownMenu(
                        serverItemText = if (service.isRunning) "Stop server" else "Start server",
                        runServer = {
                            if (!service.isRunning)
                                file.getProjectDir()?.let { dir ->
                                    service.exec(jekyll("serve"), dir)
                                }
                            else
                                service.killProcess()
                        },
                        openTerminal = {
                            showTerminalSheet = true
                        },
                        deleteFile = {
                            service.exec(rm(file))
                            context.finish()
                        }
                    )
                }
            )
        }
    ) { innerPadding ->
        val tabs = listOf("Editor", "Preview")
        var tabIndex by remember { mutableIntStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }

            when (tabIndex) {
                0 -> Editor(file, innerPadding)
                1 -> Preview( file, isBound.value && service.isRunning, innerPadding) {
                    file.getProjectDir()?.let { dir ->
                        service.exec(jekyll("serve"), dir)
                    }
                }
            }
        }

        if (showTerminalSheet) {
            TerminalSheet(
                isServiceBound = isBound.value,
                isRunning = service.isRunning,
                onDismiss = { showTerminalSheet = false },
                clearLogs = { service.clearLogs() },
                logs = service.logs.collectAsState().value,
                exec = { command: Array<String> ->
                    file.getProjectDir()?.let { dir ->
                        service.exec(command, dir)
                    }
                },
            )
        }
    }
}

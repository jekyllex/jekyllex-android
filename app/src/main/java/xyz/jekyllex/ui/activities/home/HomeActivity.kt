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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import xyz.jekyllex.R
import xyz.jekyllex.services.ProcessService
import xyz.jekyllex.ui.components.JekyllExAppBar
import xyz.jekyllex.ui.theme.JekyllExTheme
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

private fun create(callBack: () -> Unit = {}) {
    Log.d("JekyllEx", isBound.toString())
    if (!isBound) return
    service.exec(arrayOf("jekyll", "new", "test"), callBack = callBack)
}

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel()
) {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        JekyllExAppBar(title = { Text(text = "Home") }, actions = {
            if (homeViewModel.cwd.value != HOME_DIR)
                IconButton(onClick = { homeViewModel.cd(HOME_DIR) }) {
                    Icon(Icons.Default.Home, "Create new project")

                }
            if (homeViewModel.cwd.value == HOME_DIR)
                IconButton(onClick = {
                    create { homeViewModel.refresh() }
                }) {
                    Icon(Icons.Default.AddCircle, "Create new project")
                }
            else if (homeViewModel.cwd.value.contains(HOME_DIR)) {
                IconButton(onClick = {
                    if (!isBound) return@IconButton
                    if(!service.isRunning)
                        service.exec(arrayOf("jekyll", "serve"), homeViewModel.cwd.value)
                    else
                        service.killProcess()
                }) {
                    if(!service.isRunning)
                        Icon(Icons.Default.PlayArrow, "Delete this project")
                    else
                        Icon(painterResource(R.drawable.stop), "Delete this project")
                }
                IconButton(onClick = {
                    NativeUtils.exec(arrayOf("rm", "-rf", homeViewModel.cwd.value))
                    homeViewModel.cd("..")
                }) {
                    Icon(Icons.Default.Delete, "Delete this project")
                }
            }
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
                        Text(text = "No projects found!")
                        Button(onClick = { create { homeViewModel.refresh() } }) {
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

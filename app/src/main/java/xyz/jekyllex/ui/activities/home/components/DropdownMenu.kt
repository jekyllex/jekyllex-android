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

package xyz.jekyllex.ui.activities.home.components

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import xyz.jekyllex.R
import xyz.jekyllex.ui.activities.home.HomeViewModel
import xyz.jekyllex.ui.activities.settings.SettingsActivity
import xyz.jekyllex.ui.components.GenericDialog
import xyz.jekyllex.utils.Commands.bundle
import xyz.jekyllex.utils.Commands.mkDir
import xyz.jekyllex.utils.Commands.touch
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.formatDir

@Composable
fun DropDownMenu(
    homeViewModel: HomeViewModel,
    isCreating: MutableState<Boolean>,
    picker: ActivityResultLauncher<String>,
    resetQuery: () -> Unit,
    serverIcon: @Composable () -> Unit,
    onCreateConfirmation: (String, MutableState<Boolean>) -> Unit,
    exec: (Array<String>) -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val openCreateDialog = remember { mutableStateOf(false) }

    if (openCreateDialog.value) {
        if (homeViewModel.cwd.value == HOME_DIR) {
            CreateProjectDialog(
                isCreating = isCreating.value,
                onDismissRequest = { openCreateDialog.value = false },
                onConfirmation = { onCreateConfirmation(it, openCreateDialog) }
            )
        }
        else {
            CreateFileDialog(
                picker = picker,
                isOpen = openCreateDialog,
                isCreating = isCreating.value,
                onDismissRequest = { openCreateDialog.value = false },
                onConfirmation = { input, isFolder ->
                    val cwd = homeViewModel.cwd.value
                    val command = if (isFolder) mkDir(input) else touch(input)
                    homeViewModel.appendLog(
                        "${cwd.formatDir("/")} $ ${command.joinToString(" ")}"
                    )
                    NativeUtils.exec(command, cwd)
                    homeViewModel.refresh()
                    openCreateDialog.value = false
                }
            )
        }
    }

    if (homeViewModel.cwd.value != HOME_DIR) {
        IconButton(onClick = { resetQuery(); homeViewModel.cd(HOME_DIR) }) {
            Icon(Icons.Default.Home, "Go back to home")
        }
        serverIcon()
    }

    IconButton(enabled = !isCreating.value, onClick = { openCreateDialog.value = true }) {
        Icon(Icons.Default.AddCircle, "Create new project")
    }

    Box {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = Icons.Default.MoreVert, contentDescription = "More"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(text = { Text("Refresh") }, onClick = {
                expanded = !expanded
                homeViewModel.refresh()
            })
            DropdownMenuItem(text = { Text("Settings") }, onClick = {
                expanded = !expanded
                context.startActivity(
                    Intent(context, SettingsActivity::class.java)
                )
            })
            if (homeViewModel.cwd.value.contains("$HOME_DIR/")) {
                DropdownMenuItem(text = { Text("bundle install") }, onClick = {
                    expanded = !expanded
                    exec(bundle("install"))
                })
            } else {
                DropdownMenuItem(text = { Text("Share app") }, onClick = {
                    expanded = !expanded
                    context.startActivity(Intent().apply {
                        type = "text/plain"
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text))
                    })
                })
            }
        }
    }
}

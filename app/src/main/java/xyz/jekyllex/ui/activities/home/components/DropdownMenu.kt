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
import xyz.jekyllex.ui.activities.home.HomeViewModel
import xyz.jekyllex.ui.activities.settings.SettingsActivity
import xyz.jekyllex.ui.components.DeleteDialog
import xyz.jekyllex.utils.Commands.Companion.bundle
import xyz.jekyllex.utils.Commands.Companion.mkDir
import xyz.jekyllex.utils.Commands.Companion.touch
import xyz.jekyllex.utils.Constants.Companion.HOME_DIR

@Composable
fun DropDownMenu(
    homeViewModel: HomeViewModel,
    isCreating: MutableState<Boolean>,
    serverIcon: @Composable () -> Unit,
    onCreateConfirmation: (String, MutableState<Boolean>) -> Unit,
    onDeleteConfirmation: (MutableState<Boolean>) -> Unit,
    exec: (Array<String>, String, (() -> Unit)?) -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val openDeleteDialog = remember { mutableStateOf(false) }
    val openCreateDialog = remember { mutableStateOf(false) }

    if (openCreateDialog.value) {
        if (homeViewModel.cwd.value == HOME_DIR)
            CreateProjectDialog(
                isCreating = isCreating.value,
                onDismissRequest = { openCreateDialog.value = false },
                onConfirmation = { onCreateConfirmation(it, openCreateDialog) }
            )
        else
            CreateFileDialog(
                isCreating = isCreating.value,
                onDismissRequest = { openCreateDialog.value = false },
                onConfirmation = { input, isFolder ->
                    val command = if (isFolder) mkDir(input) else touch(input)
                    exec(command, homeViewModel.cwd.value) {
                        homeViewModel.refresh()
                    }
                    openCreateDialog.value = false
                }
            )
    }

    if (openDeleteDialog.value) DeleteDialog(
        dialogTitle = "Delete",
        dialogText = "Are you sure you want to delete the current directory?",
        onDismissRequest = { openDeleteDialog.value = false },
        onConfirmation = { onDeleteConfirmation(openDeleteDialog) },
    )

    if (homeViewModel.cwd.value != HOME_DIR) {
        IconButton(onClick = { homeViewModel.cd(HOME_DIR) }) {
            Icon(Icons.Default.Home, "Go back to home")

        }
        serverIcon()
    }
    IconButton(
        enabled = !isCreating.value,
        onClick = { openCreateDialog.value = true }
    ) {
        Icon(Icons.Default.AddCircle, "Create new project")
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
                text = { Text("Refresh") },
                onClick = {
                    expanded = !expanded
                    homeViewModel.refresh()
                }
            )
            if (homeViewModel.cwd.value.contains("$HOME_DIR/")) {
                DropdownMenuItem(
                    text = { Text("Bundler") },
                    onClick = {
                        expanded = !expanded
                        exec(bundle("install"), homeViewModel.cwd.value, null)
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
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = {
                    expanded = !expanded
                    context.startActivity(
                        Intent(context, SettingsActivity::class.java)
                    )
                }
            )
        }
    }
}

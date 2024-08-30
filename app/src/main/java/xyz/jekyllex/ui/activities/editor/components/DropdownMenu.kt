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

package xyz.jekyllex.ui.activities.editor.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import xyz.jekyllex.ui.components.GenericDialog

@Composable
fun DropDownMenu(
    runServer: () -> Unit = {},
    openTerminal: () -> Unit = {},
    serverItemText: String = "Start server",
    renameFile: (String) -> Unit = {},
    deleteFile: () -> Unit = {},
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var openDeleteDialog by remember { mutableStateOf(false) }
    var openRenameDialog by remember { mutableStateOf(false) }

    if (openRenameDialog) {
        var value by remember { mutableStateOf("") }
        val onValueChange: (String) -> Unit = { value = it }

        val onOk: () -> Unit = onOk@{
            if (value.isBlank()) {
                Toast.makeText(
                    context,
                    "New name cannot be empty",
                    Toast.LENGTH_SHORT
                ).show()

                return@onOk
            }

            renameFile(value)
            openRenameDialog = false
        }

        BasicAlertDialog(
            onDismissRequest = { openRenameDialog = false },
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Rename",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        label = { Text("New name") },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        keyboardOptions = KeyboardOptions(autoCorrect = false),
                        keyboardActions = KeyboardActions { onOk() },
                        singleLine = true
                    )

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                    ) {
                        TextButton(onClick = { openRenameDialog = false }) {
                            Text("Cancel")
                        }

                        TextButton(onClick = onOk) {
                            Text("Done")
                        }
                    }
                }
            }
        }

    }

    if (openDeleteDialog) GenericDialog(
        dialogTitle = "Delete",
        dialogText = "Are you sure you want to delete this file?",
        onDismissRequest = { openDeleteDialog = false },
        onConfirmation = { openDeleteDialog = false; deleteFile() },
    )

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
            text = { Text(serverItemText) },
            onClick = { expanded = false; runServer() }
        )
        DropdownMenuItem(
            text = { Text("Show logs") },
            onClick = { expanded = false; openTerminal() }
        )
        DropdownMenuItem(
            text = { Text("Rename file") },
            onClick = { expanded = false; openRenameDialog = true }
        )
        DropdownMenuItem(
            text = { Text("Delete file") },
            onClick = { expanded = false; openDeleteDialog = true }
        )
    }
}

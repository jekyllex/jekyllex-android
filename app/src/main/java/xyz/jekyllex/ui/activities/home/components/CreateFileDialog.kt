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

import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun CreateFileDialog(
    isCreating: Boolean,
    isOpen: MutableState<Boolean>,
    onDismissRequest: () -> Unit,
    picker: ActivityResultLauncher<String>,
    onConfirmation: (name: String, isFolder: Boolean) -> Unit,
) {
    BasicAlertDialog(onDismissRequest = { isOpen.value = false }) {
        val context = LocalContext.current
        var file by remember { mutableStateOf("") }
        var isFolder by remember { mutableStateOf(false) }
        val keyboardController = LocalSoftwareKeyboardController.current

        val onDone: () -> Unit = run@{
            val msg = when {
                file.isBlank() -> "Name can't by empty"
                file.trim().contains(" ") -> "Name can't contain spaces"
                !URLUtil.isValidUrl(file) && file.trim().contains("/") -> "Name can't contain '/'"
                else -> ""
            }

            if (msg.isNotBlank()) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                return@run
            }

            keyboardController?.hide()
            onConfirmation(file.trim(), isFolder)
            file = ""
        }

        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "New",
                        modifier = Modifier.align(Alignment.CenterVertically),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Row(
                        modifier = Modifier.clickable(
                            indication = null, onClick = { isFolder = !isFolder },
                            interactionSource = remember { MutableInteractionSource() }
                        )
                    ) {
                        Text(
                            text = "Folder",
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .offset(x = 4.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Checkbox(
                            checked = isFolder,
                            modifier = Modifier.scale(0.75f),
                            onCheckedChange = { isFolder = it },
                        )
                    }
                }

                if (!isCreating) {
                    OutlinedTextField(
                        value = file,
                        singleLine = true,
                        onValueChange = { file = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardActions = KeyboardActions(onDone = { onDone() }),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        label = {
                            Text("Enter the name${if (isFolder) "" else " or URL"} " +
                                    "of the ${if (isFolder) "folder" else "file"}")
                        },
                    )
                }

                if ((!isFolder && file.isNotBlank()) || isFolder) {
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

                if (!isFolder && file.isBlank()) {
                    Column {
                        Text(
                            text = "OR",
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 10.dp, bottom = 6.dp)
                        )
                        Button(
                            onClick = {
                                picker.launch("*/*")
                                isOpen.value = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose from storage")
                        }
                    }
                }
            }
        }
    }
}

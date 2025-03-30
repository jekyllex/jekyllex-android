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

package xyz.jekyllex.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.toCommand

@Composable
fun TerminalSheet(
    cwd: String = "",
    exec: (Array<String>) -> Unit = {},
    logs: List<String> = listOf(),
    clearLogs: () -> Unit = {},
    onDismiss: () -> Unit = {},
    isServiceBound: Boolean = false,
    isRunning: Boolean = false,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var text by rememberSaveable { mutableStateOf("") }
    val state = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    LaunchedEffect(logs.size) {
        listState.animateScrollToItem(logs.size)
    }

    fun run() {
        if (text.isBlank()) return
        if (isRunning) {
            Toast.makeText(
                context,
                "A process is already running",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        exec(text.toCommand())
        text = ""
    }

    ModalBottomSheet(
        sheetState = state,
        onDismissRequest = onDismiss,
        modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
            ) {
                Text(
                    text = "Session logs",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                Spacer(Modifier.weight(1f))
                if (logs.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(
                                AnnotatedString(logs.joinToString("\n"))
                            )

                            Toast.makeText(
                                context,
                                "Copied to clipboard!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text("Copy")
                    }
                }
                Button(
                    onClick = clearLogs,
                ) {
                    Text(text = "Clear")
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().let {
                    if (logs.size > 25) it.weight(1.0f) else it
                }
            ) {
                items(logs.size) {
                    Text(
                        text = logs[it],
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            if (isServiceBound) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.LightGray,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .padding(top = 8.dp)
                )
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        maxLines = 1,
                        text = cwd.formatDir("/"),
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        BasicTextField(
                            value = text,
                            singleLine = true,
                            onValueChange = { text = it },
                            keyboardActions = KeyboardActions(onDone = { run() }),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1.0f).align(Alignment.CenterVertically),
                            decorationBox = { innerTextField ->
                                innerTextField()
                                if (text.isEmpty()) {
                                    Text(
                                        color = Color.Gray,
                                        text = "Enter a command",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        )
                        TextButton(
                            onClick = { run() },
                            contentPadding = PaddingValues(0.dp),
                            enabled = text.isNotBlank() && !isRunning,
                            modifier = Modifier.size(60.dp, 30.dp).padding(start = 8.dp)
                        ) { Text(text = "Run") }
                    }
                }
            }
        }
    }
}

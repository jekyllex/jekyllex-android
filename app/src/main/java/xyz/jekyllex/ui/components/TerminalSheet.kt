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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import xyz.jekyllex.models.Command
import xyz.jekyllex.services.SessionManager
import xyz.jekyllex.utils.Commands.getProjectCommands
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.getProjectDir
import xyz.jekyllex.utils.toCommand

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun TerminalSheet(
    onDismiss: () -> Unit = {},
    sessionManager: SessionManager,
    isServiceBound: Boolean = false,
) {
    val context = LocalContext.current
    val logsListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val sessionsListState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    var text by rememberSaveable { mutableStateOf("") }
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var projectCommands by rememberSaveable { mutableStateOf<List<Command>>(emptyList()) }

    val sessions = sessionManager.sessions.collectAsState().value
    val activeSession = sessionManager.activeSession.collectAsState().value

    val sessionDir = combine(
        sessionManager.sessions, sessionManager.activeSession
    ) { s, activeS -> s[activeS] }.flatMapLatest { it.dir }
        .collectAsState(initial = "").value

    val logs: List<String> = combine(
        sessionManager.sessions, sessionManager.activeSession
    ) { s, activeS -> s[activeS] }.flatMapLatest { it.logs }
        .collectAsState(initial = emptyList()).value

    val runningCommands: List<String> = sessionManager.sessions
        .flatMapLatest { s ->
            val commandFlow = s.map { it.runningCommand }
            if (commandFlow.isEmpty()) flowOf(emptyList())
            else combine(commandFlow) { f -> f.map { it.substringBefore(" ") } }
        }.collectAsState(initial = emptyList()).value

    LaunchedEffect(logs.size) {
        logsListState.animateScrollToItem(logs.size)
    }

    LaunchedEffect(activeSession) {
        sessionsListState.animateScrollToItem(activeSession)
    }

    LaunchedEffect(activeSession, sessions.size, state.isVisible) {
        projectCommands = emptyList()
        sessionDir.getProjectDir()?.let { dir ->
            NativeUtils.exec(getProjectCommands(), CoroutineScope(Dispatchers.IO), dir) { out ->
                out.split("\u001F").takeIf { it.size > 1 && it.size % 2 == 0 }
                    ?.let { it.chunked(2).map { (name, cmd) -> Command(name, cmd) } }
                    ?.let { projectCommands = it }
            }
        }
    }

    fun run() {
        if (text.isBlank()) return
        if (sessionManager.isRunning) {
            Toast.makeText(
                context,
                "A process is already running",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        sessionManager.exec(text.toCommand())
        text = ""
    }

    ModalBottomSheet(
        sheetState = state,
        onDismissRequest = onDismiss,
        modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
            Row(Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
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
                    onClick = sessionManager::clearLogs,
                ) {
                    Text(text = "Clear")
                }
            }
            LazyRow(
                state = sessionsListState,
                modifier = Modifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(sessions.size) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                        TextButton(
                            modifier = Modifier
                                .then(
                                    when (it) {
                                        0 -> Modifier.padding(start = 16.dp)
                                        else -> Modifier
                                    }
                                )
                                .then(
                                    if (it == activeSession) Modifier.border(
                                        BorderStroke(1.dp, Color.LightGray),
                                        CircleShape
                                    ) else Modifier
                                ),
                            onClick = {
                                sessionManager.setActiveSession(it)
                                coroutineScope.launch { sessionsListState.animateScrollToItem(it) }
                            },
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                text = runningCommands.getOrNull(it)?.takeIf { v -> v.isNotBlank() }
                                    ?: if (it == 0) "Default session" else "Session ${sessions[it].number}",
                            )
                            if (it != 0 && it == activeSession) {
                                IconButton(
                                    onClick = { sessionManager.deleteSession(it) },
                                    modifier = Modifier.padding(start = 6.dp).size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear session",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    IconButton(
                        onClick = sessionManager::createSession,
                        modifier = Modifier.padding(start = 4.dp, end = 16.dp).size(28.dp)
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create session",
                        )
                    }
                }
            }
            LazyColumn(
                state = logsListState,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).let {
                    if (logs.size > 25) it.weight(1.0f) else it
                }
            ) {
                items(logs.size) {
                    Text(
                        text = logs[it],
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp).let { modifier ->
                            if (it == logs.size - 1) modifier.padding(bottom = 8.dp) else modifier
                        }
                    )
                }
            }
            if (isServiceBound) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.LightGray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    AnimatedContent(
                        label = "Description animation",
                        targetState = projectCommands,
                        transitionSpec = {
                            fadeIn() + slideInVertically(animationSpec = tween(400)) togetherWith
                                    fadeOut(animationSpec = tween(200))
                        }
                    ) { commands ->
                        if (commands.isEmpty()) return@AnimatedContent
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        ) {
                            items(commands.size) { i ->
                                OutlinedButton(
                                    onClick = { text = commands[i].command },
                                    border = BorderStroke(1.dp, Color.LightGray),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.height(24.dp).widthIn(max = 150.dp),
                                ) {
                                    Text(
                                        color = Color.DarkGray,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall,
                                        text = commands[i].name.ifBlank { commands[i].command },
                                        modifier = Modifier.padding(
                                            horizontal = 4.dp,
                                            vertical = 2.dp
                                        ),
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        text = sessionDir.formatDir("/"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row {
                        BasicTextField(
                            value = text,
                            singleLine = true,
                            onValueChange = { text = it },
                            textStyle = MaterialTheme.typography.bodySmall,
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
                            enabled = text.isNotBlank() && !sessionManager.isRunning,
                            modifier = Modifier.size(60.dp, 30.dp).padding(start = 8.dp)
                        ) { Text(text = "Run") }
                    }
                }
            }
        }
    }
}

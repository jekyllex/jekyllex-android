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

package xyz.jekyllex.models

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import java.io.BufferedReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.jekyllex.utils.Constants.BIN_DIR
import xyz.jekyllex.utils.Constants.COMMAND_NOT_ALLOWED
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.NativeUtils.buildEnvironment
import xyz.jekyllex.utils.drop
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.override
import java.io.File

private const val LOG_TAG = "Session"

data class Session(
    val number: Int,
    val buildEnvironment: (cwd: String, context: Context?) -> Array<String>,
    var initialDir: String? = null,
    val notificationCallback: () -> Unit = {}
) {
    private var trimLogs = false
    private lateinit var process: Process
    private lateinit var reader: BufferedReader
    private val processBuilder = ProcessBuilder()

    private var _dir = MutableStateFlow(initialDir ?: HOME_DIR)
    val dir get() = _dir.asStateFlow()

    private var _isRunning = mutableStateOf(false)
    val isRunning get() = _isRunning.value

    private val _logs = MutableStateFlow(emptyList<String>())
    val logs get() = _logs.asStateFlow()

    private var _runningCommand = MutableStateFlow("")
    val runningCommand get() = _runningCommand.asStateFlow()

    fun appendLog(log: String) {
        _logs.update {
            val updatedLogs = it + log
            if (trimLogs) updatedLogs.takeLast(200) else updatedLogs
        }
    }

    fun clearLogs() {
        _logs.update { emptyList() }
    }

    fun setLogTrimming(shouldTrim: Boolean) {
        trimLogs = shouldTrim
    }

    fun exec(
        command: Array<String>,
        overrideDir: String? = null,
        callBack: () -> Unit = {}
    ) {
        if (_isRunning.value) return

        val execDir = overrideDir ?: _dir.value
        appendLog("${execDir.formatDir("/")} $ ${command.joinToString(" ")}")

        val overrideFn = command.override(this)
        if (overrideFn != null) { overrideFn(); callBack(); return }

        CoroutineScope(Dispatchers.IO).launch {
            _isRunning.value = true
            _runningCommand.update { command.joinToString(" ") }
            notificationCallback()

            try {
                val environment = processBuilder.environment()
                buildEnvironment(execDir).forEach {
                    val envVar = it.split("=")
                    if (envVar.size != 2) return@forEach
                    environment[envVar[0]] = envVar[1]
                }

                process = processBuilder
                    .command(
                        if (command[0].contains("/bin")) command.toMutableList()
                        else mutableListOf("$BIN_DIR/${command.getOrNull(0)}", *command.drop(1))
                    )
                    .directory(File(execDir))
                    .redirectErrorStream(true)
                    .start()

                reader = process.inputStream.bufferedReader()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        reader.use {
                            it.forEachLine { line ->
                                appendLog(line)
                            }
                        }
                    } catch (e: Exception) {
                        Log.d(LOG_TAG, "Exception while reading output: $e")
                    }
                }

                val exitCode = process.waitFor()
                if (exitCode != 0) appendLog("Process exited with code $exitCode")
            } catch (e: Exception) {
                appendLog("${e.cause}")
                if (::process.isInitialized) process.destroy()
                Log.e(LOG_TAG, "Error while starting process: $e")
            } finally {
                stop()
                callBack()
            }
        }
    }

    fun cd(loc: String) {
        val currentDir = _dir.value
        if (loc == currentDir) return
        val file = loc.replace("~", HOME_DIR)
        val newDir = when {
            file == "." -> currentDir
            file.isEmpty() -> HOME_DIR
            file.getOrNull(0) == '/' -> file
            file == ".." -> currentDir.substringBeforeLast('/')
            else -> if (currentDir.last() == '/') "$currentDir$file" else "$currentDir/$file"
        }
        val jFile = File(newDir).canonicalFile
        if (!jFile.exists() || !jFile.isDirectory) {
            appendLog("cd: no such file or directory: $loc")
        } else if (number == 0 && (!jFile.path.startsWith(HOME_DIR) || jFile.name.startsWith("."))) {
            appendLog(COMMAND_NOT_ALLOWED)
        } else {
            _dir.value = jFile.path
        }
    }

    fun kill() = ::process.isInitialized.let { if(it) process.destroy() }

    private fun stop() {
        _isRunning.value = false
        _runningCommand.update { "" }
        if (::reader.isInitialized) reader.close()
        notificationCallback()
    }
}

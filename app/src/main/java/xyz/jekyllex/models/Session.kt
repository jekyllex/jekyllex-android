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

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import java.io.BufferedReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import xyz.jekyllex.utils.Constants.BIN_DIR
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.NativeUtils.buildEnvironment
import xyz.jekyllex.utils.drop
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.isDenied
import java.io.File

private const val LOG_TAG = "Session"

class Session(val notificationCallback: () -> Unit = {}) {
    private var trimLogs = false
    private lateinit var process: Process
    private val processBuilder = ProcessBuilder()
    private var inputReader: BufferedReader? = null
    private var errorReader: BufferedReader? = null

    private var _runningCommand = ""
    private var _isRunning = mutableStateOf(false)
    private val _logs = MutableStateFlow(listOf<String>())
    val logs get() = _logs

    val isRunning
        get() = _isRunning.value
    val runningCommand
        get() = _runningCommand

    fun appendLog(log: String) {
        _logs.value += log
    }

    fun clearLogs() {
        _logs.value = listOf()
    }

    fun setLogTrimming(shouldTrim: Boolean) {
        trimLogs = shouldTrim
    }

    fun exec(
        command: Array<String>,
        dir: String = HOME_DIR,
        env: Array<String> = buildEnvironment(dir),
        callBack: () -> Unit = {}
    ) {
        if (_isRunning.value) return

        appendLog("${dir.formatDir("/")} $ ${command.joinToString(" ")}")

        if (command.isDenied()) {
            appendLog("Command not allowed!")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            _isRunning.value = true
            _runningCommand = command.joinToString(" ")
            notificationCallback()

            try {
                val environment = processBuilder.environment()
                env.forEach {
                    val envVar = it.split("=")
                    if (envVar.size != 2) return@forEach
                    environment[envVar[0]] = envVar[1]
                }

                process = processBuilder
                    .command(
                        if (command[0].contains("/bin")) command.toMutableList()
                        else mutableListOf("$BIN_DIR/${command.getOrNull(0)}", *command.drop(1))
                    )
                    .directory(File(dir))
                    .redirectErrorStream(true)
                    .start()

                inputReader = process.inputStream.bufferedReader()
                errorReader = process.errorStream.bufferedReader()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        inputReader?.forEachLine {
                            appendLog(it)
                        }
                    } catch (e: Exception) {
                        Log.d(LOG_TAG, "Exception while reading output: $e")
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        errorReader?.forEachLine {
                            appendLog(it)
                        }
                    } catch (e: Exception) {
                        Log.d(LOG_TAG, "Exception while reading error: $e")
                    }
                }

                val exitCode = process.waitFor()
                if (exitCode != 0) appendLog("Process exited with code $exitCode")

                processStopped()
                callBack()
            } catch (e: Exception) {
                processStopped()
                appendLog("${e.cause}")
                if (::process.isInitialized) process.destroy()
                Log.e(LOG_TAG, "Error while starting process: $e")
            }
        }
    }

    private fun processStopped() {
        _runningCommand = ""
        _isRunning.value = false
        if (trimLogs) _logs.value = _logs.value.takeLast(200)
        notificationCallback()
    }

    fun killProcess() {
        if (!_isRunning.value) return
        if (::process.isInitialized) process.destroy()
    }

    fun killSelf() {
        clearLogs()
        errorReader?.close()
        inputReader?.close()
        process.destroy()
        _isRunning.value = false
    }
}

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

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File as JFile
import xyz.jekyllex.models.File
import xyz.jekyllex.utils.Commands.Companion.diskUsage
import xyz.jekyllex.utils.Commands.Companion.getFromYAML
import xyz.jekyllex.utils.Commands.Companion.shell
import xyz.jekyllex.utils.Commands.Companion.stat
import xyz.jekyllex.utils.Constants.Companion.HOME_DIR
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.getFilesInDir
import xyz.jekyllex.utils.mergeCommands
import xyz.jekyllex.utils.toDate
import xyz.jekyllex.utils.trimQuotes

class HomeViewModel(private val skipAnimations: Boolean) : ViewModel() {
    companion object {
        const val LOG_TAG = "HomeViewModel"
    }

    class Factory(private val skipAnimations: Boolean) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(skipAnimations) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private var statsJob: Job? = null
    private var _cwd = mutableStateOf("")
    private val _availableFiles = MutableStateFlow(listOf<File>())

    private val lsCmd
        get() = (_cwd.value == HOME_DIR).let {
            if (it) "ls -d */" else "ls -a ${_cwd.value.replace(" ", "\\ ")}"
        }

    val cwd
        get() = _cwd
    val availableFiles
        get() = _availableFiles

    var appendLog: (String) -> Unit = {}

    init {
        cd(HOME_DIR)
    }

    fun cd(dir: String) {
        statsJob?.let { it.cancel(); statsJob = null; Log.d(LOG_TAG, "Cancelled stats job") }
        appendLog("${_cwd.value.formatDir("/")}$ cd ${dir.formatDir("/")}")

        if (dir == "..")
            _cwd.value = _cwd.value.substringBeforeLast('/')
        else if (dir[0] == '/')
            _cwd.value = dir
        else
            _cwd.value += "/$dir"

        refresh()
        appendLog("${_cwd.value.formatDir("/")}$")
    }

    fun refresh() {
        try {
            val files = NativeUtils
                .exec(shell(lsCmd))
                .getFilesInDir(_cwd.value)

            _availableFiles.value = files
                .filter { it !in listOf(".", "..", ".git") }
                .map {
                    File(it, isDir = JFile("${_cwd.value}/$it").isDirectory)
                }

            if (!skipAnimations)
                statsJob = viewModelScope.launch(Dispatchers.IO) { fetchStats() }

            Log.d(LOG_TAG, "Available files in ${_cwd.value}: $files")
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Error while listing files in ${_cwd.value}: $e")
        }
    }

    private suspend fun fetchStats() {
        _availableFiles.value = _availableFiles.value.map {
            yield()

            val stats = NativeUtils.exec(
                shell(
                    mergeCommands(
                        diskUsage("-sh", it.name),
                        stat("-c", "%Y", it.name)
                    )
                ),
                _cwd.value
            ).split("\n")

            val properties =
                if (_cwd.value == HOME_DIR) NativeUtils.exec(
                    getFromYAML(
                        "${it.name}/_config.yml",
                        "title", "description", "url", "baseurl"
                    )
                ).split("\n").map { prop ->
                    if (prop.isBlank()) null
                    else prop.trimQuotes()
                }
                else listOf()

            it.copy(
                title = properties.getOrNull(0),
                description = properties.getOrNull(1),
                lastModified = stats.getOrNull(1)?.toDate(),
                size = stats.getOrNull(0)?.split("\t")?.first(),
                url = properties.getOrNull(2)?.let { url ->
                    url + (properties.getOrNull(3) ?: "")
                }
            )
        }
    }
}


/*
 * MIT License
 *
 * Copyright (c) 2021 Gourav Khunger
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

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import xyz.jekyllex.utils.Commands.diskUsage
import xyz.jekyllex.utils.Commands.getFromYAML
import xyz.jekyllex.utils.Commands.shell
import xyz.jekyllex.utils.Commands.stat
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.formatDir
import xyz.jekyllex.utils.getFilesInDir
import xyz.jekyllex.utils.parseOutput
import xyz.jekyllex.utils.mergeCommands
import xyz.jekyllex.utils.toDate

class HomeViewModel(private var skipAnimations: Boolean) : ViewModel() {
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

    var fileUri: Uri? = null
    private var query: String = ""
    private var statsJob: Job? = null
    var isBound by mutableStateOf(false)
    private var _cwd = mutableStateOf("")
    var isCreating by mutableStateOf(false)
    var copyFileConfirmation by mutableStateOf(false)
    var notificationRationale by mutableStateOf(false)
    private val _availableFiles = MutableStateFlow(listOf<File>())
    private val _searchedFiles = MutableStateFlow(listOf<File>())

    private val lsCmd
        get() = (_cwd.value == HOME_DIR).let {
            if (it) "ls -d */" else "ls -a ${_cwd.value.replace(" ", "\\ ")}"
        }

    val cwd
        get() = _cwd
    val availableFiles
        get() = _searchedFiles
    val filesCount
        get() = _availableFiles.value.size

    var appendLog: (String) -> Unit = {}

    init {
        cd(HOME_DIR)
    }

    fun search(query: String) {
        this.query = query

        if (query.isBlank()) {
            _searchedFiles.value = _availableFiles.value
            return
        }

        _searchedFiles.value = _availableFiles.value.filter {
            it.name.contains(query, true) or
            it.url.orEmpty().contains(query, true) or
            it.size.orEmpty().contains(query, true) or
            it.title.orEmpty().contains(query, true) or
            it.description.orEmpty().contains(query, true) or
            it.lastModified.orEmpty().contains(query, true)
        }
    }

    fun setSkipAnimation(value: Boolean) {
        skipAnimations = value
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
                    File(
                        name = it,
                        path = "${_cwd.value}/$it",
                        isDir = JFile("${_cwd.value}/$it").isDirectory
                    )
                }

            search(query)

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
            val cwd = _cwd.value

            val stats = NativeUtils.exec(
                shell(
                    mergeCommands(
                        diskUsage("-sh", it.path),
                        stat("-c", "%Y", it.path)
                    )
                )
            ).split("\n")

            val properties =
                if (cwd == HOME_DIR)
                    NativeUtils.exec(
                        getFromYAML(
                            "${it.path}/_config.yml",
                            "title", "description", "url", "baseurl"
                        )
                    ).parseOutput()
                else if (!it.isDir && cwd.contains("/_") && !cwd.contains("/_site"))
                    NativeUtils.exec(
                        getFromYAML(it.path, "title", "description")
                    ).parseOutput()
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

        search(query)
    }
}

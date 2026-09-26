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

package xyz.jekyllex.ui.home

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import xyz.jekyllex.data.FilesRepository
import xyz.jekyllex.data.ProcessRepository
import xyz.jekyllex.models.File
import xyz.jekyllex.utils.Commands.bundle
import xyz.jekyllex.utils.Commands.curl
import xyz.jekyllex.utils.Commands.git
import xyz.jekyllex.utils.Commands.jekyll
import xyz.jekyllex.utils.Commands.mkDir
import xyz.jekyllex.utils.Commands.touch
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.cloneTemplateCommand
import xyz.jekyllex.utils.getProjectDir
import xyz.jekyllex.utils.mergeSearchResults
import xyz.jekyllex.utils.matchesQuery
import xyz.jekyllex.utils.removeSymlinks
import xyz.jekyllex.utils.repoFolderName
import xyz.jekyllex.utils.toCommand
import xyz.jekyllex.utils.uniqueProjectName
import java.io.File as JFile
import java.util.concurrent.atomic.AtomicInteger

class HomeViewModel(
    private val filesRepository: FilesRepository,
    private val process: ProcessRepository,
    private val contentResolver: ContentResolver,
    private var skipAnimations: Boolean,
) : ViewModel() {
    companion object {
        const val LOG_TAG = "HomeViewModel"
    }

    class Factory(
        private val filesRepository: FilesRepository,
        private val process: ProcessRepository,
        private val contentResolver: ContentResolver,
        private val skipAnimations: Boolean,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(
                    filesRepository,
                    process,
                    contentResolver,
                    skipAnimations,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private var listJob: Job? = null
    private var statsJob: Job? = null
    private val searchGen = AtomicInteger(0)
    private var allFiles = listOf<File>()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    val isServerRunning: Boolean
        get() = process.isRunning

    val sessionManager
        get() = process.sessionManager

    init {
        viewModelScope.launch {
            process.bound.filterNotNull().collectLatest { service ->
                service.sessionManager.sessions.collectLatest { sessions ->
                    sessions.firstOrNull()?.dir?.collect { dir -> cd(dir) }
                }
            }
        }
        cd(HOME_DIR)
    }

    fun search(query: String) {
        _uiState.update { it.copy(query = query) }
        publishSearch()
    }

    private fun publishSearch() {
        val gen = searchGen.incrementAndGet()
        val query = _uiState.value.query
        val cwd = _uiState.value.cwd
        val direct = allFiles.toList()
        if (query.isBlank()) {
            _uiState.update { it.copy(files = direct) }
            return
        }
        _uiState.update { it.copy(files = direct.filter { file -> file.matchesQuery(query) }) }
        viewModelScope.launch(Dispatchers.IO) {
            val hits = try {
                filesRepository.search(cwd, query) { searchGen.get() == gen }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Search failed in $cwd: $e")
                return@launch
            }
            val files = mergeSearchResults(direct, hits, query)
            _uiState.update { state ->
                if (searchGen.get() != gen || state.cwd != cwd || state.query != query) state
                else state.copy(files = files)
            }
        }
    }

    fun setSkipAnimation(value: Boolean) {
        skipAnimations = value
    }

    fun cd(dir: String) {
        if (dir == _uiState.value.cwd) return
        statsJob?.let { it.cancel(); statsJob = null }
        _uiState.update { it.copy(cwd = dir) }
        refresh()
    }

    fun goHome() {
        process.cd(HOME_DIR)
    }

    fun goUp() {
        process.cd("..")
    }

    fun openDir(name: String) {
        process.cd(name)
    }

    fun refresh() {
        listJob?.cancel()
        statsJob?.let { it.cancel(); statsJob = null }
        val dirPath = _uiState.value.cwd
        listJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = filesRepository.list(dirPath)
                if (!isActive || _uiState.value.cwd != dirPath) return@launch
                allFiles = files
                _uiState.update { it.copy(filesCount = files.size) }
                publishSearch()
                if (!skipAnimations) {
                    statsJob = launch { fetchStats() }
                }
                Log.d(LOG_TAG, "Available files in $dirPath: ${files.map { it.name }}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Error while listing files in $dirPath: $e")
            }
        }
    }

    fun createFromTemplate(
        url: String,
        version: String?,
        onDone: (Boolean, String) -> Unit,
    ) {
        if (_uiState.value.isCreating) return
        if (process.isRunning) {
            onDone(false, "")
            return
        }
        val dest = uniqueProjectName(repoFolderName(url), JFile(HOME_DIR).list()?.toList().orEmpty())
        if (process.bound.value == null) {
            onDone(false, dest)
            return
        }
        _uiState.update { it.copy(isCreating = true) }
        val projectDir = "$HOME_DIR/$dest"
        fun fail() {
            _uiState.update { it.copy(isCreating = false) }
            refresh()
            viewModelScope.launch(Dispatchers.Main) { onDone(false, dest) }
        }
        process.exec(cloneTemplateCommand(url, dest, version), HOME_DIR) {
            if (!JFile(projectDir, ".git").exists()) {
                fail()
                return@exec
            }
            process.sessionManager?.setActiveSession(0)
            process.cd(projectDir)
            if (process.bound.value != null) {
                process.exec(bundle("install"), projectDir)
            }
            _uiState.update { it.copy(isCreating = false, pendingTerminal = true) }
            viewModelScope.launch(Dispatchers.Main) { onDone(true, dest) }
        }
    }

    fun create(input: String, onDone: () -> Unit = {}) {
        if (_uiState.value.isCreating) return
        _uiState.update { it.copy(isCreating = true) }
        val command = createCommand(input)
        process.exec(command) {
            if (command.contentEquals(jekyll("new", input))) {
                JFile(HOME_DIR, input).removeSymlinks()
            }
            _uiState.update { it.copy(isCreating = false) }
            refresh()
            onDone()
        }
    }

    fun createFile(input: String, isFolder: Boolean, onDone: () -> Unit = {}) {
        if (_uiState.value.isCreating) return
        _uiState.update { it.copy(isCreating = true) }
        val cwd = _uiState.value.cwd
        val command = when {
            isFolder -> mkDir(input)
            URLUtil.isValidUrl(input) -> curl("-s", "-O", input)
            else -> touch(input)
        }
        process.exec(command, cwd) {
            _uiState.update { it.copy(isCreating = false) }
            refresh()
            onDone()
        }
    }

    fun onFilePicked(uri: Uri?) {
        if (uri == null) return
        _uiState.update { it.copy(pendingCopyUri = uri, showCopyConfirm = true) }
    }

    fun dismissCopy() {
        _uiState.update { it.copy(pendingCopyUri = null, showCopyConfirm = false) }
    }

    fun confirmCopy(name: String, onError: () -> Unit = {}) {
        val uri = _uiState.value.pendingCopyUri ?: return
        val cwd = _uiState.value.cwd
        viewModelScope.launch(Dispatchers.IO) {
            try {
                filesRepository.copyUri(contentResolver, uri, cwd, name)
                refresh()
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Error copying file: $e")
                withContext(Dispatchers.Main) { onError() }
            } finally {
                _uiState.update { it.copy(pendingCopyUri = null, showCopyConfirm = false) }
            }
        }
    }

    fun setNotifRationale(show: Boolean) {
        _uiState.update { it.copy(showNotifRationale = show) }
    }

    fun exec(cmd: Array<String>) {
        process.sessionManager?.setActiveSession(0)
        process.exec(cmd)
        _uiState.update { it.copy(pendingTerminal = true) }
    }

    fun consumeTerminal() {
        if (!_uiState.value.pendingTerminal) return
        _uiState.update { it.copy(pendingTerminal = false) }
    }

    fun toggleServer() {
        if (process.isRunning) {
            process.kill()
        } else {
            val cwd = _uiState.value.cwd
            process.exec(jekyll("serve"), cwd.getProjectDir() ?: cwd)
        }
    }

    private suspend fun fetchStats() {
        val cwd = _uiState.value.cwd
        val files = allFiles.map {
            yield()
            filesRepository.withStats(it, cwd)
        }
        yield()
        if (_uiState.value.cwd != cwd) return
        allFiles = files
        _uiState.update { it.copy(filesCount = allFiles.size) }
        publishSearch()
    }

    private fun createCommand(input: String): Array<String> {
        if (input.startsWith("git clone ")) return input.toCommand()
        val url = if (input.contains("github.com") && !input.contains("://")) {
            "https://$input"
        } else input
        return if (URLUtil.isValidUrl(url)) git("clone", url) else jekyll("new", input)
    }
}

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
import xyz.jekyllex.data.FilesRepository
import xyz.jekyllex.models.File
import xyz.jekyllex.utils.Constants.HOME_DIR

class HomeViewModel(
    private val filesRepository: FilesRepository,
    private var skipAnimations: Boolean,
) : ViewModel() {
    companion object {
        const val LOG_TAG = "HomeViewModel"
    }

    class Factory(
        private val filesRepository: FilesRepository,
        private val skipAnimations: Boolean,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(filesRepository, skipAnimations) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    var fileUri: Uri? = null
    private var query: String = ""
    private var listJob: Job? = null
    private var statsJob: Job? = null
    private var _cwd = mutableStateOf("")
    var isCreating by mutableStateOf(false)
    var copyFileConfirmation by mutableStateOf(false)
    var notificationRationale by mutableStateOf(false)
    private val _availableFiles = MutableStateFlow(listOf<File>())
    private val _searchedFiles = MutableStateFlow(listOf<File>())

    val cwd
        get() = _cwd
    val availableFiles
        get() = _searchedFiles
    val filesCount
        get() = _availableFiles.value.size

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
        if (dir == _cwd.value) return
        statsJob?.let { it.cancel(); statsJob = null }
        _cwd.value = dir
        refresh()
    }

    fun refresh() {
        listJob?.cancel()
        statsJob?.let { it.cancel(); statsJob = null }
        val dirPath = _cwd.value
        listJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = filesRepository.list(dirPath)
                _availableFiles.value = files
                search(query)

                if (!skipAnimations) {
                    statsJob = launch { fetchStats() }
                }

                Log.d(LOG_TAG, "Available files in $dirPath: ${files.map { it.name }}")
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Error while listing files in $dirPath: $e")
            }
        }
    }

    private suspend fun fetchStats() {
        val cwd = _cwd.value
        _availableFiles.value = _availableFiles.value.map {
            yield()
            filesRepository.withStats(it, cwd)
        }
        search(query)
    }
}

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
import xyz.jekyllex.utils.Commands.Companion.shell
import xyz.jekyllex.utils.Constants.Companion.HOME_DIR
import xyz.jekyllex.utils.Constants.Companion.USR_DIR
import xyz.jekyllex.utils.NativeUtils

class HomeViewModel : ViewModel() {
    companion object {
        const val LOG_TAG = "HomeViewModel"
    }

    private var _cwd = mutableStateOf("")
    private val _availableFolders = mutableStateOf(listOf<String>())

    val cwd
        get() = _cwd
    val availableFolders
        get() = _availableFolders
    val project: String?
        get() = _cwd.value.let {
            if (!it.contains("$HOME_DIR/")) return null
            return it
                .replace(
                    it.substringAfter("$HOME_DIR/"),
                    it.substringAfter("$HOME_DIR/").substringBefore('/')
                ).replace("$HOME_DIR/", "")
        }

    init {
        cd(HOME_DIR)
    }

    fun cd(dir: String) {
        if (dir == "..")
            _cwd.value = _cwd.value.substringBeforeLast('/')
        else if (dir[0] == '/')
            _cwd.value = dir
        else
            _cwd.value += "/$dir"

        refresh()
    }

    fun refresh() {
        try {
            val folders = NativeUtils
                .exec(shell("ls -d ${_cwd.value}/*/"))
                .split("\n")
                .map {
                    it.replace(_cwd.value, "")
                        .replace("/", "")
                }
                .filter { it.isNotBlank() }

            _availableFolders.value = folders

            Log.d(LOG_TAG, "Available files in ${_cwd.value}: $folders")
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Error while listing files in ${_cwd.value}: $e")
        }
    }
}

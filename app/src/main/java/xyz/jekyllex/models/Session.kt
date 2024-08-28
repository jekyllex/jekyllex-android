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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val LOG_TAG = "Session"

data class Session(val process: Process, var isRunning: Boolean = true) {
    private val _logs: MutableList<String> = mutableListOf()
    val logs
        get() = _logs.toTypedArray()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                process.inputStream.bufferedReader().forEachLine {
                    _logs.add(it)
                }
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Exception while reading output: $e")
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                process.errorStream.bufferedReader().forEachLine {
                    _logs.add(it)
                }
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Exception while reading error: $e")
            }
        }
    }

    fun kill() {
        process.destroy()
        isRunning = false
    }
}

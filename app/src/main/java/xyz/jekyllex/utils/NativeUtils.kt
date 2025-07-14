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

package xyz.jekyllex.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import java.io.File
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.jekyllex.ui.activities.installer.BootstrapInstaller
import xyz.jekyllex.utils.Constants.BIN_DIR
import xyz.jekyllex.utils.Constants.GEM_DIR
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.Constants.PREFIX
import xyz.jekyllex.utils.Constants.TMP_DIR

object NativeUtils {
    const val LOG_TAG = "NativeUtils"

    fun launchInstaller(context: Activity, force: Boolean = false) {
        Log.d(LOG_TAG, "Launching bootstrap installer activity")

        context.startActivity(
            Intent(context, BootstrapInstaller::class.java)
                .putExtra("force", force)
                .addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
                )
        )

        context.finish()
    }

    fun isUsable(binary: String, versionFlag: String = "--version"): Boolean {
        Log.d(LOG_TAG, "Checking if $binary exists")

        val file = File("$BIN_DIR/$binary")
        if (!file.exists()) return false

        val command = arrayOf(binary, versionFlag)

        try {
            _exec(command)
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Error while executing $command: $e")
            return false
        }

        return true
    }

    fun areUsable(binaries: Array<String>): Boolean {
        for (binary in binaries) {
            if (!isUsable(binary)) return false
        }

        return true
    }

    fun ensureDirectoryExists(directory: File?) {
        if (directory !== null && (directory.exists() || directory.mkdirs())) return
        throw RuntimeException("Unable to create directory: ${directory?.absolutePath}")
    }

    private fun _exec(command: Array<String>, dir: String = HOME_DIR): String {
        val process = Runtime.getRuntime().exec(
            if (command[0].contains("/bin")) command
            else arrayOf("$BIN_DIR/${command[0]}", *command.drop(1)),
            buildEnvironment(dir),
            File(dir)
        )

        val output = process.inputStream.bufferedReader().readText()
        Log.d(LOG_TAG, "Output for command \"${command.toList()}\": $output")

        return output.trim()
    }

    fun exec(command: Array<String>, dir: String = HOME_DIR): String {
        return try {
            _exec(command, dir)
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Error while executing $command: $e")
            ""
        }
    }

    fun exec(
        command: Array<String>,
        scope: CoroutineScope,
        dir: String = HOME_DIR,
        callback: suspend (String) -> Unit = {},
    ) = scope.launch { callback(exec(command, dir)) }

    fun buildEnvironment(cwd: String, context: Context? = null): Array<String> {
        ensureDirectoryExists(File(HOME_DIR))

        return ArrayList<String>().apply {
            add("PWD=$cwd")
            add("HOME=$HOME_DIR")
            add("PREFIX=$PREFIX")
            add("TMPDIR=$TMP_DIR")
            add("GEM_HOME=$GEM_DIR")
            add("GEM_PATH=$GEM_DIR")
            add("PATH=$BIN_DIR:${System.getenv("PATH")}")

            context?.let {
                val settings = Settings(it)
                add("JEKYLL_ENV=${settings.get<String>(Setting.JEKYLL_ENV)}")
            }
        }.toTypedArray()
    }
}

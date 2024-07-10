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
import android.content.Intent
import java.io.File
import android.util.Log
import xyz.jekyllex.ui.activities.installer.BootstrapInstaller
import xyz.jekyllex.utils.Constants.Companion.BIN_DIR

class NativeUtils {
    companion object {
        const val LOG_TAG = "NativeUtils"

        fun launchInstaller(context: Activity) {
            Log.d(LOG_TAG, "Launching bootstrap installer activity")

            context.startActivity(
                Intent(context, BootstrapInstaller::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            )

            context.finish()
        }

        fun isUsable(binary: String, versionFlag: String = "--version"): Boolean {
            Log.d(LOG_TAG, "Checking if $binary exists")

            val file = File("$BIN_DIR/$binary")
            if (!file.exists()) return false

            val command = "$BIN_DIR/$binary $versionFlag"

            try{
                exec(command)
            } catch (e: Exception) {
                Log.d(LOG_TAG, "Error while executing $command: $e")
                return false
            }

            return true
        }

        fun isUsable(vararg binaries: String): Boolean {
            for (binary in binaries) {
                if (!isUsable(binary)) return false
            }
            return true
        }

        fun ensureDirectoryExists(directory: File?) {
            if (directory !== null && !directory.exists() && !directory.mkdirs())  {
                throw RuntimeException("Unable to create directory: " + directory.absolutePath)
            }
        }

        fun exec(command: String): String {
            val process = Runtime.getRuntime().exec(command)
            val output = process.inputStream.bufferedReader().readText()
            Log.d(LOG_TAG, "Output for command \"$command\": $output")
            return output
        }

        fun exec(vararg commands: String): List<String?> {
            val outputs = mutableListOf<String?>()
            for (command in commands) {
                try {
                    outputs += exec(command).trim()
                } catch (e: Exception) {
                    Log.d(LOG_TAG, "Error while executing $command: $e")
                    outputs += null
                }
            }
            return outputs.toList()
        }
    }
}

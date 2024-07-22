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

package xyz.jekyllex.ui.activities.installer

import android.content.Intent
import android.os.Bundle
import android.system.Os
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jekyllex.R
import xyz.jekyllex.ui.activities.home.HomeActivity
import xyz.jekyllex.ui.theme.JekyllExTheme
import xyz.jekyllex.utils.Commands.Companion.shell
import xyz.jekyllex.utils.Constants.Companion.FILES_DIR
import xyz.jekyllex.utils.NativeUtils
import xyz.jekyllex.utils.Constants.Companion.USR_DIR
import xyz.jekyllex.utils.Constants.Companion.requiredBinaries
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

class BootstrapInstaller : ComponentActivity() {
    companion object {
        const val LOG_TAG = "BootstrapInstaller"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JekyllExTheme {
                Column (
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize(),
                ){
                    Text(
                        getString(R.string.installer_title),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        getString(R.string.installer_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    LinearProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if (NativeUtils.areUsable(requiredBinaries)) {
            Log.d(LOG_TAG, "Required tools already set up. Aborting re-installation...")
            finish()
            return
        }

        // Adapted from
        // https://github.com/termux/termux-app/blob/android-10/app/src/main/java/com/termux/app/TermuxPackageInstaller.java#L45
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(LOG_TAG, "Starting bootstrap installation...")

            val filesMapping = File(applicationInfo.nativeLibraryDir, "libfiles.so")
            var reader = BufferedReader(FileReader(filesMapping))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val parts = line?.split("←") ?: listOf()
                if (parts.size != 2) continue
                val oldPath = applicationInfo.nativeLibraryDir + "/" + parts[0]
                val newPath = "$USR_DIR/${parts[1]}"

                NativeUtils.ensureDirectoryExists(File(newPath).parentFile)

                File(newPath).delete()
                Os.symlink(oldPath, newPath)
            }

            val symlinksFile = File(applicationInfo.nativeLibraryDir, "libsymlinks.so")
            reader = BufferedReader(FileReader(symlinksFile))

            while (reader.readLine().also { line = it } != null) {
                val parts = line?.split("←") ?: listOf()
                if (parts.size != 2) continue
                val oldPath = parts[0]
                val newPath = "$USR_DIR/${parts[1]}"

                NativeUtils.ensureDirectoryExists(File(newPath).parentFile)

                File(newPath).delete()
                Os.symlink(oldPath, newPath)
            }

            NativeUtils.ensureDirectoryExists(File(USR_DIR, "tmp"))
            NativeUtils.ensureDirectoryExists(File(FILES_DIR, "home"))

            Log.d(LOG_TAG, "Bootstrap installation complete!")
            withContext(Dispatchers.Main) {
                startActivity(
                    Intent(this@BootstrapInstaller, HomeActivity::class.java)
                )
                finish()
            }
        }
    }
}

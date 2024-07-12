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

package xyz.jekyllex.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.jekyllex.R
import xyz.jekyllex.utils.Constants.Companion.BIN_DIR
import xyz.jekyllex.utils.Constants.Companion.HOME_DIR
import java.io.BufferedReader
import java.io.File

class ProcessService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 420
        private const val LOG_TAG = "ProcessService"
        private const val ACTION_KILL_PROCESS = "xyz.jekyllex.process_service_kill"
    }

    var runningCommand = ""
    var _isRunning = mutableStateOf(false)

    val isRunning
        get() = _isRunning.value

    private lateinit var process: Process
    private lateinit var outputReader: BufferedReader
    private lateinit var errorReader: BufferedReader
    private lateinit var notifBuilder: NotificationCompat.Builder
    private var job: Job? = null

    private val _events = MutableStateFlow("")
    val events: Flow<String> = _events.asStateFlow()

    inner class LocalBinder : Binder() {
        val service: ProcessService = this@ProcessService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::process.isInitialized) process.destroy()
        if (::errorReader.isInitialized) errorReader.close()
        if (::outputReader.isInitialized) outputReader.close()

        Log.d(LOG_TAG, "Service destroyed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(LOG_TAG, "Received action: $action")

        if (action == ACTION_KILL_PROCESS) {
            killProcess()
        }

        return START_NOT_STICKY
    }

    fun exec(command: Array<String>, dir: String = HOME_DIR, callBack: () -> Unit = {}) {
        job = CoroutineScope(Dispatchers.IO).launch {
            if (_isRunning.value) {
                _events.value = "Process is already running"
                return@launch
            }
            // Start the process
            try {
                _isRunning.value = true
                runningCommand = command.joinToString(" ")
                updateKillActionOnNotif()

                Log.d(LOG_TAG, "Starting process with command:\n\"${command.toList()}\"")

                process = Runtime.getRuntime().exec(
                    if (command[0].contains("/bin")) command
                    else arrayOf("$BIN_DIR/${command[0]}", *command.drop(1).toTypedArray()),
                    null,
                    File(dir)
                )

                outputReader = process.inputStream.bufferedReader()
                errorReader = process.errorStream.bufferedReader()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        var out: String? = outputReader.readLine()
                        while (out != null) {
                            _events.value = out
                            out = outputReader.readLine()
                        }
                    } catch (e: Exception) {
                        Log.d(LOG_TAG, "Exception while reading output: $e")
                    }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        var err: String? = errorReader.readLine()
                        while (err != null) {
                            _events.value = err
                            err = errorReader.readLine()
                        }
                    } catch (e: Exception) {
                        Log.d(LOG_TAG, "Exception while reading error: $e")
                    }
                }

                val exitCode = process.waitFor()
                _events.value = "Process exited with code $exitCode"

                killProcess()
                callBack()
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Error while starting process: $e")
            }
        }
    }

    fun killProcess() {
        if (!_isRunning.value) {
            _events.value = "No process is running"
            return
        }

        job?.cancel()
        process.destroy()

        val exitCode = process.waitFor()
        _events.value = "Process exited with code $exitCode"

        _isRunning.value = false
        runningCommand = ""

        updateKillActionOnNotif()
    }

    private fun createNotification(): Notification {
        notifBuilder = NotificationCompat
            .Builder(this, getString(R.string.process_notifications_id))
            .setContentTitle(getText(R.string.notification_text_title))
            .setContentText(getText(R.string.notification_text_waiting))
            .setSmallIcon(android.R.drawable.ic_delete)
            .setOngoing(true)
            .setShowWhen(false)
            .setColor(-0x9f8275)

        return notifBuilder.build()
    }

    @SuppressLint("RestrictedApi")
    fun updateKillActionOnNotif() {
        if(!::notifBuilder.isInitialized) return
        Log.d(LOG_TAG, "Updating notification")

        notifBuilder.mActions.clear()

        if (_isRunning.value) {
            val exitIntent = Intent(this, ProcessService::class.java)
                .setAction(ACTION_KILL_PROCESS)

            notifBuilder.setContentText("Currently running:\n$runningCommand")

            notifBuilder.addAction(
                android.R.drawable.ic_delete,
                getString(R.string.notification_action_kill),
                PendingIntent.getService(
                    this,
                    0,
                    exitIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        } else {
            notifBuilder.setContentText(getText(R.string.notification_text_waiting))
        }

        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIFICATION_ID, notifBuilder.build())
    }
}

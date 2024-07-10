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

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import xyz.jekyllex.R
import xyz.jekyllex.ui.activities.home.HomeActivity
import xyz.jekyllex.utils.Constants.Companion.HOME_DIR
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ProcessService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 420
        private const val ACTION_STOP_SERVICE = "xyz.jekyllex.process_service_stop"
        private const val ACTION_EXECUTE_COMMAND = "xyz.jekyllex.process_service_execute"
    }

    private lateinit var process: Process
    private lateinit var processWriter: BufferedWriter
    private lateinit var processReader: BufferedReader

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

        // Start the process
        val processBuilder = ProcessBuilder("/system/bin/sh")
        processBuilder.directory(File(HOME_DIR))
        process = processBuilder.start()
        processReader = BufferedReader(InputStreamReader(process.inputStream))
        processWriter = BufferedWriter(OutputStreamWriter(process.outputStream))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_SERVICE) {
            stopSelf()
        }

        else if (action == ACTION_EXECUTE_COMMAND){
            TODO()
        }

        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        val notifyIntent = Intent(this, HomeActivity::class.java)
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pendingIntent = PendingIntent.getActivity(
            this,0,
            notifyIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat
            .Builder(this, getString(R.string.process_notifications_id))
        builder.setContentTitle(getText(R.string.app_name))
        builder.setContentText("contentText")
        builder.setSmallIcon(android.R.drawable.ic_delete)
        builder.setContentIntent(pendingIntent)
        builder.setOngoing(true)

        // No need to show a timestamp:
        builder.setShowWhen(false)

        // Background color for small notification icon:
        builder.setColor(-0x9f8275)

        val res = resources
        val exitIntent = Intent(this, ProcessService::class.java)
            .setAction(ACTION_STOP_SERVICE)
        builder.addAction(
            android.R.drawable.ic_delete,
            res.getString(R.string.notification_action_destroy),
            PendingIntent.getService(
                this,
                0,
                exitIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        return builder.build()
    }
}

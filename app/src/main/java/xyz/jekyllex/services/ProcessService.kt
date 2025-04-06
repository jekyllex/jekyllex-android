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
import androidx.core.app.NotificationCompat
import xyz.jekyllex.R
import xyz.jekyllex.models.Session
import xyz.jekyllex.utils.Constants.HOME_DIR
import xyz.jekyllex.utils.NativeUtils.buildEnvironment
import xyz.jekyllex.utils.Setting
import xyz.jekyllex.utils.Settings
import xyz.jekyllex.utils.transform

class ProcessService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 420
        private const val LOG_TAG = "ProcessService"
        private const val ACTION_KILL_PROCESS = "xyz.jekyllex.process_kill"
        private const val ACTION_STOP_SERVICE = "xyz.jekyllex.service_stop"
    }

    private var hasConnections = false
    private val _sessions = mutableListOf<Session>()
    private lateinit var notifBuilder: NotificationCompat.Builder

    val sessions
        get() = _sessions.toList()
    val isRunning
        get() = _sessions.getOrNull(0)?.isRunning ?: false

    inner class LocalBinder : Binder() {
        val service: ProcessService = this@ProcessService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        hasConnections = true
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        hasConnections = false
        updateKillActionOnNotif()
        return true
    }

    override fun onRebind(intent: Intent?) {
        hasConnections = true
        super.onRebind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        _sessions.getOrElse(0) {
            Session { updateKillActionOnNotif() }.apply {
                _sessions.add(this)
                setLogTrimming(Settings(this@ProcessService).get(Setting.TRIM_LOGS))
            }
        }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "Service destroyed")
        _sessions.forEach { it.killSelf() }.also { _sessions.clear() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(LOG_TAG, "Received action: $action")

        when (action) {
            ACTION_KILL_PROCESS -> killProcess()
            ACTION_STOP_SERVICE -> stopSelf()
        }

        return START_NOT_STICKY
    }

    fun createSession() {
        val shouldTrim = Settings(this).get<Boolean>(Setting.TRIM_LOGS)

        _sessions.apply {
            forEach { it.isActive = false; it.setLogTrimming(shouldTrim) }
            add(Session(true))
        }
    }

    fun exec(
        cmd: Array<String>,
        dir: String = HOME_DIR,
        inActive: Boolean = false,
        callBack: () -> Unit = {}
    ) {
        val command = cmd.let {
            if (it[0].contains("/bin")) it
            else it.transform(this)
        }

        _sessions.let {
            if (inActive) it.firstOrNull { s -> s.isActive } ?: it.first() else it.first()
        }.exec(command, dir, buildEnvironment(dir, this), callBack)
    }

    fun killProcess() {
        _sessions.first().killProcess()
    }

    private fun createNotification(): Notification {
        notifBuilder = NotificationCompat
            .Builder(this, getString(R.string.process_notifications_id))
            .setContentTitle(getText(R.string.notification_text_title))
            .setContentText(getText(R.string.notification_text_waiting))
            .setSmallIcon(R.drawable.ic_notif_logo)
            .setOngoing(true)
            .setShowWhen(false)
            .setColor(-0x9f8275)

        return notifBuilder.build()
    }

    @SuppressLint("RestrictedApi")
    fun updateKillActionOnNotif() {
        if (!::notifBuilder.isInitialized) return
        Log.d(LOG_TAG, "Updating notification")

        notifBuilder.mActions.clear()
        val exitIntent = Intent(this, ProcessService::class.java)

        val killProcess = NotificationCompat.Action(
            R.drawable.ic_notif_logo,
            getString(R.string.notification_action_kill),
            PendingIntent.getService(
                this,
                0,
                exitIntent.setAction(ACTION_KILL_PROCESS),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        val stopService = NotificationCompat.Action(
            R.drawable.ic_notif_logo,
            getString(R.string.notification_action_stop),
            PendingIntent.getService(
                this,
                0,
                exitIntent.setAction(ACTION_STOP_SERVICE),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )

        if (_sessions.first().isRunning) {
            notifBuilder.setContentText("Currently running:\n${_sessions.first().runningCommand}")
            notifBuilder.addAction(killProcess)
        } else {
            notifBuilder.setContentText(getText(R.string.notification_text_waiting))
            if (!hasConnections) notifBuilder.addAction(stopService)
        }

        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIFICATION_ID, notifBuilder.build())
    }
}

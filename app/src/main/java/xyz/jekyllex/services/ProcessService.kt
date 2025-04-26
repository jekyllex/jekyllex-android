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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    private val _activeSession = MutableStateFlow(0)
    private val _sessions = MutableStateFlow(listOf<Session>())
    private lateinit var notifBuilder: NotificationCompat.Builder

    val isRunning
        get() = _sessions.value.firstOrNull()?.isRunning ?: false

    lateinit var sessionManager: SessionManager

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

        _sessions.value.getOrElse(0) {
            Session { updateKillActionOnNotif() }.apply {
                _sessions.value += this
                setLogTrimming(Settings(this@ProcessService).get(Setting.TRIM_LOGS))
            }
        }

        sessionManager = object: SessionManager {
            override val activeSession: StateFlow<Int>
                get() = _activeSession.asStateFlow()
            override val sessions
                get() = _sessions.asStateFlow()
            override val isRunning: Boolean
                get() = _sessions.value[_activeSession.value].isRunning

            override fun clearLogs() {
                _sessions.value[_activeSession.value].clearLogs()
            }

            override fun killProcess() {
                _sessions.value[_activeSession.value].killProcess()
            }

            override fun createSession() {
                val shouldTrim = Settings(this@ProcessService).get<Boolean>(Setting.TRIM_LOGS)

                _sessions.value.apply {
                    _sessions.update { it + Session() }
                    forEach { it.setLogTrimming(shouldTrim) }
                }

                setActiveSession(_sessions.value.size - 1)
            }

            override fun deleteSession(index: Int) {
                _sessions.value[index].killSelf()
                _sessions.update { it.filterIndexed { i, _ -> i != index } }
                if (_activeSession.value >= index) {
                    _activeSession.update { it - 1 }
                }
            }

            override fun setActiveSession(index: Int) {
                _activeSession.value = index
            }

            override fun exec(
                cmd: Array<String>,
                dir: String,
                callBack: () -> Unit
            ) {
                val command = cmd.let {
                    if (it[0].contains("/bin")) it
                    else it.transform(this@ProcessService)
                }

                _sessions.value.let {
                    it[_activeSession.value]
                }.exec(command, dir, buildEnvironment(dir, this@ProcessService), callBack)
            }
        }

        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun exec(cmd: Array<String>, dir: String = HOME_DIR, callBack: () -> Unit = {}) {
        val command = cmd.let {
            if (it[0].contains("/bin")) it
            else it.transform(this)
        }

        _sessions.value.first().exec(command, dir, buildEnvironment(dir, this), callBack)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "Service destroyed")
        _sessions.value.forEach { it.killSelf() }.also { _sessions.value = listOf() }
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

    fun killProcess() {
        _sessions.value.first().killProcess()
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

        if (_sessions.value.first().isRunning) {
            notifBuilder.setContentText("Currently running:\n${_sessions.value.first().runningCommand.value}")
            notifBuilder.addAction(killProcess)
        } else {
            notifBuilder.setContentText(getText(R.string.notification_text_waiting))
            if (!hasConnections) notifBuilder.addAction(stopService)
        }

        val notifManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notifManager.notify(NOTIFICATION_ID, notifBuilder.build())
    }
}

interface SessionManager {
    val isRunning: Boolean
    val activeSession: StateFlow<Int>
    val sessions: StateFlow<List<Session>>

    fun clearLogs()
    fun killProcess()
    fun createSession()
    fun deleteSession(index: Int)
    fun setActiveSession(index: Int)
    fun exec(
        cmd: Array<String>,
        dir: String = HOME_DIR,
        callBack: () -> Unit = {}
    )
}


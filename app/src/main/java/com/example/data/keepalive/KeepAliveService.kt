package com.example.data.keepalive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.repository.AgentServerRepository
import com.example.data.repository.OpenHandsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps OpenHands sandboxes alive while the app is in the background.
 *
 * The agent server pauses a sandbox after `runtime_idle_timeout_seconds`
 * (1200s / 20 min) without runtime activity, and permanently deletes a paused
 * conversation after the deployment's deletion window. Only calls that reach
 * the runtime reset the idle timer, so this service periodically issues a
 * lightweight runtime call per active conversation and resumes any sandbox
 * that is already paused.
 *
 * A foreground service (not WorkManager) is used because WorkManager's minimum
 * periodic interval is 15 minutes and Doze can defer it far beyond that, which
 * is too coarse to beat a 20-minute idle timer.
 */
class KeepAliveService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null
    private lateinit var runner: KeepAliveRunner

    override fun onCreate() {
        super.onCreate()
        val repository = OpenHandsRepository(
            applicationContext,
            AppDatabase.getInstance(applicationContext)
        )
        runner = KeepAliveRunner(repository, AgentServerRepository(AppDatabase.getInstance(applicationContext).conversationDao()))
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForegroundWithNotification()
        if (loop?.isActive != true) {
            loop = scope.launch { runKeepAliveLoop() }
        }
        return START_STICKY
    }

    private suspend fun runKeepAliveLoop() {
        while (scope.isActive) {
            val result = try {
                runner.tick()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                KeepAliveRunner.TickResult(0, 0)
            }
            updateNotification(result)
            delay(PING_INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Monitoring active sandboxes")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(result: KeepAliveRunner.TickResult) {
        val text = when {
            result.total == 0 -> "Waiting for active sandboxes"
            result.resumed > 0 -> "Keeping ${result.touched} alive, resumed ${result.resumed}"
            else -> "Keeping ${result.touched} sandbox(es) alive"
        }
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, KeepAliveService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenHands sandbox keep-alive")
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sandbox keep-alive",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Keeps OpenHands sandboxes running in the background" }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "openhands_keepalive"
        const val NOTIFICATION_ID = 4201
        const val ACTION_STOP = "com.example.data.keepalive.STOP"

        /**
         * 10 minutes: comfortably inside the 20-minute idle window, and two
         * pings per window absorb a single dropped or delayed request.
         */
        const val PING_INTERVAL_MS = 10 * 60 * 1000L

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }
}

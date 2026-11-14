package com.example.data.keepalive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.repository.OpenHandsRepository

/**
 * Restarts [KeepAliveService] after a reboot or an app update when the user had
 * background keep-alive switched on. Without this, a reboot silently drops the
 * keep-alive and the sandboxes pause while the user thinks they are protected.
 */
class KeepAliveBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val repository = OpenHandsRepository(
            context.applicationContext,
            AppDatabase.getInstance(context.applicationContext)
        )
        if (repository.keepAliveEnabled && repository.apiKey.isNotBlank()) {
            KeepAliveService.start(context.applicationContext)
        }
    }
}

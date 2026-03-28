package com.wireless.control.device.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 通知监听服务
 */
class NotificationListenerService : android.service.notification.NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListenerService"
        private const val NOTIFICATION_ID = 1004
        private const val CHANNEL_ID = "notification_channel"
        private const val CHANNEL_NAME = "Notification Listener Service"
    }

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        coroutineScope = CoroutineScope(Dispatchers.Main)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)

        createNotificationChannel()
        Log.i(TAG, "NotificationListenerService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        coroutineScope.launch {
            while (true) {
                try {
                    delay(5000)
                    Log.d(TAG, "NotificationListenerService running")
                } catch (e: Exception) {
                    Log.e(TAG, "Service error", e)
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock.release()
        Log.i(TAG, "NotificationListenerService destroyed")
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("无线群控")
            .setContentText("通知监听服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_info)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }
}

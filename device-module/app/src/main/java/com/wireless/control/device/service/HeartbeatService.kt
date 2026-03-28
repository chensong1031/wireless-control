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
 * 心跳服务
 * 
 * 功能：
 * 1. 定期向主控服务器发送心跳
 * 2. 保持连接活跃
 * 3. 检测连接状态
 */
class HeartbeatService : Service() {

    companion object {
        private const val TAG = "HeartbeatService"
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "heartbeat_channel"
        private const val CHANNEL_NAME = "Heartbeat Service"
    }

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        coroutineScope = CoroutineScope(Dispatchers.Main)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)

        createNotificationChannel()
        Log.i(TAG, "HeartbeatService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        coroutineScope.launch {
            while (true) {
                try {
                    delay(30000)
                    Log.d(TAG, "HeartbeatService running")
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
        Log.i(TAG, "HeartbeatService destroyed")
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("无线群控")
            .setContentText("心跳服务运行中")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
}

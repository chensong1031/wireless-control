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
 * 设备控制服务
 * 
 * 功能：
 * 1. 启动 HTTP 服务器
 * 2. 接收控制命令
 * 3. 执行设备操作
 */
class DeviceControlService : Service() {

    companion object {
        private const val TAG = "DeviceControlService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "device_control_channel"
        private const val CHANNEL_NAME = "Device Control Service"
    }

    private lateinit var coroutineScope: CoroutineScope
    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        coroutineScope = CoroutineScope(Dispatchers.Main)
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)

        createNotificationChannel()
        Log.i(TAG, "DeviceControlService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        coroutineScope.launch {
            while (true) {
                try {
                    delay(5000)
                    Log.d(TAG, "DeviceControlService running")
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
        Log.i(TAG, "DeviceControlService destroyed")
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
            .setContentText("设备控制服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_info)
            .build()
    }
}

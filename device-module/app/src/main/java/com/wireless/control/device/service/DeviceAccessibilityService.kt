package com.wireless.control.device.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 无障碍服务
 */
class DeviceAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "DeviceAccessibilityService"
        private const val NOTIFICATION_ID = 1005
        private const val CHANNEL_ID = "accessibility_channel"
        private const val CHANNEL_NAME = "Accessibility Service"
    }

    private lateinit var coroutineScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        coroutineScope = CoroutineScope(Dispatchers.Main)
        createNotificationChannel()
        Log.i(TAG, "DeviceAccessibilityService created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "AccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 处理无障碍事件
        Log.d(TAG, "AccessibilityEvent: ${event.eventType}")
    }

    override fun onInterrupt() {
        Log.i(TAG, "AccessibilityService interrupted")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        coroutineScope.launch {
            while (true) {
                try {
                    delay(10000)
                    Log.d(TAG, "DeviceAccessibilityService running")
                } catch (e: Exception) {
                    Log.e(TAG, "Service error", e)
                }
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("无线群控")
            .setContentText("无障碍服务运行中")
            .setSmallIcon(android.R.drawable.ic_menu_info)
            .setPriority(android.app.Notification.PRIORITY_LOW)
            .build()
    }
}

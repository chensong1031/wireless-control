package com.wireless.control.device.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.wireless.control.device.utils.HttpClient
import org.json.JSONObject

/**
 * 通知监听服务
 *
 * 功能：
 * 1. 监听所有应用的通知
 * 2. 提取通知内容
 * 3. 上报通知到服务器
 */
class NotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        private var instance: NotificationListenerService? = null
        private var serverUrl = "http://192.168.1.1:5000"

        fun getInstance(): NotificationListenerService? = instance

        fun setServerUrl(url: String) {
            serverUrl = url
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "NotificationListenerService started")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected")

        // 上报现有通知
        val notifications = activeNotifications
        Log.d(TAG, "Current notifications count: ${notifications.size}")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) {
            Log.w(TAG, "StatusBarNotification is null")
            return
        }

        try {
            val notification = extractNotificationInfo(sbn)
            Log.d(TAG, "Notification posted: $notification")

            // 上报到服务器
            reportNotification(notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process notification", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)

        if (sbn == null) {
            Log.w(TAG, "StatusBarNotification is null")
            return
        }

        try {
            val packageName = sbn.packageName
            Log.d(TAG, "Notification removed: $packageName")

            // 上报通知移除事件
            reportNotificationRemoved(packageName, sbn.key)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process notification removal", e)
        }
    }

    /**
     * 提取通知信息
     */
    private fun extractNotificationInfo(sbn: StatusBarNotification): JSONObject {
        val notification = sbn.notification
        val extras = notification.extras

        val info = JSONObject().apply {
            put("package", sbn.packageName)
            put("key", sbn.key)
            put("post_time", sbn.postTime)
            put("id", sbn.id)
            put("tag", sbn.tag ?: "")

            // 提取通知内容
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""

            put("title", title)
            put("text", text)
            put("big_text", bigText)
            put("sub_text", subText)
            put("info_text", infoText)

            // 提取应用名称
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                sbn.packageName
            }
            put("app_name", appName)

            // 提取通知类别
            val category = notification.category ?: ""
            put("category", category)

            // 检查是否正在播放媒体
            val isMedia = extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
            put("is_media", isMedia)

            // 提取进度信息
            if (extras.containsKey(Notification.EXTRA_PROGRESS)) {
                val progress = extras.getInt(Notification.EXTRA_PROGRESS, 0)
                val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 100)
                put("progress", progress)
                put("progress_max", max)
            }

            // 提取通知优先级
            put("priority", notification.priority)
            put("flags", notification.flags)

            // 检查是否为群组通知
            val isGroup = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
            put("is_group", isGroup)
            put("group_key", notification.groupKey ?: "")
        }

        return info
    }

    /**
     * 上报通知到服务器
     */
    private fun reportNotification(notification: JSONObject) {
        Thread {
            try {
                val url = "$serverUrl/api/device/notification"
                val response = HttpClient.post(url, notification.toString())

                if (response != null) {
                    Log.d(TAG, "Notification reported successfully")
                } else {
                    Log.w(TAG, "Failed to report notification")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report notification", e)
            }
        }.start()
    }

    /**
     * 上报通知移除事件
     */
    private fun reportNotificationRemoved(packageName: String, key: String) {
        Thread {
            try {
                val data = JSONObject().apply {
                    put("package", packageName)
                    put("key", key)
                    put("action", "removed")
                    put("timestamp", System.currentTimeMillis())
                }

                val url = "$serverUrl/api/device/notification_removed"
                val response = HttpClient.post(url, data.toString())

                if (response != null) {
                    Log.d(TAG, "Notification removal reported successfully")
                } else {
                    Log.w(TAG, "Failed to report notification removal")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report notification removal", e)
            }
        }.start()
    }

    /**
     * 获取所有活动通知
     */
    fun getActiveNotifications(): List<JSONObject> {
        val notifications = mutableListOf<JSONObject>()

        try {
            activeNotifications.forEach { sbn ->
                notifications.add(extractNotificationInfo(sbn))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active notifications", e)
        }

        return notifications
    }

    /**
     * 取消指定通知
     */
    fun cancelNotification(key: String): Boolean {
        return try {
            val notifications = activeNotifications
            val target = notifications.find { it.key == key }

            if (target != null) {
                cancelNotification(target.packageName, target.tag, target.id)
                Log.d(TAG, "Notification cancelled: $key")
                true
            } else {
                Log.w(TAG, "Notification not found: $key")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel notification", e)
            false
        }
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications(): Boolean {
        return try {
            cancelAllNotifications()
            Log.d(TAG, "All notifications cancelled")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel all notifications", e)
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "NotificationListenerService destroyed")
    }
}

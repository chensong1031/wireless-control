package com.wireless.control.device.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * 心跳服务
 *
 * 功能：
 * 1. 定期向主控服务器发送心跳
 * 2. 上报设备状态
 * 3. 保持连接活跃
 * 4. 接收服务器指令
 */
class HeartbeatService : LifecycleService() {

    companion object {
        private const val TAG = "HeartbeatService"
        private const val HEARTBEAT_INTERVAL = 30000L // 30秒
        private const val MASTER_SERVER_IP = "192.168.1.1"
        private const val MASTER_SERVER_PORT = 5000
    }

    private var isRunning = false
    private var heartbeatJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HeartbeatService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "HeartbeatService started")

        if (!isRunning) {
            isRunning = true
            startHeartbeat()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HeartbeatService destroyed")
        isRunning = false
        heartbeatJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * 启动心跳
     */
    private fun startHeartbeat() {
        heartbeatJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isRunning) {
                try {
                    sendHeartbeat()
                    delay(HEARTBEAT_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in heartbeat loop", e)
                    delay(5000) // 出错后等待5秒
                }
            }
        }

        Log.d(TAG, "Heartbeat loop started")
    }

    /**
     * 发送心跳
     */
    private fun sendHeartbeat() {
        try {
            val deviceId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            val heartbeatData = JSONObject()
            heartbeatData.put("device_id", deviceId)
            heartbeatData.put("status", "online")
            heartbeatData.put("manufacturer", android.os.Build.MANUFACTURER)
            heartbeatData.put("model", android.os.Build.MODEL)
            heartbeatData.put("android_version", android.os.Build.VERSION.RELEASE)
            heartbeatData.put("api_level", android.os.Build.VERSION.SDK_INT)
            heartbeatData.put("ip", getLocalIpAddress())
            heartbeatData.put("port", 8080)
            heartbeatData.put("battery_level", getBatteryLevel())
            heartbeatData.put("is_charging", isCharging())
            heartbeatData.put("network_type", getNetworkType())
            heartbeatData.put("timestamp", System.currentTimeMillis())

            // 发送POST请求
            val url = URL("http://$MASTER_SERVER_IP:$MASTER_SERVER_PORT/api/devices/heartbeat")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(heartbeatData.toString())
            writer.flush()

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readLine()
                reader.close()

                Log.d(TAG, "Heartbeat response: $response")

                // 处理服务器返回的指令
                processServerResponse(JSONObject(response))

            } else {
                Log.w(TAG, "Heartbeat failed: $responseCode")
            }

            conn.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send heartbeat", e)
        }
    }

    /**
     * 处理服务器响应
     */
    private fun processServerResponse(response: JSONObject) {
        try {
            val command = response.optString("command", "")

            if (command.isNotEmpty()) {
                Log.d(TAG, "Received command from server: $command")

                when (command) {
                    "restart" -> {
                        Log.i(TAG, "Restart command received, restarting app...")
                        restartApp()
                    }
                    "reboot" -> {
                        Log.i(TAG, "Reboot command received, rebooting device...")
                        rebootDevice()
                    }
                    "update" -> {
                        val version = response.optString("version", "")
                        Log.i(TAG, "Update command received for version: $version")
                        // TODO: 实现自动更新
                    }
                    else -> {
                        Log.w(TAG, "Unknown command: $command")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process server response", e)
        }
    }

    /**
     * 重启应用
     */
    private fun restartApp() {
        try {
            val packageManager = packageManager
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

            android.os.Process.killProcess(android.os.Process.myPid())

        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart app", e)
        }
    }

    /**
     * 重启设备
     */
    private fun rebootDevice() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
            powerManager.reboot(null)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to reboot device", e)
        }
    }

    /**
     * 获取电池电量
     */
    private fun getBatteryLevel(): Int {
        val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /**
     * 是否在充电
     */
    private fun isCharging(): Boolean {
        val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS) ==
                android.os.BatteryManager.BATTERY_STATUS_CHARGING
    }

    /**
     * 获取网络类型
     */
    private fun getNetworkType(): String {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo ?: return "none"
        return when (activeNetwork.type) {
            android.net.ConnectivityManager.TYPE_WIFI -> "wifi"
            android.net.ConnectivityManager.TYPE_MOBILE -> "mobile"
            else -> "unknown"
        }
    }

    /**
     * 获取本地IP地址
     */
    private fun getLocalIpAddress(): String {
        try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
            val connectionInfo = wifiManager.connectionInfo
            val ipAddress = android.net.InetAddress.getByAddress(connectionInfo.ipAddress).hostAddress
            return ipAddress
        } catch (e: Exception) {
            return "0.0.0.0"
        }
    }
}

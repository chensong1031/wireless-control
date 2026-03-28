package com.wireless.control.device.service

import android.app.Service
import android.app.usage.UsageStats
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import android.util.Log
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
 * 设备监控服务
 *
 * 功能：
 * 1. 监控应用使用情况
 * 2. 监控系统性能
 * 3. 监控电池状态
 * 4. 定期上报到主控服务器
 */
class DeviceMonitorService : LifecycleService() {

    companion object {
        private const val TAG = "DeviceMonitorService"
        private const val MONITOR_INTERVAL = 60000L // 1分钟
        private const val MASTER_SERVER_IP = "192.168.1.1"
        private const val MASTER_SERVER_PORT = 5000
    }

    private var isRunning = false
    private var monitorJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DeviceMonitorService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "DeviceMonitorService started")

        if (!isRunning) {
            isRunning = true
            startMonitoring()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DeviceMonitorService destroyed")
        isRunning = false
        monitorJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * 启动监控
     */
    private fun startMonitoring() {
        monitorJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isRunning) {
                try {
                    collectDeviceStatus()
                    delay(MONITOR_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitor loop", e)
                    delay(10000)
                }
            }
        }

        Log.d(TAG, "Monitor loop started")
    }

    /**
     * 收集设备状态
     */
    private fun collectDeviceStatus() {
        try {
            val deviceId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            val deviceStatus = JSONObject()
            deviceStatus.put("device_id", deviceId)
            deviceStatus.put("timestamp", System.currentTimeMillis())

            // 电池状态
            val battery = JSONObject()
            battery.put("level", getBatteryLevel())
            battery.put("is_charging", isCharging())
            battery.put("voltage", getBatteryVoltage())
            battery.put("temperature", getBatteryTemperature())
            deviceStatus.put("battery", battery)

            // CPU使用率
            val cpu = JSONObject()
            cpu.put("usage", getCpuUsage())
            cpu.put("temperature", getCpuTemperature())
            cpu.put("frequency", getCpuFrequency())
            deviceStatus.put("cpu", cpu)

            // 内存状态
            val memory = JSONObject()
            memory.put("total", getTotalMemory())
            memory.put("available", getAvailableMemory())
            memory.put("usage_percent", getMemoryUsagePercent())
            deviceStatus.put("memory", memory)

            // 存储状态
            val storage = JSONObject()
            storage.put("total", getTotalStorage())
            storage.put("available", getAvailableStorage())
            storage.put("usage_percent", getStorageUsagePercent())
            deviceStatus.put("storage", storage)

            // 网络状态
            val network = JSONObject()
            network.put("type", getNetworkType())
            network.put("wifi_enabled", isWifiEnabled())
            network.put("wifi_ssid", getWifiSSID())
            network.put("signal_strength", getWifiSignalStrength())
            network.put("mobile_data_enabled", isMobileDataEnabled())
            network.put("rx_bytes", getNetworkRxBytes())
            network.put("tx_bytes", getNetworkTxBytes())
            deviceStatus.put("network", network)

            // 前台应用列表
            val apps = getForegroundApps()
            deviceStatus.put("apps", apps)

            // 上报到服务器
            reportToServer(deviceStatus)

            Log.d(TAG, "Device status collected and reported")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to collect device status", e)
        }
    }

    /**
     * 上报到服务器
     */
    private fun reportToServer(data: JSONObject) {
        try {
            val url = URL("http://$MASTER_SERVER_IP:$MASTER_SERVER_PORT/api/devices/status")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(data.toString())
            writer.flush()

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Status reported successfully")
            } else {
                Log.w(TAG, "Status report failed: $responseCode")
            }

            conn.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to report status to server", e)
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
     * 获取电池电压
     */
    private fun getBatteryVoltage(): Int {
        val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_VOLTAGE)
    }

    /**
     * 获取电池温度
     */
    private fun getBatteryTemperature(): Float {
        val batteryManager = getSystemService(BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_TEMPERATURE) / 10.0f
    }

    /**
     * 获取CPU使用率
     */
    private fun getCpuUsage(): Int {
        // 读取 /proc/stat 计算CPU使用率
        return try {
            val reader = BufferedReader(java.io.FileReader("/proc/stat"))
            val line1 = reader.readLine()
            reader.close()

            val parts = line1.split("\\s+".toTypedArray()
            val idle = parts[4].toLong()
            val total = parts.slice(1).sumOf { it.toLong() }

            val usage = ((total - idle) * 100) / total
            usage.toInt()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get CPU usage", e)
            0
        }
    }

    /**
     * 获取CPU温度
     */
    private fun getCpuTemperature(): Int {
        return try {
            val reader = BufferedReader(java.io.FileReader("/sys/class/thermal/thermal_zone0/temp"))
            val tempStr = reader.readLine()
            reader.close()
            (tempStr.toInt() / 1000)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get CPU temperature", e)
            0
        }
    }

    /**
     * 获取CPU频率
     */
    private fun getCpuFrequency(): Int {
        return try {
            val reader = BufferedReader(java.io.FileReader("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"))
            val freqStr = reader.readLine()
            reader.close()
            (freqStr.toInt() / 1000000) // 转换为MHz

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get CPU frequency", e)
            0
        }
    }

    /**
     * 获取总内存
     */
    private fun getTotalMemory(): Long {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem / (1024 * 1024) // MB
    }

    /**
     * 获取可用内存
     */
    private fun getAvailableMemory(): Long {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024 * 1024) // MB
    }

    /**
     * 获取内存使用百分比
     */
    private fun getMemoryUsagePercent(): Int {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return ((memInfo.totalMem - memInfo.availMem) * 100 / memInfo.totalMem).toInt()
    }

    /**
     * 获取总存储空间
     */
    private fun getTotalStorage(): Long {
        return try {
            val stat = android.os.StatFs(android.os.Environment.getExternalStorageDirectory().path)
            stat.totalBytes / (1024 * 1024) // MB

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get total storage", e)
            0
        }
    }

    /**
     * 获取可用存储空间
     */
    private fun getAvailableStorage(): Long {
        return try {
            val stat = android.os.StatFs(android.os.Environment.getExternalStorageDirectory().path)
            stat.availableBytes / (1024 * 1024) // MB

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available storage", e)
            0
        }
    }

    /**
     * 获取存储使用百分比
     */
    private fun getStorageUsagePercent(): Int {
        return try {
            val stat = android.os.StatFs(android.os.Environment.getExternalStorageDirectory().path)
            val used = stat.totalBytes - stat.availableBytes
            ((used * 100) / stat.totalBytes).toInt()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get storage usage", e)
            0
        }
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
     * 检查WiFi是否开启
     */
    private fun isWifiEnabled(): Boolean {
        val wifiManager = getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
        return wifiManager.isWifiEnabled
    }

    /**
     * 获取WiFi SSID
     */
    private fun getWifiSSID(): String {
        val wifiManager = getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
        val wifiInfo = wifiManager.connectionInfo
        if (wifiInfo != null && wifiInfo.ssid != null) {
            return String(wifiInfo.ssid)
        }
        return ""
    }

    /**
     * 获取WiFi信号强度
     */
    private fun getWifiSignalStrength(): Int {
        val wifiManager = getSystemService(WIFI_SERVICE) as android.net.wifi.WifiManager
        val wifiInfo = wifiManager.connectionInfo
        return if (wifiInfo != null) {
            val level = android.net.wifi.WifiManager.calculateSignalLevel(wifiInfo.rssi)
            level * 25 // 0-4转换为百分比
        } else {
            0
        }
    }

    /**
     * 检查移动数据是否开启
     */
    private fun isMobileDataEnabled(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkInfo = connectivityManager.getNetworkInfo(android.net.NetworkCapabilities.TYPE_MOBILE)
        return networkInfo != null && networkInfo.isConnected
    }

    /**
     * 获取接收字节数
     */
    private fun getNetworkRxBytes(): Long {
        return try {
            val reader = BufferedReader(java.io.FileReader("/proc/net/dev"))
            var rxBytes = 0L
            reader.lineSequence().filter { it.contains(":") }
                .map { it.split("\\s+") }
                .filter { it[1].contains(":") }
                .forEach { parts ->
                    rxBytes += parts[2].toLong()
                }
            rxBytes / (1024 * 1024) // MB

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Rx bytes", e)
            0
        }
    }

    /**
     * 获取发送字节数
     */
    private fun getNetworkTxBytes(): Long {
        return try {
            val reader = BufferedReader(java.io.FileReader("/proc/net/dev"))
            var txBytes = 0L
            reader.lineSequence().filter { it.contains(":") }
                .map { it.split("\\s+") }
                .filter { it[1].contains(":") }
                .forEach { parts ->
                    txBytes += parts[10].toLong()
                }
            txBytes / (1024 * 1024) // MB

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Tx bytes", e)
            0
        }
    }

    /**
     * 获取前台应用列表
     */
    private fun getForegroundApps(): List<JSONObject> {
        val apps = mutableListOf<JSONObject>()

        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val queryUsageStats = UsageStats.QueryEvents(
                android.app.usage.UsageEvents.queryEvents(
                    android.os.System.currentTimeMillis() - 60000,
                    android.os.System.currentTimeMillis()
                )
            )

            val events = queryUsageStats.query(usageStatsManager)
            for (event in events) {
                val app = JSONObject()
                app.put("package_name", event.packageName)
                app.put("event_type", event.eventType)
                app.put("timeStamp", event.timeStamp)
                apps.add(app)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get foreground apps", e)
        }

        return apps
    }
}

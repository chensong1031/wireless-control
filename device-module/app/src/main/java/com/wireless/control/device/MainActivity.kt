package com.wireless.control.device

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wireless.control.device.server.DeviceControlServer
import com.wireless.control.device.service.DeviceControlService
import com.wireless.control.device.service.DeviceMonitorService
import com.wireless.control.device.service.HeartbeatService
import com.wireless.control.device.service.DeviceAccessibilityService
import com.wireless.control.device.service.NotificationListenerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 设备控制主Activity
 *
 * 功能：
 * 1. 启动HTTP API服务器
 * 2. 启动监控服务
 * 3. 启动心跳服务
 * 4. 注册到主控服务器
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val SERVER_PORT = 8080
        private const val MASTER_SERVER_IP = "192.168.1.1"
        private const val MASTER_SERVER_PORT = 5000
    }

    private var server: DeviceControlServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "MainActivity created")

        // 启动所有服务
        startServer()
        startServices()
        registerToMaster()
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "MainActivity started")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServices()
        Log.d(TAG, "MainActivity destroyed")
    }

    /**
     * 启动HTTP服务器
     */
    private fun startServer() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                server = DeviceControlServer(SERVER_PORT)
                val success = server?.start()

                if (success == true) {
                    Log.i(TAG, "HTTP server started on port $SERVER_PORT")
                } else {
                    Log.e(TAG, "Failed to start HTTP server")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error starting server", e)
            }
        }
    }

    /**
     * 启动所有服务
     */
    private fun startServices() {
        // 启动设备控制服务
        val controlService = Intent(this, DeviceControlService::class.java)
        startService(controlService)
        Log.d(TAG, "DeviceControlService started")

        // 启动设备监控服务
        val monitorService = Intent(this, DeviceMonitorService::class.java)
        startService(monitorService)
        Log.d(TAG, "DeviceMonitorService started")

        // 启动心跳服务
        val heartbeatService = Intent(this, HeartbeatService::class.java)
        startService(heartbeatService)
        Log.d(TAG, "HeartbeatService started")

        // 检查和引导开启无障碍服务
        checkAndRequestAccessibilityService()

        // 检查和引导开启通知监听服务
        checkAndRequestNotificationListenerService()
    }

    /**
     * 停止所有服务
     */
    private fun stopServices() {
        // 停止HTTP服务器
        server?.stop()

        // 停止服务
        val controlService = Intent(this, DeviceControlService::class.java)
        stopService(controlService)

        val monitorService = Intent(this, DeviceMonitorService::class.java)
        stopService(monitorService)

        val heartbeatService = Intent(this, HeartbeatService::class.java)
        stopService(heartbeatService)

        Log.d(TAG, "All services stopped")
    }

    /**
     * 注册到主控服务器
     */
    private fun registerToMaster() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val deviceId = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )

                val deviceInfo = """
                    {
                        "device_id": "$deviceId",
                        "manufacturer": "${android.os.Build.MANUFACTURER}",
                        "model": "${android.os.Build.MODEL}",
                        "android_version": "${android.os.Build.VERSION.RELEASE}",
                        "api_level": ${android.os.Build.VERSION.SDK_INT},
                        "ip": "${getLocalIpAddress()}",
                        "port": $SERVER_PORT,
                        "status": "online",
                        "timestamp": ${System.currentTimeMillis()}
                    }
                """.trimIndent()

                // TODO: 通过HTTP POST到主控服务器
                Log.d(TAG, "Registering to master server: $MASTER_SERVER_IP:$MASTER_SERVER_PORT")
                Log.d(TAG, "Device info: $deviceInfo")

                delay(2000)

                Log.i(TAG, "Device registered successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to register to master server", e)
            }
        }
    }

    /**
     * 获取本地IP地址
     */
    private fun getLocalIpAddress(): String {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            val connectionInfo = wifiManager.connectionInfo
            val ipAddress = android.net.InetAddress.getByAddress(connectionInfo.ipAddress).hostAddress

            // 获取WiFi配置
            val dhcpInfo = wifiManager.dhcpInfo
            if (dhcpInfo != null) {
                val gateway = dhcpInfo.gateway
                val netmask = dhcpInfo.netmask
                val server = dhcpInfo.serverAddress
                Log.d(TAG, "WiFi info - IP: $ipAddress, Gateway: $gateway, Netmask: $netmask, Server: $server")
            }

            return ipAddress

        } catch (e: Exception) {
            Log.e(TAG, "Failed to get IP address", e)
            return "0.0.0.0"
        }
    }

    /**
     * 检查无障碍服务是否开启
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "com.wireless.control.device/com.wireless.control.device.service.DeviceAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        return if (enabledServices != null) {
            val services = enabledServices.split(":")
            services.contains(service)
        } else {
            false
        }
    }

    /**
     * 检查并请求开启无障碍服务
     */
    private fun checkAndRequestAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            Log.w(TAG, "AccessibilityService is not enabled")
            Toast.makeText(
                this,
                "请开启无障碍服务以支持设备控制功能",
                Toast.LENGTH_LONG
            ).show()

            // 延迟引导用户
            lifecycleScope.launch(Dispatchers.Main) {
                delay(3000)
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open accessibility settings", e)
                    Toast.makeText(this, "请在设置中手动开启无障碍服务", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Log.d(TAG, "AccessibilityService is enabled")
        }
    }

    /**
     * 检查通知监听服务是否开启
     */
    private fun isNotificationListenerServiceEnabled(): Boolean {
        val packageName = packageName
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )

        return if (!TextUtils.isEmpty(enabledServices)) {
            enabledServices!!.contains(packageName)
        } else {
            false
        }
    }

    /**
     * 检查并请求开启通知监听服务
     */
    private fun checkAndRequestNotificationListenerService() {
        if (!isNotificationListenerServiceEnabled()) {
            Log.w(TAG, "NotificationListenerService is not enabled")
            Toast.makeText(
                this,
                "请开启通知监听服务以支持通知上报功能",
                Toast.LENGTH_LONG
            ).show()

            // 延迟引导用户
            lifecycleScope.launch(Dispatchers.Main) {
                delay(5000)
                try {
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open notification listener settings", e)
                    Toast.makeText(this, "请在设置中手动开启通知监听服务", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Log.d(TAG, "NotificationListenerService is enabled")
        }
    }
}

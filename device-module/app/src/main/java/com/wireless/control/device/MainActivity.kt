package com.wireless.control.device

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
 */

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private var instance: MainActivity? = null
        
        fun getInstance(): MainActivity? {
            return instance
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        try {
            setContentView(R.layout.activity_main)
            Log.i(TAG, "MainActivity created")
            startServices()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MainActivity", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    private fun startServices() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // 启动设备控制服务
                val controlIntent = Intent(this@MainActivity, DeviceControlService::class.java)
                startForegroundService(controlIntent)
                Log.i(TAG, "Device control service started")

                // 启动监控服务
                val monitorIntent = Intent(this@MainActivity, DeviceMonitorService::class.java)
                startForegroundService(monitorIntent)
                Log.i(TAG, "Device monitor service started")

                // 启动心跳服务
                val heartbeatIntent = Intent(this@MainActivity, HeartbeatService::class.java)
                startForegroundService(heartbeatIntent)
                Log.i(TAG, "Heartbeat service started")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start services", e)
            }
        }
    }
}

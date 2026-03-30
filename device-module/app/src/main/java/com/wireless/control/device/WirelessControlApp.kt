package com.wireless.control.device

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import com.wireless.control.device.server.DeviceControlServer

/**
 * 应用类 - 用于启动HTTP服务器
 */
class WirelessControlApp : Application() {

    companion object {
        private const val TAG = "WirelessControlApp"
        private lateinit var instance: WirelessControlApp
        private var server: DeviceControlServer? = null
        
        fun getInstance(): WirelessControlApp = instance
        fun getServer(): DeviceControlServer? = server
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 启动HTTP服务器（添加异常处理防止闪退）
        try {
            startServer()
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to start server", e)
        }
        
        Log.i(TAG, "✓ Application initialized")
    }

    private fun startServer() {
        try {
            server = DeviceControlServer(this, 8080)
            val started = server?.start() ?: false
            
            if (started) {
                Log.i(TAG, "✓ HTTP server started on port 8080")
            } else {
                Log.w(TAG, "⚠ HTTP server failed to start")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Exception starting server", e)
            // 不抛出异常，让应用继续运行
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        try {
            server?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error stopping server", e)
        }
        Log.i(TAG, "✓ Application terminated")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "⚠ Low memory warning")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "Memory trimmed: level=$level")
    }
}

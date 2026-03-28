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
        
        // 启动HTTP服务器
        startServer()
        
        Log.i(TAG, "✓ Application initialized")
    }

    private fun startServer() {
        try {
            server = DeviceControlServer(this, 8080)
            val success = server?.start() ?: false
            
            if (success) {
                Log.i(TAG, "✓ HTTP server started on port 8080")
                Log.i(TAG, "  API endpoints:")
                Log.i(TAG, "  - GET  http://localhost:8080/status")
                Log.i(TAG, "  - GET  http://localhost:8080/wechat/info")
                Log.i(TAG, "  - GET  http://localhost:8080/chats")
                Log.i(TAG, "  - POST http://localhost:8080/message/send")
                Log.i(TAG, "  - GET  http://localhost:8080/logs")
            } else {
                Log.e(TAG, "✗ Failed to start HTTP server")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Exception starting server", e)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        server?.stop()
        Log.i(TAG, "✓ Server stopped")
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

package com.wireless.control.device

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.wireless.control.device.server.DeviceControlServer
import java.util.concurrent.Executors

/**
 * 应用类 - 用于启动HTTP服务器
 */
class WirelessControlApp : Application() {

    companion object {
        private const val TAG = "WirelessControlApp"
        private lateinit var instance: WirelessControlApp
        private var server: DeviceControlServer? = null
        private val executor = Executors.newSingleThreadExecutor()
        
        fun getInstance(): WirelessControlApp = instance
        fun getServer(): DeviceControlServer? = server
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        Log.i(TAG, "✓ Application starting...")
        
        // 延迟2秒后启动服务器，避免阻塞主线程
        Handler(Looper.getMainLooper()).postDelayed({
            startServerAsync()
        }, 2000)
        
        Log.i(TAG, "✓ Application initialized")
    }

    private fun startServerAsync() {
        // 在后台线程启动服务器
        executor.execute {
            try {
                Thread.sleep(500) // 额外延迟
                server = DeviceControlServer(this@WirelessControlApp, 8080)
                val started = server?.start() ?: false
                
                if (started) {
                    Log.i(TAG, "✓ HTTP server started on port 8080")
                } else {
                    Log.w(TAG, "⚠ HTTP server failed to start (returned false)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Exception starting server", e)
                // 不抛出异常，让应用继续运行
            }
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
package com.wireless.control.device.server

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD

/**
 * HTTP API 服务器
 * 
 * 功能：
 * 1. 暴露 HTTP 接口供远程控制
 * 2. 处理设备命令
 * 3. 返回设备状态
 */
class DeviceControlServer(private val port: Int) {

    companion object {
        private const val TAG = "DeviceControlServer"
        private var instance: DeviceControlServer? = null
    }

    private var server: NanoHTTPD? = null

    /**
     * 启动服务器
     */
    fun start(): Boolean {
        try {
            server = object : NanoHTTPD(port) {
                override fun serve(session: IHTTPSession): Response {
                    return newFixedLengthResponse(
                        Response.Status.OK,
                        "application/json",
                        "{status: 'running'}"
                    )
                }
            }
            server?.start()
            Log.i(TAG, "Server started on port $port")
            instance = this
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            return false
        }
    }

    /**
     * 停止服务器
     */
    fun stop() {
        try {
            server?.stop()
            Log.i(TAG, "Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop server", e)
        }
    }

    companion object {
        fun getInstance(): DeviceControlServer? {
            return instance
        }
    }
}

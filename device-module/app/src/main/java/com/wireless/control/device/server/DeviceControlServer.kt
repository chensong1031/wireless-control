package com.wireless.control.device.server

import android.util.Log
import fi.iki.elonen.NanoHTTPD

/**
 * HTTP API 服务器
 */
class DeviceControlServer(private val port: Int) {

    companion object {
        private const val TAG = "DeviceControlServer"
        private var instance: DeviceControlServer? = null
        
        fun getInstance(): DeviceControlServer? {
            return instance
        }
    }

    private var server: NanoHTTPD? = null

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

    fun stop() {
        try {
            server?.stop()
            Log.i(TAG, "Server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop server", e)
        }
    }
}

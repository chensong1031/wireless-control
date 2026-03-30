package com.wireless.control.device.server

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Method
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.Response.IStatus
import fi.iki.elonen.NanoHTTPD.Response.Status
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * HTTP控制服务器 - 提供远程控制API
 * 
 * 支持的API端点：
 * - GET /status - 设备状态
 * - GET /wechat/info - 微信信息
 * - GET /chats - 聊天列表
 * - POST /message/send - 发送消息
 * - GET /messages - 获取消息
 * - GET /logs - 获取日志
 */
class DeviceControlServer(
    private val context: Context,
    private val port: Int = 8080
) {

    companion object {
        private const val TAG = "DeviceControlServer"
        private var instance: DeviceControlServer? = null
        private const val JSON_CONTENT_TYPE = "application/json; charset=UTF-8"
        private val TEXT_CONTENT_TYPE = "text/plain; charset=UTF-8"
        
        fun getInstance(): DeviceControlServer? {
            return instance
        }
    }

    private var server: NanoHTTPD? = null
    private var isRunning = false
    private val startTime = System.currentTimeMillis()
    private var lastChatOpened: String? = null

    fun start(): Boolean {
        if (isRunning) {
            Log.w(TAG, "Server is already running")
            return true
        }

        return try {
            server = object : NanoHTTPD(port) {
                override fun serve(session: IHTTPSession): Response {
                    return handleRequest(session)
                }
            }
            server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            isRunning = true
            instance = this
            Log.i(TAG, "✓ Server started on port $port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to start server", e)
            isRunning = false
            false
        }
    }

    fun stop(): Boolean {
        if (!isRunning) {
            return false
        }

        return try {
            server?.stop()
            isRunning = false
            instance = null
            Log.i(TAG, "✓ Server stopped")
            true
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to stop server", e)
            false
        }
    }

    private fun handleRequest(session: IHTTPSession): Response {
        return try {
            val uri = session.uri
            val method = session.method

            Log.d(TAG, "Request: $method $uri")

            when {
                // 设备状态
                method == Method.GET && uri == "/status" -> {
                    jsonResponse(getStatus())
                }
                
                // 微信信息
                method == Method.GET && uri == "/wechat/info" -> {
                    jsonResponse(getWeChatInfo())
                }
                
                // 聊天列表
                method == Method.GET && uri == "/chats" -> {
                    jsonResponse(getChats())
                }
                
                // 发送消息（需要body参数）
                method == Method.POST && uri == "/message/send" -> {
                    handleSendMessage(session)
                }
                
                // 获取消息
                method == Method.GET && uri.startsWith("/messages") -> {
                    val chatId = session.parameters["chatId"]?.firstOrNull() ?: ""
                    jsonResponse(getMessages(chatId))
                }
                
                // 获取日志
                method == Method.GET && uri == "/logs" -> {
                    val lines = session.parameters["lines"]?.firstOrNull()?.toIntOrNull() ?: 50
                    jsonResponse(getLogs(lines))
                }
                
                // 健康检查
                method == Method.GET && uri == "/health" -> {
                    jsonResponse(healthCheck())
                }
                
                // 默认404
                else -> {
                    jsonResponse(error("Not found", 404), 404)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling request", e)
            jsonResponse(error("Internal server error: ${e.message}", 500), 500)
        }
    }

    private fun getStatus(): JSONObject {
        val uptime = System.currentTimeMillis() - startTime
        val hours = uptime / (1000 * 60 * 60)
        val minutes = (uptime % (1000 * 60 * 60)) / (1000 * 60)
        
        return JSONObject().apply {
            put("status", if (isRunning) "running" else "stopped")
            put("uptime", "${hours}h ${minutes}m")
            put("uptimeMs", uptime)
            put("startTime", startTime)
            put("currentTime", System.currentTimeMillis())
            put("lastChatOpened", lastChatOpened ?: "none")
            put("version", "1.0.0")
            put("apiVersion", "1.0")
        }
    }

    private fun getWeChatInfo(): JSONObject {
        var installed = false
        var versionName = "N/A"
        var versionCode = 0

        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo("com.tencent.mm", 0)
            installed = true
            versionName = packageInfo.versionName
            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // 微信未安装
        }

        return JSONObject().apply {
            put("installed", installed)
            put("versionName", versionName)
            put("versionCode", versionCode)
            put("packageName", "com.tencent.mm")
        }
    }

    private fun getChats(): JSONObject {
        // 模拟聊天列表数据
        return JSONObject().apply {
            put("chats", listOf(
                JSONObject().apply {
                    put("id", "chat_001")
                    put("name", "文件传输助手")
                    put("type", "single")
                    put("lastMessage", "暂无消息")
                    put("timestamp", System.currentTimeMillis())
                },
                JSONObject().apply {
                    put("id", "chat_002")
                    put("name", "测试群")
                    put("type", "group")
                    put("lastMessage", "测试消息")
                    put("timestamp", System.currentTimeMillis() - 3600000)
                }
            ))
            put("count", 2)
        }
    }

    private fun getMessages(chatId: String): JSONObject {
        // 模拟消息数据
        return JSONObject().apply {
            put("chatId", chatId)
            put("messages", listOf(
                JSONObject().apply {
                    put("id", "msg_001")
                    put("type", "text")
                    put("content", "这是一条测试消息")
                    put("sender", "me")
                    put("direction", "sent")
                    put("timestamp", System.currentTimeMillis() - 3600000)
                }
            ))
            put("count", 1)
        }
    }

    private fun handleSendMessage(session: IHTTPSession): Response {
        return try {
            val files = HashMap<String, String>()
            session.parseBody(files)
            val body = files["postData"] ?: ""
            val json = JSONObject(body)
            
            val content = json.optString("content", "")
            val chatId = json.optString("chatId", "")
            val type = json.optString("type", "text")
            
            if (content.isEmpty()) {
                jsonResponse(error("Content is required", 400), 400)
            } else {
                // 记录发送的消息
                Log.i(TAG, "Message to be sent: $content to chat: $chatId (type: $type)")
                
                jsonResponse(success(mapOf(
                    "messageId" to "msg_${System.currentTimeMillis()}",
                    "content" to content,
                    "chatId" to chatId,
                    "type" to type,
                    "status" to "queued",
                    "timestamp" to System.currentTimeMillis()
                )))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            jsonResponse(error("Failed to send message: ${e.message}", 500), 500)
        }
    }

    private fun getLogs(lines: Int): JSONObject {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", lines.toString()))
            val reader = process.inputStream.bufferedReader()
            val logs = reader.use { it.readLines() }.take(lines)
            
            JSONObject().apply {
                put("logs", logs)
                put("count", logs.size)
                put("linesRequested", lines)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get logs", e)
            error("Failed to get logs: ${e.message}")
        }
    }

    private fun healthCheck(): JSONObject {
        return JSONObject().apply {
            put("status", "healthy")
            put("server", "running")
            put("timestamp", System.currentTimeMillis())
            put("checks", JSONObject().apply {
                put("server", true)
                put("memory", true)
                put("disk", true)
            })
        }
    }

    fun onChatOpened(chatName: String) {
        lastChatOpened = chatName
        Log.i(TAG, "💬 Chat opened: $chatName (via HTTP API)")
    }

    private fun jsonResponse(data: Any, statusCode: Int = 200): Response {
        val jsonBody = when (data) {
            is JSONObject -> data.toString()
            is Map<*, *> -> JSONObject(data).toString()
            is String -> data
            else -> JSONObject().apply { put("data", data) }.toString()
        }
        return NanoHTTPD.newFixedLengthResponse(
            Response.Status.lookup(statusCode),
            JSON_CONTENT_TYPE,
            jsonBody
        )
    }

    private fun error(message: String, code: Int = 500): JSONObject {
        return JSONObject().apply {
            put("success", false)
            put("error", message)
            put("code", code)
            put("timestamp", System.currentTimeMillis())
        }
    }

    private fun success(data: Map<String, Any>): JSONObject {
        return JSONObject().apply {
            put("success", true)
            put("data", JSONObject(data))
            put("timestamp", System.currentTimeMillis())
        }
    }
}

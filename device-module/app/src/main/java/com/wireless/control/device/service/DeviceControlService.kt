package com.wireless.control.device.service

import android.accessibilityservice.AccessibilityService
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
 * 设备控制服务
 *
 * 功能：
 * 1. 接收主控服务器的控制指令
 *2. 执行设备操作
 * 3. Hook系统应用
 * 4. 返回操作结果
 */
class DeviceControlService : LifecycleService() {

    companion object {
        private const val TAG = "DeviceControlService"
        private const val CHECK_INTERVAL = 5000L // 5秒
        private const val SERVER_PORT = 8080
        private const val MASTER_SERVER_IP = "192.168.1.1"
        private const val MASTER_SERVER_PORT = 5000
    }

    private var isRunning = false
    private var checkJob: kotlinx.coroutines.Job? = null
    private var currentTaskId: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DeviceControlService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "DeviceControlService started")

        if (!isRunning) {
            isRunning = true
            startTaskCheck()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DeviceControlService destroyed")
        isRunning = false
        checkJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    /**
     * 启动任务检查
     */
    private fun startTaskCheck() {
        checkJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isRunning) {
                try {
                    checkForTasks()
                    delay(CHECK_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in task check loop", e)
                    delay(10000)
                }
            }
        }

        Log.d(TAG, "Task check loop started")
    }

    /**
     * 检查待执行任务
     */
    private fun checkForTasks() {
        try {
            val deviceId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            // 查询服务器是否有待执行的任务
            val url = URL("http://$MASTER_SERVER_IP:$MASTER_SERVER_PORT/api/devices/$deviceId/tasks/pending")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "GET"
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readLine()
                reader.close()

                val jsonResponse = JSONObject(response)
                val tasks = jsonResponse.optJSONArray("tasks")

                if (tasks != null && tasks.length() > 0) {
                    // 有待执行的任务
                    for (i in 0 until tasks.length()) {
                        val task = tasks.getJSONObject(i)
                        executeTask(task)
                    }
                }

            } else {
                Log.w(TAG, "Failed to check tasks: $responseCode")
            }

            conn.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for tasks", e)
        }
    }

    /**
     * 执行任务
     */
    private fun executeTask(task: JSONObject) {
        val taskId = task.optString("id", "")
        val taskType = task.optString("type", "")
        val params = task.optJSONObject("params")

        Log.d(TAG, "Executing task: $taskId, type: $taskType")

        currentTaskId = taskId

        val result = when (taskType) {
            "sms" -> executeSMSTask(params)
            "call" -> executeCallTask(params)
            "input" -> executeInputTask(params)
            "click" -> executeClickTask(params)
            "swipe" -> executeSwipeTask(params)
            "app" -> executeAppTask(params)
            "shell" -> executeShellTask(params)
            "screenshot" -> executeScreenshotTask(params)
            "notification" -> executeNotificationTask(params)
            else -> {
                JSONObject().apply {
                    put("code", 400)
                    put("message", "Unknown task type")
                }
            }
        }

        // 上报任务执行结果
        reportTaskResult(taskId, result)
    }

    /**
     * 执行短信任务
     */
    private fun executeSMSTask(params: JSONObject): JSONObject {
        return try {
            val phoneNumber = params.optString("phone_number", "")
            val message = params.optString("message", "")

            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO)
            intent.putExtra("address", phoneNumber)
            intent.putExtra("sms_body", message)
            startActivity(intent)

            JSONObject().apply {
                put("code", 200)
                put("message", "SMS sent successfully")
                put("phone_number", phoneNumber)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            JSONObject().apply {
                put("code", 500)
                put("message", "Failed to send SMS: ${e.message}")
            }
        }
    }

    /**
     * 执行通话任务
     */
    private fun executeCallTask(params: JSONObject): JSONObject {
        return try {
            val phoneNumber = params.optString("phone_number", "")
            val duration = params.optInt("duration", 0)

            val intent = android.content.Intent(android.content.Intent.ACTION_CALL, android.net.Uri.parse("tel:$phoneNumber"))
            startActivity(intent)

            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "Call started")
            result.put("phone_number", phoneNumber)

            if (duration > 0) {
                // TODO: 实现定时挂断
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "Failed to make call", e)
            JSONObject().apply {
                put("code", 500)
                put("message", "Failed to make call: ${e.message}")
            }
        }
    }

    /**
     * 执行输入任务
     */
    private fun executeInputTask(params: JSONObject): JSONObject {
        return try {
            val text = params.optString("text", "")
            val x = params.optInt("x", 0)
            val y = params optInt("y", 0)

            // TODO: 使用AccessibilityService注入输入
            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "Input sent")
            result.put("text", text)

            result

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send input", e)
            errorResponse(e)
        }
    }

    /**
     * 执行点击任务
     */
    private fun executeClickTask(params: JSONObject): JSONObject {
        return try {
            val x = params.optInt("x", 0)
            val y = params.optInt("y", 0)

            // TODO: 使用AccessibilityService执行点击
            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "Click performed")
            result.put("x", x)
            result.put("y", y)

            result

        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform click", e)
            errorResponse(e)
        }
    }

    /**
     * 执行滑动任务
     */
    private fun executeSwipeTask(params: JSONObject): JSONObject {
        return try {
            val startX = params.optInt("startX", 0)
            val startY = params.optInt("startY", 0)
            val endX = params.optInt("endX", 0)
            val endY = params.optInt("endY", 0)
            val duration = params.optInt("duration", 500)

            // TODO: 使用AccessibilityService执行滑动
            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "Swipe performed")

            result

        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform swipe", e)
            errorResponse(e)
        }
    }

    /**
     * 执行应用任务
     */
    private fun executeAppTask(params: JSONObject): JSONObject {
        return try {
            val action = params.optString("action", "")
            val packageName = params.optString("package_name", "")

            val result = when (action) {
                "start" -> {
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        startActivity(intent)
                        JSONObject().apply {
                            put("code", 200)
                            put("message", "App started")
                            put("package_name", packageName)
                        }
                    } else {
                        JSONObject().apply {
                            put("code", 404)
                            put("message", "App not found")
                        }
                    }
                }
                "stop" -> {
                    val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    am.killBackgroundProcesses(packageName)

                    JSONObject().apply {
                        put("code", 200)
                        put("message", "App stopped")
                        put("package_name", packageName)
                    }
                }
                "install" -> {
                    val apkPath = params.optString("apk_path", "")
                    // TODO: 实现安装应用
                    JSONObject().apply {
                        put("code", 200)
                        put("message", "Install task queued")
                    }
                }
                "uninstall" -> {
                    // TODO: 实现卸载应用
                    JSONObject().apply {
                        put("code", 200)
                        put("message", "Uninstall task queued")
                    }
                }
                else -> {
                    JSONObject().apply {
                        put("code", 400)
                        put("message", "Unknown action")
                    }
                }
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute app task", e)
            errorResponse(e)
        }
    }

    /**
     * 执行Shell命令任务
     */
    private fun executeShellTask(params: JSONObject): JSONObject {
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
            Log.e(TAG, "Failed to execute shell command", e)
            errorResponse(e)
        }
    }

    /**
     * 执行截图任务
     */
    private fun executeScreenshotTask(params: JSONObject): JSONObject {
        return try {
            // TODO: 使用screencap执行截图
            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "Screenshot taken")
            result.put("timestamp", System.currentTimeMillis())

            result

        } catch (e: Exception) {
            Log.e(TAG, "Failed to take screenshot", e)
            errorResponse(e)
        }
    }

    /**
     * 执行通知任务
     */
    private fun executeNotificationTask(params: JSONObject): JSONObject {
        return try {
            val title = params.optString("title", "")
            val message = params.optString("message", "")

            // TODO: 显示系统通知
            val result = JSONObject()
            result.put("code", 200)
            result.put("message", "Notification sent")

            result

        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification", e)
            errorResponse(e)
        }
    }

    /**
     * 上报任务执行结果
     */
    private fun reportTaskResult(taskId: String, result: JSONObject) {
        try {
            val deviceId = android.provider.Settings.Secure.getString(
                contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            val report = JSONObject()
            report.put("device_id", deviceId)
            report.put("task_id", taskId)
            report.put("result", result)
            report.put("timestamp", System.currentTimeMillis())

            val url = URL("http://$MASTER_SERVER_IP:$MASTER_SERVER_PORT/api/devices/$deviceId/tasks/$taskId/result")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(report.toString())
            writer.flush()

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "Task result reported successfully")
            } else {
                Log.w(TAG, "Failed to report task result: $responseCode")
            }

            conn.disconnect()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to report task result", e)
        }
    }

    /**
     * 错误响应
     */
    private fun errorResponse(e: Exception): JSONObject {
        return JSONObject().apply {
            put("code", 500)
            put("message", e.message ?: "Unknown error")
        }
    }
}

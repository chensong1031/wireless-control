package com.wireless.control.device.utils

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP客户端工具类
 *
 * 功能：
 * 1. GET请求
 * 2. POST请求
 * 3. PUT请求
 * 4. DELETE请求
 * 5. 文件上传
 * 6. 连接池管理
 */
object HttpClient {

    private const val TAG = "HttpClient"

    // JSON媒体类型
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    // OkHttp客户端（单例）
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * GET请求
     */
    fun get(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val requestBuilder = Request.Builder().url(url).get()

            // 添加请求头
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e(TAG, "GET failed: ${response.code} - ${response.message}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "GET request failed", e)
            null
        }
    }

    /**
     * POST请求（字符串）
     */
    fun post(url: String, body: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val requestBody = body.toRequestBody(JSON_MEDIA_TYPE)

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

            // 添加请求头
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e(TAG, "POST failed: ${response.code} - ${response.message}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "POST request failed", e)
            null
        }
    }

    /**
     * POST请求（JSON）
     */
    fun postJson(url: String, json: JSONObject, headers: Map<String, String> = emptyMap()): String? {
        return post(url, json.toString(), headers)
    }

    /**
     * POST请求（表单）
     */
    fun postForm(url: String, formData: Map<String, String>, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val formBodyBuilder = FormBody.Builder()
            formData.forEach { (key, value) ->
                formBodyBuilder.add(key, value)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(formBodyBuilder.build())

            // 添加请求头
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e(TAG, "POST form failed: ${response.code} - ${response.message}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "POST form request failed", e)
            null
        }
    }

    /**
     * PUT请求
     */
    fun put(url: String, body: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val requestBody = body.toRequestBody(JSON_MEDIA_TYPE)

            val requestBuilder = Request.Builder()
                .url(url)
                .put(requestBody)

            // 添加请求头
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e(TAG, "PUT failed: ${response.code} - ${response.message}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "PUT request failed", e)
            null
        }
    }

    /**
     * PUT请求（JSON）
     */
    fun putJson(url: String, json: JSONObject, headers: Map<String, String> = emptyMap()): String? {
        return put(url, json.toString(), headers)
    }

    /**
     * DELETE请求
     */
    fun delete(url: String, headers: Map<String, String> = emptyMap()): String? {
        return try {
            val requestBuilder = Request.Builder().url(url).delete()

            // 添加请求头
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e(TAG, "DELETE failed: ${response.code} - ${response.message}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "DELETE request failed", e)
            null
        }
    }

    /**
     * 上传文件
     */
    fun uploadFile(
        url: String,
        fileBytes: ByteArray,
        filename: String,
        mimeType: String = "application/octet-stream",
        headers: Map<String, String> = emptyMap()
    ): String? {
        return try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    filename,
                    fileBytes.toRequestBody(mimeType.toMediaType())
                )
                .build()

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)

            // 添加请求头
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    Log.e(TAG, "Upload failed: ${response.code} - ${response.message}")
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Upload request failed", e)
            null
        }
    }

    /**
     * 异步GET请求
     */
    fun getAsync(url: String, callback: (String?) -> Unit, headers: Map<String, String> = emptyMap()) {
        Thread {
            val result = get(url, headers)
            callback(result)
        }.start()
    }

    /**
     * 异步POST请求
     */
    fun postAsync(url: String, body: String, callback: (String?) -> Unit, headers: Map<String, String> = emptyMap()) {
        Thread {
            val result = post(url, body, headers)
            callback(result)
        }.start()
    }

    /**
     * 下载文件
     */
    fun downloadFile(url: String, callback: (ByteArray?) -> Unit): String? {
        return try {
            val request = Request.Builder().url(url).get().build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                    Log.d(TAG, "File downloaded successfully: ${response.body?.contentLength()} bytes")
                    response.body?.string()
                } else {
                    Log.e(TAG, "Download failed: ${response.code} - ${response.message}")
                    callback(null)
                    null
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Download request failed", e)
            callback(null)
            null
        }
    }

    /**
     * 获取连接池状态
     */
    fun getConnectionPoolInfo(): String {
        val connectionPool = client.connectionPool
        return JSONObject().apply {
            put("idle_connections", connectionPool.idleConnectionCount())
            put("total_connections", connectionPool.connectionCount())
        }.toString()
    }

    /**
     * 取消所有请求
     */
    fun cancelAllRequests() {
        client.dispatcher.cancelAll()
        Log.d(TAG, "All requests cancelled")
    }
}

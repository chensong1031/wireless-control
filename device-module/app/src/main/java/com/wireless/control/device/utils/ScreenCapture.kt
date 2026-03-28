package com.wireless.control.device.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 截图工具类
 *
 * 功能：
 * 1. 执行screencap命令截图
 * 2. 保存截图到文件
 * 3. 将截图转换为Base64
 * 4. 压缩图片
 */
object ScreenCapture {

    private const val TAG = "ScreenCapture"

    /**
     * 截图保存路径
     */
    private val SCREENSHOT_DIR = File(
        Environment.getExternalStorageDirectory(),
        "WirelessControl/screenshots"
    )

    init {
        // 创建截图目录
        if (!SCREENSHOT_DIR.exists()) {
            SCREENSHOT_DIR.mkdirs()
        }
    }

    /**
     * 执行截图（使用screencap命令）
     */
    fun captureScreen(): ByteArray? {
        return try {
            val tempFile = File(SCREENSHOT_DIR, "temp_screen.png")

            // 执行screencap命令
            val process = Runtime.getRuntime().exec(arrayOf(
                "su",
                "-c",
                "screencap -p ${tempFile.absolutePath}"
            ))

            val exitCode = process.waitFor()
            if (exitCode == 0 && tempFile.exists()) {
                // 读取截图文件
                val bytes = tempFile.readBytes()
                tempFile.delete()
                Log.d(TAG, "Screenshot captured: ${bytes.size} bytes")
                bytes
            } else {
                Log.e(TAG, "screencap failed with exit code: $exitCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screen", e)
            null
        }
    }

    /**
     * 截图并保存到文件
     */
    fun captureScreenToFile(filename: String): File? {
        return try {
            val bytes = captureScreen() ?: return null
            val file = File(SCREENSHOT_DIR, filename)

            FileOutputStream(file).use { output ->
                output.write(bytes)
            }

            Log.d(TAG, "Screenshot saved: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save screenshot", e)
            null
        }
    }

    /**
     * 截图并转换为Base64
     */
    fun captureScreenToBase64(quality: Int = 80): String? {
        return try {
            val bytes = captureScreen() ?: return null

            // 解码为Bitmap进行压缩
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val stream = ByteArrayOutputStream()

            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            val compressedBytes = stream.toByteArray()

            // 转换为Base64
            val base64 = android.util.Base64.encodeToString(
                compressedBytes,
                android.util.Base64.NO_WRAP
            )

            Log.d(TAG, "Screenshot converted to Base64: ${base64.length} chars")
            base64
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert to Base64", e)
            null
        }
    }

    /**
     * 压缩截图
     */
    fun compressScreenshot(bytes: ByteArray, maxWidth: Int = 1080, quality: Int = 80): ByteArray? {
        return try {
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            // 计算缩放比例
            val width = bitmap.width
            val height = bitmap.height
            val scale = if (width > maxWidth) maxWidth.toFloat() / width else 1f

            val scaledWidth = (width * scale).toInt()
            val scaledHeight = (height * scale).toInt()

            // 缩放图片
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                scaledWidth,
                scaledHeight,
                true
            )

            // 压缩为JPEG
            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

            Log.d(TAG, "Screenshot compressed: ${bytes.size} -> ${stream.size()} bytes")
            stream.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress screenshot", e)
            null
        }
    }

    /**
     * 删除旧的截图文件
     */
    fun cleanupOldScreenshots(maxAgeHours: Int = 24) {
        try {
            val currentTime = System.currentTimeMillis()
            val maxAge = maxAgeHours * 60 * 60 * 1000L

            SCREENSHOT_DIR.listFiles()?.forEach { file ->
                if (currentTime - file.lastModified() > maxAge) {
                    file.delete()
                    Log.d(TAG, "Deleted old screenshot: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old screenshots", e)
        }
    }

    /**
     * 获取截图文件列表
     */
    fun getScreenshotFiles(): List<File> {
        return try {
            SCREENSHOT_DIR.listFiles()?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get screenshot files", e)
            emptyList()
        }
    }

    /**
     * 删除指定截图
     */
    fun deleteScreenshot(filename: String): Boolean {
        return try {
            val file = File(SCREENSHOT_DIR, filename)
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted screenshot: $filename")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete screenshot", e)
            false
        }
    }

    /**
     * 清空所有截图
     */
    fun clearAllScreenshots(): Boolean {
        return try {
            SCREENSHOT_DIR.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "All screenshots cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear screenshots", e)
            false
        }
    }
}

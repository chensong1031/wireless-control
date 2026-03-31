package com.wireless.control.device

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 设备控制主Activity
 * 功能：扫码注册、连接服务器、心跳上报、消息上报
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val SCAN_MODE = 1
        private const val CONNECTED_MODE = 2
        
        private var instance: MainActivity? = null
        private var deviceToken: String? = null
        private var deviceId: Int? = null
        private var serverUrl: String? = null
        
        fun getInstance(): MainActivity? = instance
        fun getServerUrl(): String? = serverUrl
        fun getDeviceToken(): String? = deviceToken
        fun getDeviceId(): Int? = deviceId
        
        internal fun setDeviceId(id: Int?) { deviceId = id }
        internal fun setDeviceToken(token: String?) { deviceToken = token }
        internal fun setServerUrl(url: String?) { serverUrl = url }
        internal fun setInstance(inst: MainActivity?) { instance = inst }
    }

    private lateinit var modeTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var actionButton: Button
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    
    private val httpClient = OkHttpClient()
    
    private var currentMode = SCAN_MODE
    private var cameraProvider: ProcessCameraProvider? = null
    private var isScanning = false
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "需要摄像头权限才能扫描二维码", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        
        try {
            setContentView(R.layout.activity_main)
            initViews()
            
            // 检查是否已配置服务器
            if (isServerConfigured()) {
                switchToConnectedMode()
                startHeartbeat()
            } else {
                switchToScanMode()
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to initialize MainActivity", e)
            try {
                setContentView(R.layout.activity_main)
            } catch (e2: Exception) {
                Log.e(TAG, "✗ Failed to set content view", e2)
            }
        }
    }

    private fun initViews() {
        modeTextView = findViewById(R.id.modeTextView)
        statusTextView = findViewById(R.id.statusTextView)
        actionButton = findViewById(R.id.actionButton)
        previewView = findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        actionButton.setOnClickListener {
            when (currentMode) {
                SCAN_MODE -> startScanQRCode()
                CONNECTED_MODE -> disconnectFromServer()
            }
        }
    }

    private fun switchToScanMode() {
        currentMode = SCAN_MODE
        runOnUiThread {
            modeTextView.text = "扫码注册"
            statusTextView.text = "点击按钮扫描二维码"
            actionButton.text = "开始扫码"
            previewView.visibility = android.view.View.VISIBLE
        }
    }

    private fun switchToConnectedMode() {
        currentMode = CONNECTED_MODE
        runOnUiThread {
            modeTextView.text = "已连接"
            statusTextView.text = "服务器: $serverUrl\n设备ID: $deviceId"
            actionButton.text = "断开连接"
            previewView.visibility = android.view.View.GONE
        }
        
        stopCamera()
    }

    private fun startScanQRCode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
            isScanning = true
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
                
                runOnUiThread {
                    statusTextView.text = "正在扫描..."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        try {
            val mediaImage = imageProxy.image
            if (mediaImage != null && isScanning) {
                val buffer = mediaImage.planes[0].buffer
                val yData = ByteArray(buffer.remaining())
                buffer.get(yData)
                
                val width = mediaImage.width
                val height = mediaImage.height
                
                val luminances = IntArray(width * height)
                for (i in yData.indices) {
                    luminances[i] = yData[i].toInt() and 0xFF
                }
                
                val rgbLuminanceSource = RGBLuminanceSource(width, height, luminances)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(rgbLuminanceSource))
                
                val reader = MultiFormatReader()
                val hints = mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true
                )
                reader.setHints(hints)
                
                try {
                    val result = reader.decode(binaryBitmap)
                    val qrData = result.text
                    
                    if (qrData != null && qrData.contains("|")) {
                        Log.i(TAG, "Scanned QR code: $qrData")
                        handleQRCodeData(qrData)
                        isScanning = false
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image", e)
        } finally {
            imageProxy.close()
        }
    }

    private fun handleQRCodeData(qrData: String) {
        try {
            val parts = qrData.split("|")
            if (parts.size >= 2) {
                serverUrl = parts[0]
                val token = parts[1]
                
                Log.i(TAG, "Server URL: $serverUrl")
                Log.i(TAG, "Token: $token")
                
                runOnUiThread {
                    Toast.makeText(this, "扫描成功，正在注册...", Toast.LENGTH_SHORT).show()
                    statusTextView.text = "正在注册到服务器..."
                }
                
                stopCamera()
                
                GlobalScope.launch(Dispatchers.IO) {
                    registerToDeviceServer(token)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse QR code data", e)
            runOnUiThread {
                Toast.makeText(this, "二维码格式错误", Toast.LENGTH_SHORT).show()
                statusTextView.text = "扫码失败，请重试"
                isScanning = true
            }
        }
    }

    private fun registerToDeviceServer(token: String) {
        try {
            val deviceInfo = JSONObject().apply {
                put("name", android.os.Build.MODEL)
                put("brand", android.os.Build.BRAND)
                put("model", android.os.Build.MODEL)
                put("version", android.os.Build.VERSION.RELEASE)
                put("sdk", android.os.Build.VERSION.SDK_INT)
            }
            
            val payload = JSONObject().apply {
                put("token", token)
                put("device_info", deviceInfo)
            }
            
            val response = httpPost("$serverUrl/api/device-conn/register", payload.toString())
            val registerResult = JSONObject(response)
            
            if (registerResult.getInt("code") == 200) {
                val data = registerResult.getJSONObject("data")
                setDeviceId(data.getInt("device_id"))
                setDeviceToken(data.getString("device_token"))
                
                saveConfig()
                
                runOnUiThread {
                    Toast.makeText(this, "注册成功！", Toast.LENGTH_SHORT).show()
                    switchToConnectedMode()
                    startHeartbeat()
                }
            } else {
                throw Exception(registerResult.getString("message"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            runOnUiThread {
                Toast.makeText(this, "注册失败: ${e.message}", Toast.LENGTH_SHORT).show()
                statusTextView.text = "注册失败，请重试"
                isScanning = true
                switchToScanMode()
            }
        }
    }

    private fun startHeartbeat() {
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                delay(30000)
                try {
                    sendHeartbeat()
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat failed", e)
                }
            }
        }
    }

    private fun sendHeartbeat() {
        val payload = JSONObject().apply {
            put("device_id", deviceId)
            put("token", deviceToken)
        }
        
        val response = httpPost("$serverUrl/api/device-conn/heartbeat", payload.toString())
        Log.d(TAG, "Heartbeat response: $response")
    }

    private fun disconnectFromServer() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("device_id", deviceId)
                    put("token", deviceToken)
                }
                httpPost("$serverUrl/api/device-conn/disconnect", payload.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect failed", e)
            }
            
            clearConfig()
            setDeviceId(null)
            setDeviceToken(null)
            setServerUrl(null)
            
            runOnUiThread {
                Toast.makeText(this@MainActivity, "已断开连接", Toast.LENGTH_SHORT).show()
                switchToScanMode()
            }
        }
    }

    private fun isServerConfigured(): Boolean {
        val prefs = getSharedPreferences("wireless_control", MODE_PRIVATE)
        setServerUrl(prefs.getString("server_url", null))
        setDeviceToken(prefs.getString("device_token", null))
        setDeviceId(prefs.getInt("device_id", -1))
        return serverUrl != null && deviceToken != null && deviceId != -1
    }

    private fun saveConfig() {
        val prefs = getSharedPreferences("wireless_control", MODE_PRIVATE)
        prefs.edit().apply {
            putString("server_url", serverUrl)
            putString("device_token", deviceToken)
            putInt("device_id", deviceId ?: -1)
            apply()
        }
    }

    private fun clearConfig() {
        val prefs = getSharedPreferences("wireless_control", MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun stopCamera() {
        try {
            isScanning = false
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop camera", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        cameraExecutor.shutdown()
        stopCamera()
    }
    
    fun reportWeChatMessage(message: JSONObject) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("device_id", deviceId)
                    put("token", deviceToken)
                    put("type", "wechat")
                    put("content", message)
                }
                
                httpPost("$serverUrl/api/device-conn/message", payload.toString())
                Log.d(TAG, "WeChat message reported")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report message", e)
            }
        }
    }
    
    fun reportQQMessage(message: JSONObject) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("device_id", deviceId)
                    put("token", deviceToken)
                    put("type", "qq")
                    put("content", message)
                }
                
                httpPost("$serverUrl/api/device-conn/message", payload.toString())
                Log.d(TAG, "QQ message reported")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report message", e)
            }
        }
    }

    private fun httpPost(url: String, jsonBody: String): String {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        
        val response = httpClient.newCall(request).execute()
        return response.body?.string() ?: throw Exception("Empty response")
    }
}
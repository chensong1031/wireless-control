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
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.*
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
    }

    private lateinit var modeTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var actionButton: Button
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    
    private var currentMode = SCAN_MODE
    private var cameraProvider: ProcessCameraProvider? = null
    
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
        
        setContentView(R.layout.activity_main)
        
        try {
            initViews()
            checkServerStatus()
            
            // 检查是否已配置服务器
            if (isServerConfigured()) {
                switchToConnectedMode()
                startHeartbeat()
            } else {
                switchToScanMode()
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to initialize MainActivity", e)
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun checkServerStatus() {
        try {
            val server = WirelessControlApp.getServer()
            if (server != null) {
                Log.i(TAG, "✓ HTTP server is running on port 8080")
            } else {
                Log.w(TAG, "⚠ HTTP server is not running")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to check server status", e)
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
        
        // 停止相机
        stopCamera()
    }

    private fun startScanQRCode() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            == PackageManager.PERMISSION_GRANTED) {
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
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val qrData = barcode.rawValue
                        if (qrData != null && qrData.contains("|")) {
                            Log.i(TAG, "Scanned QR code: $qrData")
                            handleQRCodeData(qrData)
                            imageProxy.close()
                            return@addOnSuccessListener
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Barcode scan failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
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
            }
        }
    }

    private fun registerToDeviceServer(token: String) {
        try {
            // 获取设备信息
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
            
            val response = HttpClient.post("$serverUrl/api/device-conn/register", payload.toString())
            val result = JSONObject(response)
            
            if (result.getInt("code") == 200) {
                val data = result.getJSONObject("data")
                deviceId = data.getInt("device_id")
                deviceToken = data.getString("device_token")
                
                // 保存配置
                saveConfig()
                
                runOnUiThread {
                    Toast.makeText(this, "注册成功！", Toast.LENGTH_SHORT).show()
                    switchToConnectedMode()
                    startHeartbeat()
                }
            } else {
                throw Exception(result.getString("message"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            runOnUiThread {
                Toast.makeText(this, "注册失败: ${e.message}", Toast.LENGTH_SHORT).show()
                statusTextView.text = "注册失败，请重试"
                switchToScanMode()
            }
        }
    }

    private fun startHeartbeat() {
        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                delay(30000) // 每30秒发送一次心跳
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
        
        val response = HttpClient.post("$serverUrl/api/device-conn/heartbeat", payload.toString())
        Log.d(TAG, "Heartbeat response: $response")
    }

    private fun disconnectFromServer() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("device_id", deviceId)
                    put("token", deviceToken)
                }
                HttpClient.post("$serverUrl/api/device-conn/disconnect", payload.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect failed", e)
            }
            
            clearConfig()
            deviceId = null
            deviceToken = null
            serverUrl = null
            
            runOnUiThread {
                Toast.makeText(this@MainActivity, "已断开连接", Toast.LENGTH_SHORT).show()
                switchToScanMode()
            }
        }
    }

    private fun isServerConfigured(): Boolean {
        val prefs = getSharedPreferences("wireless_control", MODE_PRIVATE)
        serverUrl = prefs.getString("server_url", null)
        deviceToken = prefs.getString("device_token", null)
        deviceId = prefs.getInt("device_id", -1)
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
    
    /**
     * 上报微信消息
     */
    fun reportWeChatMessage(message: JSONObject) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("device_id", deviceId)
                    put("token", deviceToken)
                    put("type", "wechat")
                    put("content", message)
                }
                
                HttpClient.post("$serverUrl/api/device-conn/message", payload.toString())
                Log.d(TAG, "WeChat message reported")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report message", e)
            }
        }
    }
    
    /**
     * 上报QQ消息
     */
    fun reportQQMessage(message: JSONObject) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("device_id", deviceId)
                    put("token", deviceToken)
                    put("type", "qq")
                    put("content", message)
                }
                
                HttpClient.post("$serverUrl/api/device-conn/message", payload.toString())
                Log.d(TAG, "QQ message reported")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to report message", e)
            }
        }
    }
}

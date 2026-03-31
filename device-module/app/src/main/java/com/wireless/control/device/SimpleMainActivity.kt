package com.wireless.control.device

import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.graphics.Color
import android.widget.LinearLayout
import android.os.Handler
import android.os.Looper

/**
 * 真实HTTP服务器测试版
 */
class SimpleMainActivity : android.app.Activity() {

    private var httpServer: com.wireless.control.device.server.DeviceControlServer? = null
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建布局
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 50, 50, 50)
        layout.gravity = android.view.Gravity.CENTER
        
        // 标题
        val title = TextView(this)
        title.text = "工作通 - 测试版"
        title.textSize = 24f
        title.setTextColor(Color.BLACK)
        title.gravity = android.view.Gravity.CENTER
        layout.addView(title)
        
        // 状态
        val status = TextView(this)
        status.text = "HTTP服务器: 未启动"
        status.textSize = 16f
        status.setTextColor(Color.GRAY)
        status.setPadding(0, 20, 0, 20)
        layout.addView(status)
        
        // 按钮
        val button = Button(this)
        button.text = "启动HTTP服务器"
        layout.addView(button)
        
        setContentView(layout)
        
        // 按钮点击事件 - 真正启动HTTP服务器
        button.setOnClickListener {
            status.text = "正在启动服务器..."
            button.isEnabled = false
            
            // 在后台线程启动，避免阻塞UI
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    // 真正创建和启动HTTP服务器
                    httpServer = com.wireless.control.device.server.DeviceControlServer(this@SimpleMainActivity, 8080)
                    val started = httpServer?.start() ?: false
                    
                    if (started) {
                        status.text = "HTTP服务器: 启动成功！\n端口: 8080"
                        status.setTextColor(Color.GREEN)
                        button.text = "已启动"
                    } else {
                        status.text = "HTTP服务器: 启动失败"
                        status.setTextColor(Color.RED)
                        button.isEnabled = true
                    }
                } catch (e: Exception) {
                    status.text = "HTTP服务器: 启动失败\n${e.javaClass.simpleName}: ${e.message}"
                    status.setTextColor(Color.RED)
                    button.isEnabled = true
                    e.printStackTrace()
                }
            }, 500)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 停止HTTP服务器
        try {
            httpServer?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
package com.wireless.control.device

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Button

/**
 * 中等复杂度测试版
 */
class SimpleMainActivity : android.app.Activity() {

    companion object {
        private const val TAG = "SimpleMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "onCreate started")
        
        try {
            // 创建一个带按钮的布局
            val layout = android.widget.LinearLayout(this)
            layout.orientation = android.widget.LinearLayout.VERTICAL
            layout.setPadding(50, 50, 50, 50)
            layout.gravity = android.view.Gravity.CENTER
            
            // 标题
            val title = TextView(this)
            title.text = "工作通 - 测试版"
            title.textSize = 28f
            title.setPadding(0, 0, 0, 50)
            layout.addView(title)
            
            // 状态
            val status = TextView(this)
            status.text = "HTTP 服务器启动中..."
            status.textSize = 16f
            layout.addView(status)
            
            // 按钮
            val button = Button(this)
            button.text = "测试按钮"
            layout.addView(button)
            
            setContentView(layout)
            
            Log.d(TAG, "✓ onCreate completed")
            
            // 3秒后更新状态
            button.postDelayed({
                status.text = "HTTP 服务器: 运行中\n端口: 8080"
            }, 3000)
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ onCreate failed", e)
            e.printStackTrace()
        }
    }
}
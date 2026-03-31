package com.wireless.control.device

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * 临时测试版 - 移除所有 Xposed 功能
 * 用于测试基础应用是否能正常启动
 */
class TestMainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TestMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "===== onCreate 开始 =====")
        
        try {
            // 检查 1: 基础布局
            Log.d(TAG, "步骤1: 创建基础布局")
            val layout = android.widget.LinearLayout(this)
            layout.orientation = android.widget.LinearLayout.VERTICAL
            layout.gravity = android.view.Gravity.CENTER
            
            // 添加标题
            val title = android.widget.TextView(this)
            title.text = "工作通 - 测试版"
            title.textSize = 32f
            title.setPadding(0, 50, 0, 20)
            title.gravity = android.view.Gravity.CENTER
            layout.addView(title)
            
            // 添加状态
            val status = android.widget.TextView(this)
            status.text = "应用运行正常 ✓"
            status.textSize = 18f
            status.setTextColor(-16711936)
            status.gravity = android.view.Gravity.CENTER
            layout.addView(status)
            
            setContentView(layout)
            
            Log.d(TAG, "✓ setContentView 成功")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ onCreate 失败", e)
            e.printStackTrace()
            
            // 即使出错也尝试设置一个简单的内容
            try {
                val errorText = android.widget.TextView(this)
                errorText.text = "应用初始化失败\n\n错误: ${e.message}"
                errorText.textSize = 16f
                errorText.setPadding(20, 20, 20, 20)
                setContentView(errorText)
            } catch (e2: Exception) {
                Log.e(TAG, "✗ 甚至错误界面也失败", e2)
            }
        }
        
        Log.d(TAG, "===== onCreate 完成 =====")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - 应用应该可见了")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy - 应用被销毁")
    }
}

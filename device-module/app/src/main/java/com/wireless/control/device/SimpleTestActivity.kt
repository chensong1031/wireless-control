package com.wireless.control.device

import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import android.widget.Toast

/**
 * 极简测试版 - 排除所有复杂逻辑
 */
class SimpleTestActivity : android.app.Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建一个最简单的界面
        val textView = TextView(this)
        textView.text = "如果能看到这行字，说明应用没有闪退！\n\n时间: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}"
        textView.setPadding(50, 50, 50, 50)
        textView.textSize = 18f
        
        setContentView(textView)
        
        // 延迟3秒显示Toast测试
        textView.postDelayed({
            Toast.makeText(this, "应用正常运行！", Toast.LENGTH_LONG).show()
        }, 3000)
    }
}

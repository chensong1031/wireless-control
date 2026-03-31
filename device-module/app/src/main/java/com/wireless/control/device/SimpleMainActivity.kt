package com.wireless.control.device

import android.os.Bundle
import android.widget.TextView
import android.graphics.Color

/**
 * 单 TextView 测试版
 */
class SimpleMainActivity : android.app.Activity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 只创建一个TextView
        val textView = TextView(this)
        textView.text = "工作通 - 测试版\n\n应用启动成功！"
        textView.textSize = 20f
        textView.setTextColor(Color.BLACK)
        textView.setPadding(50, 50, 50, 50)
        textView.gravity = android.view.Gravity.CENTER
        
        setContentView(textView)
    }
}
package com.wireless.control.device

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast

/**
 * 简化版 MainActivity
 */
class SimpleMainActivity : android.app.Activity() {

    companion object {
        private const val TAG = "SimpleMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "onCreate started")
        
        try {
            val textView = TextView(this)
            textView.text = "工作通 - 简化版\n\n点击按钮测试"
            textView.textSize = 20f
            textView.gravity = android.view.Gravity.CENTER
            textView.setPadding(50, 50, 50, 50)
            
            setContentView(textView)
            
            Log.d(TAG, "✓ onCreate completed")
            
            // 测试 Toast
            Toast.makeText(this, "应用正常运行!", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ onCreate failed", e)
            e.printStackTrace()
        }
    }
}
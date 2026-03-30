package com.wireless.control.device

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * 简化版 MainActivity - 用于调试崩溃问题
 */
class SimpleMainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SimpleMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "onCreate started")
        
        try {
            // 创建一个简单的文本视图
            val textView = TextView(this)
            textView.text = "工作通 - 简化版"
            textView.textSize = 24f
            textView.gravity = android.view.Gravity.CENTER
            
            setContentView(textView)
            
            Log.d(TAG, "✓ onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "✗ onCreate failed", e)
            e.printStackTrace()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
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
        Log.d(TAG, "onDestroy")
    }
}

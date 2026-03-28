package com.wireless.control.device

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * 设备控制主Activity
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private var instance: MainActivity? = null

        fun getInstance(): MainActivity? {
            return instance
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        try {
            Log.i(TAG, "MainActivity created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MainActivity", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}

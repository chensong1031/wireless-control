package com.wireless.control.device

import android.app.Application

/**
 * 应用类 - 暂时禁用所有功能
 */
class WirelessControlApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // 暂时不启动任何东西
    }
}
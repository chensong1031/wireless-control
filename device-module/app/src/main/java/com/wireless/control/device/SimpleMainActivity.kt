package com.wireless.control.device

import android.app.Activity

/**
 * 最简测试版 - 不依赖任何外部组件
 */
class SimpleMainActivity : Activity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // 不设置任何界面，只测试能否启动
    }
}
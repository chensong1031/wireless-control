package com.wireless.control.device

import android.util.Log

/**
 * 无线群控设备端Xposed模块主类
 *
 * 注意：由于 Xposed API 不在标准的 Maven 仓库中，需要手动添加 JAR 文件。
 * 请查看 app/libs/README.md 了解如何获取 Xposed API JAR 文件。
 */
class WirelessControlModule {

    companion object {
        private const val TAG = "WirelessControl"
    }

    /**
     * 当添加了 Xposed API JAR 文件后，取消下面的注释并实现 IXposedHookLoadPackage 接口
     */
    /*
    init {
        Log.i(TAG, "WirelessControlModule loaded")
    }
    */
}

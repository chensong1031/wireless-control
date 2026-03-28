package com.wireless.control.device

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 无线群控设备端Xposed模块主类
 */
class WirelessControlModule : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "WirelessControl"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName
        Log.d(TAG, "Loading package: $packageName")

        when (packageName) {
            "com.tencent.mm" -> hookWeChat(lpparam)
            "com.android.systemui" -> hookSystemUI(lpparam)
            "com.tencent.mobileqq" -> hookQQ(lpparam)
        }
    }

    private fun hookWeChat(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Log.i(TAG, "Hooking WeChat")
            // Hook 占位，实际实现需要适配微信版本
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook WeChat", e)
        }
    }

    private fun hookSystemUI(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Log.i(TAG, "Hooking SystemUI")
            // Hook 占位
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook SystemUI", e)
        }
    }

    private fun hookQQ(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Log.i(TAG, "Hooking QQ")
            // Hook 占位
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook QQ", e)
        }
    }
}

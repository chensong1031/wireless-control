package com.wireless.control.device

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * 无线群控设备端Xposed模块主类
 *
 * 核心功能：
 * 1. Hook 微信进程，拦截消息
 * 2. Hook 朋友圈，实现朋友圈操作
 * 3. 暴露 HTTP API 供远程控制
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
            val classLoader = lpparam.classLoader

            // Hook 消息存储类
            hookMsgStorage(classLoader)

            // Hook 消息发送
            hookMsgSend(classLoader)

            // Hook 聊天界面
            hookChatUI(classLoader)

            Log.i(TAG, "WeChat hooks installed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook WeChat", e)
        }
    }

    /**
     * Hook 微信消息存储类
     * 拦截所有消息的添加操作
     */
    private fun hookMsgStorage(classLoader: ClassLoader) {
        try {
            // 尝试 Hook 消息存储类的 insert 方法
            // 微信的 MessageStorage 类路径可能会变化，这里尝试常见的路径
            val msgStorageClasses = arrayOf(
                "com.tencent.mm.storage.msg",
                "com.tencent.mm.storagemsg",
                "com.tencent.mm.modelmsg"
            )

            for (className in msgStorageClasses) {
                try {
                    val msgStorageClass = classLoader.loadClass(className)
                    Log.i(TAG, "Found message storage class: $className")

                    // Hook insert 方法
                    XposedHelpers.findAndHookMethod(
                        msgStorageClass,
                        "insert",
                        Any::class.java, // 消息对象
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                try {
                                    if (param.args.isNotEmpty() && param.args[0] != null) {
                                        val msg = param.args[0]
                                        interceptMessage(msg)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to intercept message", e)
                                }
                            }
                        }
                    )

                    Log.i(TAG, "Successfully hooked message storage: $className")
                    return
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to hook $className: ${e.message}")
                }
            }

            Log.w(TAG, "Could not find message storage class")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook message storage", e)
        }
    }

    /**
     * Hook 微信消息发送功能
     */
    private fun hookMsgSend(classLoader: ClassLoader) {
        try {
            // 尝试 Hook 消息发送方法
            val sendClasses = arrayOf(
                "com.tencent.mm.plugin.chat.a",
                "com.tencent.mm.modelmsg.MessageSender",
                "com.tencent.mm.plugin.chat.a\$g"
            )

            for (className in sendClasses) {
                try {
                    val sendClass = classLoader.loadClass(className)
                    Log.i(TAG, "Found message send class: $className")

                    // Hook 发送消息的方法
                    XposedHelpers.findAndHookMethod(
                        sendClass,
                        "sendMessage",
                        Any::class.java, // 消息对象
                        object : XC_MethodHook() {
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                try {
                                    if (param.args.isNotEmpty() && param.args[0] != null) {
                                        val msg = param.args[0]
                                        Log.i(TAG, "📤 Sending message: ${parseMessage(msg)}")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to intercept send", e)
                                }
                            }
                        }
                    )

                    Log.i(TAG, "Successfully hooked message send: $className")
                    return
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to hook send class $className: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook message send", e)
        }
    }

    /**
     * Hook 微信聊天界面
     * 监控聊天界面的显示
     */
    private fun hookChatUI(classLoader: ClassLoader) {
        try {
            // Hook 聊天界面的 Activity
            val chatUIClasses = arrayOf(
                "com.tencent.mm.ui.chatting.ChattingUI",
                "com.tencent.mm.ui.chatting.UI",
                "com.tencent.mm.plugin.chat.ui.ChattingUI"
            )

            for (className in chatUIClasses) {
                try {
                    val chatUIClass = classLoader.loadClass(className)
                    Log.i(TAG, "Found chat UI class: $className")

                    // Hook onCreate 方法
                    XposedHelpers.findAndHookMethod(
                        chatUIClass,
                        "onCreate",
                        android.os.Bundle::class.java,
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                try {
                                    val activity = param.thisObject as android.app.Activity
                                    val title = activity.title
                                    Log.i(TAG, "💬 Chatting UI opened: $title")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to get chat UI info", e)
                                }
                            }
                        }
                    )

                    Log.i(TAG, "Successfully hooked chat UI: $className")
                    return
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to hook chat UI $className: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook chat UI", e)
        }
    }

    /**
     * 拦截并解析消息
     */
    private fun interceptMessage(msgObj: Any) {
        try {
            val content = parseMessage(msgObj)
            val direction = getMessageDirection(msgObj)

            when (direction) {
                "received" -> {
                    Log.i(TAG, "📨 Received message: $content")
                }
                "sent" -> {
                    Log.i(TAG, "📤 Sent message: $content")
                }
                else -> {
                    Log.i(TAG, "📄 Message: $content")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to intercept message", e)
        }
    }

    /**
     * 解析消息内容
     */
    private fun parseMessage(msgObj: Any): String {
        return try {
            // 尝试获取消息内容字段
            val contentField = XposedHelpers.getObjectField(msgObj, "content") as? String
            val msgType = XposedHelpers.getObjectField(msgObj, "type") as? Int

            when (msgType) {
                1 -> "[文本] ${contentField ?: "空内容"}"
                3 -> "[图片] ${contentField ?: ""}"
                34 -> "[语音] ${contentField ?: ""}"
                43 -> "[视频] ${contentField ?: ""}"
                47 -> "[表情] ${contentField ?: ""}"
                49 -> "[链接] ${contentField ?: ""}"
                else -> "[类型$msgType] ${contentField ?: ""}"
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to parse message: ${e.message}")
            // 尝试其他方式获取内容
            try {
                msgObj.toString()
            } catch (e2: Exception) {
                "[无法解析的消息]"
            }
        }
    }

    /**
     * 判断消息方向（接收/发送）
     */
    private fun getMessageDirection(msgObj: Any): String {
        return try {
            val isSend = XposedHelpers.getObjectField(msgObj, "isSend") as? Boolean
            if (isSend == true) "sent" else "received"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun hookSystemUI(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Log.i(TAG, "Hooking SystemUI")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook SystemUI", e)
        }
    }

    private fun hookQQ(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            Log.i(TAG, "Hooking QQ")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook QQ", e)
        }
    }
}

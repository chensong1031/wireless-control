package com.wireless.control.device

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File

/**
 * 无线群控设备端Xposed模块主类
 *
 * 核心功能：
 * 1. Hook 微信进程，拦截消息
 * 2. Hook 朋友圈，实现朋友圈操作
 * 3. 暴露 HTTP API 供远程控制
 * 4. 反检测功能：避免被微信检测到Xposed/Root
 */
class WirelessControlModule : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "WirelessControl"
        private const val WECHAT_PACKAGE = "com.tencent.mm"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        val packageName = lpparam.packageName
        Log.d(TAG, "Loading package: $packageName")

        when (packageName) {
            WECHAT_PACKAGE -> {
                // 先执行反检测，再进行其他Hook
                antiDetection(lpparam.classLoader)
                hookWeChat(lpparam)
            }
            "com.android.systemui" -> hookSystemUI(lpparam)
            "com.tencent.mobileqq" -> hookQQ(lpparam)
        }
    }

    /**
     * 反检测功能：避免微信检测到Xposed和Root
     */
    private fun antiDetection(classLoader: ClassLoader) {
        try {
            Log.i(TAG, "Running anti-detection for WeChat")

            // 1. Hook 文件存在性检查，隐藏Root相关文件
            hookFileExistsCheck()

            // 2. Hook 系统属性读取，隐藏调试和Root标识
            hookSystemProperties()

            // 3. Hook 类加载检查，隐藏Xposed相关类
            hookClassLoadingCheck(classLoader)

            // 4. Hook 进程内存检查，隐藏Xposed模块
            hookMemoryCheck()

            Log.i(TAG, "Anti-detection measures applied")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply anti-detection", e)
        }
    }

    /**
     * Hook 文件存在性检查，隐藏Root相关文件
     */
    private fun hookFileExistsCheck() {
        try {
            // 要隐藏的Root相关文件路径
            val rootFiles = arrayOf(
                "/system/bin/su",
                "/system/xbin/su",
                "/sbin/su",
                "/su",
                "/system/app/Superuser.apk",
                "/system/app/SuperSU.apk",
                "/system/xbin/daemonsu",
                "/system/etc/init.d/99SuperSUDaemon",
                "/dev/com.koushikdutta.superuser.daemon/",
                "/data/app/eu.chainfire.supersu-1.apk",
                "/data/app/eu.chainfire.supersu-2.apk",
                "/data/local/tmp/su",
                "/data/local/su",
                "/data/local/xbin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/su/bin/su",
                "/magisk",
                "/sbin/magisk",
                "/system/bin/magisk",
                "/data/adb/magisk"
            )

            // Hook File.exists() 方法
            XposedHelpers.findAndHookMethod(
                File::class.java,
                "exists",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val file = param.thisObject as File
                        val path = file.absolutePath
                        if (rootFiles.contains(path) || path.contains("xposed") || path.contains("lsposed")) {
                            // 返回false，假装文件不存在
                            param.result = false
                        }
                    }
                }
            )

            // Hook File.canExecute() 方法
            XposedHelpers.findAndHookMethod(
                File::class.java,
                "canExecute",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val file = param.thisObject as File
                        val path = file.absolutePath
                        if (rootFiles.contains(path)) {
                            param.result = false
                        }
                    }
                }
            )

            Log.d(TAG, "Root file check hook installed")
        } catch (e: Exception) {
            Log.d(TAG, "Failed to hook file check: ${e.message}")
        }
    }

    /**
     * Hook 系统属性读取，隐藏调试和Root标识
     */
    private fun hookSystemProperties() {
        try {
            // 要隐藏的系统属性
            val suspiciousProps = arrayOf(
                "ro.debuggable",
                "ro.secure",
                "ro.build.type",
                "ro.build.tags",
                "ro.product.model",
                "ro.hardware",
                "init.svc.su",
                "init.svc.supersu",
                "init.svc.magiskd",
                "persist.sys.root_access",
                "sys.isolated_storage",
                "ro.build.selinux"
            )

            // Hook SystemProperties.get()
            val systemPropertiesClass = XposedHelpers.findClass("android.os.SystemProperties", null)
            XposedHelpers.findAndHookMethod(
                systemPropertiesClass,
                "get",
                String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        val result = param.result as String
                        
                        when (key) {
                            "ro.debuggable" -> param.result = "0"
                            "ro.secure" -> param.result = "1"
                            "ro.build.type" -> param.result = "user"
                            "ro.build.tags" -> param.result = "release-keys"
                            "ro.build.selinux" -> param.result = "1"
                        }

                        if (suspiciousProps.contains(key) && (result.contains("test") || result.contains("debug") || result.contains("root"))) {
                            param.result = result.replace("test", "").replace("debug", "").replace("root", "")
                        }
                    }
                }
            )

            // Hook SystemProperties.getBoolean()
            XposedHelpers.findAndHookMethod(
                systemPropertiesClass,
                "getBoolean",
                String::class.java,
                Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        if (key == "ro.debuggable" || key.contains("root") || key.contains("su")) {
                            param.result = false
                        }
                    }
                }
            )

            Log.d(TAG, "System properties hook installed")
        } catch (e: Exception) {
            Log.d(TAG, "Failed to hook system properties: ${e.message}")
        }
    }

    /**
     * Hook 类加载检查，隐藏Xposed相关类
     */
    private fun hookClassLoadingCheck(classLoader: ClassLoader) {
        try {
            // 要隐藏的Xposed相关类名
            val xposedClasses = arrayOf(
                "de.robv.android.xposed.",
                "de.robv.android.xposed.IXposedHookLoadPackage",
                "de.robv.android.xposed.XposedHelpers",
                "de.robv.android.xposed.XC_MethodHook",
                "de.robv.android.xposed.callbacks.XC_LoadPackage",
                "lsposed.",
                "LSPosed",
                "XposedBridge"
            )

            // Hook ClassLoader.loadClass()
            XposedHelpers.findAndHookMethod(
                ClassLoader::class.java,
                "loadClass",
                String::class.java,
                Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val className = param.args[0] as String
                        for (xposedClass in xposedClasses) {
                            if (className.startsWith(xposedClass) || className.contains("xposed") || className.contains("Xposed")) {
                                // 抛出ClassNotFoundException，假装类不存在
                                throw ClassNotFoundException("$className not found")
                            }
                        }
                    }
                }
            )

            Log.d(TAG, "Class loading check hook installed")
        } catch (e: Exception) {
            Log.d(TAG, "Failed to hook class loading: ${e.message}")
        }
    }

    /**
     * Hook 进程内存检查，隐藏Xposed模块
     */
    private fun hookMemoryCheck() {
        try {
            // Hook Runtime.exec() 方法，拦截读取/proc/self/maps等危险命令
            XposedHelpers.findAndHookMethod(
                Runtime::class.java,
                "exec",
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val command = param.args[0] as String
                        // 拦截读取/proc/self/maps、cat /proc/cpuinfo等检测命令
                        if (command.contains("cat /proc/") || command.contains("grep xposed") || command.contains("grep Xposed") || command.contains("grep lsposed")) {
                            // 执行一个空命令，返回空结果
                            param.result = Runtime.getRuntime().exec("echo")
                        }
                    }
                }
            )

            Log.d(TAG, "Memory check hook installed")
        } catch (e: Exception) {
            Log.d(TAG, "Failed to hook memory check: ${e.message}")
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
                "com.tencent.mm.modelmsg",
                "com.tencent.mm.sdk.platformtools.Message",
                "com.tencent.mm.storage.MessageInfo",
                "com.tencent.mm.model.im.MsgInfo"
            )

            for (className in msgStorageClasses) {
                try {
                    val msgStorageClass = classLoader.loadClass(className)
                    Log.i(TAG, "Found message storage class: $className")

                    // Hook insert, add, save 等多种方法名
                    val methodNames = arrayOf("insert", "add", "save", "addMessage", "insertMessage")
                    for (methodName in methodNames) {
                        try {
                            XposedHelpers.findAndHookMethod(
                                msgStorageClass,
                                methodName,
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
                            Log.i(TAG, "Successfully hooked $methodName on $className")
                            return
                        } catch (e: Exception) {
                            Log.d(TAG, "Failed to hook $methodName on $className: ${e.message}")
                        }
                    }
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
                "com.tencent.mm.plugin.chat.a\$g",
                "com.tencent.mm.model.im.SendMessageHelper",
                "com.tencent.mm.sdk.platformtools.MessageSender",
                "com.tencent.mm.chatting.utils.SendMsgUtil"
            )

            for (className in sendClasses) {
                try {
                    val sendClass = classLoader.loadClass(className)
                    Log.i(TAG, "Found message send class: $className")

                    // Hook 发送消息的方法
                    val methodNames = arrayOf("sendMessage", "send", "sendTextMessage", "sendMsg")
                    for (methodName in methodNames) {
                        try {
                            XposedHelpers.findAndHookMethod(
                                sendClass,
                                methodName,
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
                            Log.i(TAG, "Successfully hooked $methodName on $className")
                            return
                        } catch (e: Exception) {
                            Log.d(TAG, "Failed to hook $methodName on $className: ${e.message}")
                        }
                    }
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
                "com.tencent.mm.plugin.chat.ui.ChattingUI",
                "com.tencent.mm.chatting.ChatActivity",
                "com.tencent.mm.ui.ConversationActivity"
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

                    // 也尝试 onResume 方法，有些界面可能不会调用 onCreate
                    try {
                        XposedHelpers.findAndHookMethod(
                            chatUIClass,
                            "onResume",
                            object : XC_MethodHook() {
                                override fun afterHookedMethod(param: MethodHookParam) {
                                    try {
                                        val activity = param.thisObject as android.app.Activity
                                        val title = activity.title
                                        Log.i(TAG, "💬 Chatting UI resumed: $title")
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to get chat UI resume info", e)
                                    }
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Log.d(TAG, "Failed to hook onResume for $className: ${e.message}")
                    }

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
            val talker = XposedHelpers.getObjectField(msgObj, "talker") as? String
            val createTime = XposedHelpers.getObjectField(msgObj, "createTime") as? Long

            when (msgType) {
                1 -> "[文本] ${contentField ?: "空内容"} （来自：${talker ?: "未知"}）"
                3 -> "[图片] ${contentField ?: ""} （来自：${talker ?: "未知"}）"
                34 -> "[语音] ${contentField ?: ""} （来自：${talker ?: "未知"}）"
                43 -> "[视频] ${contentField ?: ""} （来自：${talker ?: "未知"}）"
                47 -> "[表情] ${contentField ?: ""} （来自：${talker ?: "未知"}）"
                49 -> "[链接] ${contentField ?: ""} （来自：${talker ?: "未知"}）"
                else -> "[类型$msgType] ${contentField ?: ""} （来自：${talker ?: "未知"}）"
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
            val type = XposedHelpers.getObjectField(msgObj, "msgType") as? Int
            if (isSend == true || type == 1) "sent" else "received"
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

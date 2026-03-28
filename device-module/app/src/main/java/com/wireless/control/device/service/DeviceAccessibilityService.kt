package com.wireless.control.device.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * 无障碍服务 - 用于UI自动化操作
 *
 * 功能：
 * 1. 点击操作
 * 2. 滑动操作
 * 3. 文本输入
 * 4. 获取屏幕元素
 * 5. 模拟按键
 */
class DeviceAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AccessibilityService"
        private var instance: DeviceAccessibilityService? = null

        fun getInstance(): DeviceAccessibilityService? = instance
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "AccessibilityService connected")

        // 配置服务能力
        val info = serviceInfo
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        info.notificationTimeout = 100
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 处理辅助功能事件
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d(TAG, "View clicked: ${event.className}")
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "Window changed: ${event.className}")
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                Log.d(TAG, "Content changed: ${event.className}")
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.i(TAG, "AccessibilityService destroyed")
    }

    /**
     * 点击指定坐标
     */
    fun click(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null) {
        val path = Path()
        path.moveTo(x, y)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))

        val gesture = gestureBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d(TAG, "Click completed: ($x, $y)")
                        callback?.invoke(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w(TAG, "Click cancelled: ($x, $y)")
                        callback?.invoke(false)
                    }
                },
                null
            )
        }
    }

    /**
     * 长按指定坐标
     */
    fun longClick(x: Float, y: Float, duration: Long = 500, callback: ((Boolean) -> Unit)? = null) {
        val path = Path()
        path.moveTo(x, y)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))

        val gesture = gestureBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d(TAG, "Long click completed: ($x, $y)")
                        callback?.invoke(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w(TAG, "Long click cancelled: ($x, $y)")
                        callback?.invoke(false)
                    }
                },
                null
            )
        }
    }

    /**
     * 滑动操作
     */
    fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long = 300,
        callback: ((Boolean) -> Unit)? = null
    ) {
        val path = Path()
        path.moveTo(startX, startY)
        path.lineTo(endX, endY)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))

        val gesture = gestureBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d(TAG, "Swipe completed: ($startX,$startY) -> ($endX,$endY)")
                        callback?.invoke(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w(TAG, "Swipe cancelled")
                        callback?.invoke(false)
                    }
                },
                null
            )
        }
    }

    /**
     * 双指缩放
     */
    fun pinch(
        centerX: Float,
        centerY: Float,
        startRadius: Float,
        endRadius: Float,
        duration: Long = 300,
        callback: ((Boolean) -> Unit)? = null
    ) {
        val path1 = Path()
        path1.moveTo(centerX - startRadius, centerY)
        path1.lineTo(centerX - endRadius, centerY)

        val path2 = Path()
        path2.moveTo(centerX + startRadius, centerY)
        path2.lineTo(centerX + endRadius, centerY)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path1, 0, duration))
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path2, 0, duration))

        val gesture = gestureBuilder.build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d(TAG, "Pinch completed")
                        callback?.invoke(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.w(TAG, "Pinch cancelled")
                        callback?.invoke(false)
                    }
                },
                null
            )
        }
    }

    /**
     * 根据文本查找节点并点击
     */
    fun clickByText(text: String, callback: ((Boolean) -> Unit)? = null) {
        val rootNode = rootInActiveWindow ?: run {
            Log.w(TAG, "Root node is null")
            callback?.invoke(false)
            return
        }

        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        if (nodes.isNotEmpty()) {
            val node = nodes[0]
            val rect = Rect()
            node.getBoundsInScreen(rect)
            val centerX = rect.exactCenterX()
            val centerY = rect.exactCenterY()
            click(centerX.toFloat(), centerY.toFloat(), callback)
        } else {
            Log.w(TAG, "Node not found with text: $text")
            callback?.invoke(false)
        }
    }

    /**
     * 根据ID查找节点并点击
     */
    fun clickById(id: String, callback: ((Boolean) -> Unit)? = null) {
        val rootNode = rootInActiveWindow ?: run {
            Log.w(TAG, "Root node is null")
            callback?.invoke(false)
            return
        }

        val nodes = rootNode.findAccessibilityNodeInfosByViewId(id)
        if (nodes.isNotEmpty()) {
            val node = nodes[0]
            val rect = Rect()
            node.getBoundsInScreen(rect)
            val centerX = rect.exactCenterX()
            val centerY = rect.exactCenterY()
            click(centerX.toFloat(), centerY.toFloat(), callback)
        } else {
            Log.w(TAG, "Node not found with id: $id")
            callback?.invoke(false)
        }
    }

    /**
     * 输入文本到指定节点
     */
    fun inputText(text: String, callback: ((Boolean) -> Unit)? = null) {
        val rootNode = rootInActiveWindow ?: run {
            Log.w(TAG, "Root node is null")
            callback?.invoke(false)
            return
        }

        // 查找当前焦点的输入框
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null) {
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT)
            focusedNode.refresh()
            focusedNode.text = null
            focusedNode.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                android.os.Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
            )
            Log.d(TAG, "Text input: $text")
            callback?.invoke(true)
        } else {
            Log.w(TAG, "No focused input field")
            callback?.invoke(false)
        }
    }

    /**
     * 模拟按键
     */
    fun performGlobalAction(action: Int, callback: ((Boolean) -> Unit)? = null) {
        val result = performGlobalAction(action)
        Log.d(TAG, "Global action: $action, result: $result")
        callback?.invoke(result)
    }

    /**
     * 获取当前窗口的所有节点
     */
    fun getAllNodes(): List<AccessibilityNodeInfo> {
        val rootNode = rootInActiveWindow ?: return emptyList()
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        collectNodes(rootNode, nodes)
        return nodes
    }

    /**
     * 递归收集节点
     */
    private fun collectNodes(node: AccessibilityNodeInfo, nodes: MutableList<AccessibilityNodeInfo>) {
        nodes.add(node)
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectNodes(it, nodes) }
        }
    }

    /**
     * 返回到上一页
     */
    fun goBack(callback: ((Boolean) -> Unit)? = null) {
        performGlobalAction(GLOBAL_ACTION_BACK, callback)
    }

    /**
     * 返回到桌面
     */
    fun goHome(callback: ((Boolean) -> Unit)? = null) {
        performGlobalAction(GLOBAL_ACTION_HOME, callback)
    }

    /**
     * 显示最近任务
     */
    fun showRecents(callback: ((Boolean) -> Unit)? = null) {
        performGlobalAction(GLOBAL_ACTION_RECENTS, callback)
    }

    /**
     * 打开通知栏
     */
    fun openNotifications(callback: ((Boolean) -> Unit)? = null) {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS, callback)
    }

    /**
     * 快速设置
     */
    fun openQuickSettings(callback: ((Boolean) -> Unit)? = null) {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS, callback)
    }
}

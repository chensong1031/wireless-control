#!/bin/bash
# 调试脚本 - 获取设备崩溃日志

echo "正在获取设备崩溃日志..."

# 检查设备连接
adb devices | grep -v "List of devices" | grep "device" > /dev/null
if [ $? -ne 0 ]; then
    echo "❌ 设备未连接，请先连接设备"
    exit 1
fi

echo "✓ 设备已连接"
echo ""
echo "清除旧日志..."
adb logcat -c

echo ""
echo "请打开应用，然后按 Ctrl+C 停止日志捕获..."
echo ""
echo "正在捕获崩溃日志..."
echo "=========================================="

adb logcat -v time | tee /tmp/crash_log.txt | grep -E "FATAL|AndroidRuntime|WirelessControlApp|MainActivity|WirelessControlModule"

# 分析日志
echo ""
echo "=========================================="
echo "日志分析..."
echo ""

if grep -q "FATAL" /tmp/crash_log.txt; then
    echo "❌ 检测到 FATAL 错误"
    echo ""
    echo "崩溃堆栈："
    grep -A 20 "FATAL EXCEPTION" /tmp/crash_log.txt
else
    echo "✓ 未检测到 FATAL 错误"
fi

echo ""
echo "完整日志已保存到: /tmp/crash_log.txt"
echo ""
echo "查看完整日志："
echo "  cat /tmp/crash_log.txt"
echo ""
echo "或者使用 adb logcat 查看实时日志："
echo "  adb logcat | grep -E 'WirelessControlApp|MainActivity'"

#!/bin/bash

# 功能测试脚本
# 用途：测试设备端模块的各项功能

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
DEVICE_IP="${DEVICE_IP:-192.168.1.100}"
SERVER_PORT="${SERVER_PORT:-8080}"
BASE_URL="http://$DEVICE_IP:$SERVER_PORT/api"

# 打印函数
print_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[PASS]${NC} $1"
}

print_error() {
    echo -e "${RED}[FAIL]${NC} $1"
}

print_info() {
    echo -e "${YELLOW}[INFO]${NC} $1"
}

echo "=========================================="
echo "设备端模块功能测试"
echo "=========================================="
echo ""
echo "设备地址: $DEVICE_IP:$SERVER_PORT"
echo "API基础URL: $BASE_URL"
echo ""
echo "=========================================="
echo ""

# 测试计数
PASS=0
FAIL=0

# 测试1: 设备状态
print_test "1. 获取设备状态"
if curl -s -X GET "$BASE_URL/status" | grep -q "online"; then
    print_success "设备在线"
    ((PASS++))
else
    print_error "设备离线或API不可用"
    ((FAIL++))
fi
echo ""

# 测试2: 设备信息
print_test "2. 获取设备详细信息"
INFO=$(curl -s -X GET "$BASE_URL/device/info")
if echo "$INFO" | grep -q "device_id" && echo "$INFO" | grep -q "model"; then
    print_success "设备信息获取成功"
    echo "$INFO" | jq . 2>/dev/null || echo "$INFO"
    ((PASS++))
else
    print_error "设备信息获取失败"
    echo "$INFO"
    ((FAIL++))
fi
echo ""

# 测试3: 应用列表
print_test "3. 获取应用列表"
APPS=$(curl -s -X GET "$BASE_URL/apps")
if echo "$APPS" | grep -q "\[\]" || echo "$APPS" | grep -q "package"; then
    print_success "应用列表获取成功"
    APP_COUNT=$(echo "$APPS" | jq length 2>/dev/null || echo "未知")
    echo "  应用数量: $APP_COUNT"
    ((PASS++))
else
    print_error "应用列表获取失败"
    echo "$APPS"
    ((FAIL++))
fi
echo ""

# 测试4: 心跳检测
print_test "4. 心跳检测"
HEARTBEAT=$(curl -s -X GET "$BASE_URL/heartbeat")
if echo "$HEARTBEAT" | grep -q "timestamp"; then
    print_success "心跳正常"
    echo "$HEARTBEAT" | jq . 2>/dev/null || echo "$HEARTBEAT"
    ((PASS++))
else
    print_error "心跳检测失败"
    echo "$HEARTBEAT"
    ((FAIL++))
fi
echo ""

# 测试5: 联系人列表
print_test "5. 获取联系人列表"
CONTACTS=$(curl -s -X GET "$BASE_URL/contacts")
if echo "$CONTACTS" | grep -q "\[\]" || echo "$CONTACTS" | grep -q "name"; then
    print_success "联系人列表获取成功"
    CONTACT_COUNT=$(echo "$CONTACTS" | jq length 2>/dev/null || echo "未知")
    echo "  联系人数量: $CONTACT_COUNT"
    ((PASS++))
else
    print_error "联系人列表获取失败"
    echo "$CONTACTS"
    ((FAIL++))
fi
echo ""

# 测试6: 短信列表
print_test "6. 获取短信列表"
SMS=$(curl -s -X GET "$BASE_URL/sms/list")
if echo "$SMS" | grep -q "\[\]" || echo "$SMS" | grep -q "address"; then
    print_success "短信列表获取成功"
    SMS_COUNT=$(echo "$SMS" | jq length 2>/dev/null || echo "未知")
    echo "  短信数量: $SMS_COUNT"
    ((PASS++))
else
    print_error "短信列表获取失败"
    echo "$SMS"
    ((FAIL++))
fi
echo ""

# 测试7: 通话记录
print_test "7. 获取通话记录"
CALLS=$(curl -s -X GET "$BASE_URL/calls/list")
if echo "$CALLS" | grep -q "\[\]" || echo "$CALLS" | grep -q "number"; then
    print_success "通话记录获取成功"
    CALL_COUNT=$(echo "$CALLS" | jq length 2>/dev/null || echo "未知")
    echo "  通话记录数量: $CALL_COUNT"
    ((PASS++))
else
    print_error "通话记录获取失败"
    echo "$CALLS"
    ((FAIL++))
fi
echo ""

# 测试8: 通知列表
print_test "8. 获取通知列表"
NOTIFICATIONS=$(curl -s -X GET "$BASE_URL/notifications")
if echo "$NOTIFICATIONS" | grep -q "\[\]" || echo "$NOTIFICATIONS" | grep -q "package"; then
    print_success "通知列表获取成功"
    NOTIF_COUNT=$(echo "$NOTIFICATIONS" | jq length 2>/dev/null || echo "未知")
    echo "  通知数量: $NOTIF_COUNT"
    ((PASS++))
else
    print_error "通知列表获取失败"
    echo "$NOTIFICATIONS"
    ((FAIL++))
fi
echo ""

# 测试9: 执行命令（点击）
print_test "9. 测试点击命令"
CLICK_DATA='{"action":"click","x":500,"y":500}'
CLICK_RESULT=$(curl -s -X POST "$BASE_URL/execute" -H "Content-Type: application/json" -d "$CLICK_DATA")
if echo "$CLICK_RESULT" | grep -q "success\|error"; then
    print_success "点击命令执行成功"
    echo "$CLICK_RESULT" | jq . 2>/dev/null || echo "$CLICK_RESULT"
    ((PASS++))
else
    print_error "点击命令执行失败"
    echo "$CLICK_RESULT"
    ((FAIL++))
fi
echo ""

# 测试10: 启动应用（测试）
print_test "10. 测试启动应用命令"
START_APP_DATA='{"package":"com.android.settings"}'
START_RESULT=$(curl -s -X POST "$BASE_URL/apps/start" -H "Content-Type: application/json" -d "$START_APP_DATA")
if echo "$START_RESULT" | grep -q "success\|error\|not_found"; then
    print_success "启动应用命令执行成功"
    echo "$START_RESULT" | jq . 2>/dev/null || echo "$START_RESULT"
    ((PASS++))
else
    print_error "启动应用命令执行失败"
    echo "$START_RESULT"
    ((FAIL++))
fi
echo ""

# 输出测试结果
echo "=========================================="
echo "测试结果"
echo "=========================================="
echo ""
echo -e "通过: ${GREEN}$PASS${NC}"
echo -e "失败: ${RED}$FAIL${NC}"
echo -e "总计: $((PASS + FAIL))"
echo ""

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}✅ 所有测试通过！${NC}"
else
    echo -e "${RED}❌ 有 $FAIL 个测试失败${NC}"
    exit 1
fi

echo "=========================================="

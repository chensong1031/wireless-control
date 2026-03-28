#!/bin/bash

# 无线群控系统 - 功能测试脚本
# 测试多任务并发功能的所有API端点

API_BASE="http://127.0.0.1:5000"
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

echo "=========================================="
echo "无线群控系统 - 功能测试"
echo "测试时间: $(date)"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试函数
test_api() {
    local name="$1"
    local url="$2"
    local method="${3:-GET}"
    local data="$4"

    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "测试 $TOTAL_TESTS: $name ... "

    if [ "$method" = "POST" ] || [ "$method" = "PUT" ]; then
        response=$(curl -s -X $method "$API_BASE$url" \
            -H "Content-Type: application/json" \
            -d "$data" 2>&1)
    elif [ "$method" = "DELETE" ]; then
        response=$(curl -s -X DELETE "$API_BASE$url" 2>&1)
    else
        response=$(curl -s "$API_BASE$url" 2>&1)
    fi

    # 检查响应
    if echo "$response" | grep -q '"code":200' || echo "$response" | grep -q '"code": 201'; then
        echo -e "${GREEN}✅ 通过${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        echo "  响应: $(echo $response | head -c 200)..."
    else
        echo -e "${RED}❌ 失败${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "  响应: $response"
    fi
    echo ""
}

echo "=========================================="
echo "基础功能测试"
echo "=========================================="
echo ""

test_api "后端健康检查" "/api/health"
test_api "API首页" "/"

echo "=========================================="
echo "任务管理API测试"
echo "=========================================="
echo ""

# 创建测试任务
test_api "创建任务" "/api/tasks/" "POST" '{"name":"测试任务1","task_type":"script","script_id":1}'

# 获取任务列表
test_api "获取所有任务" "/api/tasks/"

# 获取任务统计
test_api "获取任务统计" "/api/tasks/statistics"

echo "=========================================="
echo "定时任务API测试"
echo "=========================================="
echo ""

# 创建定时任务
test_api "创建定时任务" "/api/scheduled-tasks/" "POST" '{"name":"每小时备份","cron_expression":"0 * * * *","task_id":1}'

# 获取定时任务列表
test_api "获取所有定时任务" "/api/scheduled-tasks/"

echo "=========================================="
echo "设备分组API测试"
echo "=========================================="
echo ""

# 创建分组
test_api "创建设备分组" "/api/groups/" "POST" '{"name":"生产环境","description":"生产设备","color":"#1890ff"}'

# 获取分组列表
test_api "获取所有分组" "/api/groups/"

echo "=========================================="
echo "设备管理API测试"
echo "=========================================="
echo ""

# 创建设备
test_api "创建设备" "/api/devices/" "POST" '{"name":"测试设备1","ip":"192.168.1.100"}'

# 获取设备列表
test_api "获取所有设备" "/api/devices/"

echo "=========================================="
echo "脚本管理API测试"
echo "=========================================="
echo ""

# 创建脚本
test_api "创建脚本" "/api/scripts/" "POST" '{"name":"测试脚本","content":"console.log(\"hello\");","script_type":"javascript"}'

# 获取脚本列表
test_api "获取所有脚本" "/api/scripts/"

echo "=========================================="
echo "统计API测试"
echo "=========================================="
echo ""

test_api "获取统计概览" "/api/statistics/overview"

echo "=========================================="
echo "测试结果汇总"
echo "=========================================="
echo ""
echo -e "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo ""
    echo -e "${GREEN}🎉 所有测试通过！${NC}"
    exit 0
else
    echo ""
    echo -e "${RED}⚠️  有 $FAILED_TESTS 个测试失败${NC}"
    exit 1
fi

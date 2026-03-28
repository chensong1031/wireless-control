#!/bin/bash

# 无线群控系统 - 功能测试脚本

API_BASE="http://101.43.0.77:5000"

echo "=========================================="
echo "无线群控系统 - 功能测试"
echo "=========================================="

echo ""
echo "测试 1: 后端健康检查"
echo "----------------------------------------"
curl -s $API_BASE/api/health | python3 -m json.tool

echo ""
echo "测试 2: 获取任务统计"
echo "----------------------------------------"
curl -s $API_BASE/api/tasks/statistics | python3 -m json.tool

echo ""
echo "测试 3: 获取所有任务"
echo "----------------------------------------"
curl -s $API_BASE/api/tasks/ | python3 -m json.tool | head -20

echo ""
echo "测试 4: 获取定时任务"
echo "----------------------------------------"
curl -s $API_BASE/api/scheduled-tasks/ | python3 -m json.tool | head -20

echo ""
echo "测试 5: 获取设备分组"
echo "----------------------------------------"
curl -s $API_BASE/api/groups/ | python3 -m json.tool | head -20

echo ""
echo "=========================================="
echo "✅ API测试完成"
echo "=========================================="
echo ""
echo "前端页面测试："
echo "  1. 打开浏览器访问: http://101.43.0.77/"
echo "  2. 登录 (admin / admin123)"
echo "  3. 测试新功能："
echo "     - 点击 '定时任务' 菜单"
echo "     - 点击 '任务统计' 菜单"
echo "     - 点击 '任务管理' 菜单 (测试批量执行)"
echo ""
echo "=========================================="

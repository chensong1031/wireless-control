#!/bin/bash

# 无线群控系统 - 前端更新部署脚本

echo "=========================================="
echo "无线群控系统 - 前端更新部署"
echo "=========================================="

# 备份现有文件
echo "[1/4] 备份现有前端文件..."
BACKUP_DIR="/opt/wireless-control/backup_frontend_$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR
cp /opt/wireless-control/frontend/wireless-web/src/App.jsx $BACKUP_DIR/
echo "  备份完成: $BACKUP_DIR"

# 更新前端代码
echo "[2/4] 更新前端代码..."
# 这一步在脚本外部完成，使用 scp 上传文件

# 重新构建前端
echo "[3/4] 重新构建前端..."
cd /opt/wireless-control/frontend/wireless-web
npm run build
echo "  构建完成"

# 测试前端访问
echo "[4/4] 测试前端访问..."
if curl -s -f http://127.0.0.1/ > /dev/null; then
    echo "  ✅ 前端访问正常"
else
    echo "  ❌ 前端访问失败"
    exit 1
fi

# 测试后端API
if curl -s -f http://127.0.0.1/api/health > /dev/null; then
    echo "  ✅ 后端API正常"
else
    echo "  ❌ 后端API失败"
    exit 1
fi

echo ""
echo "=========================================="
echo "✅ 前端更新部署完成！"
echo "=========================================="
echo ""
echo "访问地址："
echo "  前端：http://101.43.0.77/"
echo "  后端：http://101.43.0.77:5000/"
echo ""
echo "新功能测试："
echo "  - 定时任务管理：http://101.43.0.77/#/scheduled-tasks"
echo "  - 任务统计页面：http://101.43.0.77/#/task-statistics"
echo "  - 增强任务管理：http://101.43.0.77/#/tasks"
echo ""
echo "备份位置："
echo "  $BACKUP_DIR"
echo ""
echo "=========================================="

#!/bin/bash

# 无线群控系统 - 代码更新部署脚本

echo "=========================================="
echo "无线群控系统 - 代码更新部署"
echo "=========================================="

# 停止服务
echo "[1/5] 停止后端服务..."
sudo systemctl stop wireless-control-backend
sleep 2

# 备份现有代码
echo "[2/5] 备份现有代码..."
BACKUP_DIR="/opt/wireless-control/backup_$(date +%Y%m%d_%H%M%S)"
sudo mkdir -p $BACKUP_DIR
sudo cp -r /opt/wireless-control/backend/app $BACKUP_DIR/
sudo cp /opt/wireless-control/backend/task_executor.py $BACKUP_DIR/ 2>/dev/null || true
echo "  备份完成: $BACKUP_DIR"

# 更新代码
echo "[3/5] 更新代码..."
sudo cp /tmp/deploy_package/backend/app/models.py /opt/wireless-control/backend/app/
sudo cp /tmp/deploy_package/backend/app/__init__.py /opt/wireless-control/backend/app/
sudo cp /tmp/deploy_package/backend/app/routes/__init__.py /opt/wireless-control/backend/app/routes/
sudo cp /tmp/deploy_package/backend/app/routes/tasks.py /opt/wireless-control/backend/app/routes/
sudo cp /tmp/deploy_package/backend/app/routes/scheduled_tasks.py /opt/wireless-control/backend/app/routes/
sudo cp /tmp/deploy_package/backend/app/routes/groups.py /opt/wireless-control/backend/app/routes/
sudo cp /tmp/deploy_package/backend/app/routes/devices.py /opt/wireless-control/backend/app/routes/
sudo cp /tmp/deploy_package/backend/task_executor.py /opt/wireless-control/backend/
echo "  代码更新完成"

# 安装新依赖
echo "[4/5] 安装新依赖..."
cd /opt/wireless-control/backend
sudo venv/bin/pip install APScheduler==3.10.4
echo "  依赖安装完成"

# 初始化数据库（创建新表）
echo "[4.5/5] 初始化数据库..."
sudo venv/bin/python -c "from app import app; with app.app_context(): from app import db; db.create_all(); print('数据库初始化完成')"
echo "  数据库初始化完成"

# 启动服务
echo "[5/5] 启动服务..."
sudo systemctl start wireless-control-backend
sleep 3

# 检查服务状态
echo ""
echo "=========================================="
echo "检查服务状态..."
echo "=========================================="
sudo systemctl status wireless-control-backend --no-pager
echo ""
echo "服务进程:"
ps aux | grep gunicorn | grep -v grep | wc -l | xargs echo "  Gunicorn 进程数:"

echo ""
echo "=========================================="
echo "✅ 部署完成！"
echo "=========================================="
echo ""
echo "访问地址："
echo "  前端：http://101.43.0.77/"
echo "  后端：http://101.43.0.77:5000/"
echo ""
echo "测试新功能："
echo "  - 任务并发执行: POST /api/tasks/batch/execute"
echo "  - 定时任务管理: /api/scheduled-tasks"
echo "  - 设备分组管理: /api/groups"
echo ""
echo "=========================================="

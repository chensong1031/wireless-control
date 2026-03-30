# 服务器配置备份清单

> 无线群控系统 v2.0 - 服务器迁移备份
> 日期: 2026-03-30

---

## 📋 服务器信息

| 项目 | 值 |
|------|-----|
| 当前服务器 IP | 101.43.0.77 |
| SSH 端口 | 22 |
| SSH 用户 | ubuntu |
| SSH 密钥 | /workspace/projects/workspace-projects/无线群控项目/11.pem |

---

## 📁 重要文件清单

### 1. 后端 (/opt/wireless-control/backend/)

```
backend/
├── app/                          # Flask 应用代码
│   ├── __init__.py              # 应用初始化
│   ├── models.py                # 数据库模型
│   └── routes/                  # API 路由
│       ├── auth.py              # 认证
│       ├── devices.py           # 设备管理
│       ├── tasks.py             # 任务管理
│       ├── scripts.py           # 脚本管理
│       └── device_connection.py # 设备连接（扫码注册）
├── app.py                       # 应用入口
├── run.py                       # 启动脚本
├── requirements.txt             # Python 依赖
├── device_connection.py        # 设备连接模块
├── instance/
│   └── wireless.db              # SQLite 数据库
└── venv/                        # Python 虚拟环境
```

### 2. 前端 (/opt/wireless-control/frontend/)

```
frontend/
└── wireless-web/                # React 前端
    ├── src/
    │   └── App.jsx             # 主应用组件
    ├── dist/                   # 构建输出
    │   ├── index.html
    │   └── assets/
    │       └── index-*.js      # 打包后的 JS
    └── package.json
```

### 3. Web 静态文件 (/var/www/html/)

```
/var/www/html/
├── index.html                  # 主页面
├── qrcode.html                 # 二维码页面
├── assets/                     # 前端资源
│   └── index-*.js
└── test.html                   # 测试页面
```

### 4. 数据库 (/opt/wireless-control/database/)

```
database/
└── init.sql                    # 数据库初始化脚本
```

---

## ⚙️ 配置文件

### 1. 环境变量 (/opt/wireless-control/backend/.env)

```
FLASK_APP=app
FLASK_ENV=production
SECRET_KEY=dev-secret-key
DATABASE_URL=sqlite:///wireless.db
JWT_SECRET_KEY=jwt-secret-key
JWT_ACCESS_TOKEN_EXPIRES=3600
```

### 2. Nginx 配置

- 配置文件: `/etc/nginx/sites-available/default`
- Web 根目录: `/var/www/html`
- 监听端口: 80
- 反向代理: 5000 端口 → 后端 Flask

### 3. 后端运行方式

```bash
# 启动命令
cd /opt/wireless-control/backend
source venv/bin/activate
python run.py

# 或使用 Gunicorn
gunicorn -w 4 -b 0.0.0.0:5000 app:app
```

---

## 🔌 端口配置

| 端口 | 服务 | 状态 |
|------|------|------|
| 80 | Nginx (前端) | 运行中 |
| 5000 | Flask API | 运行中 |
| 8080 | 设备端 HTTP 服务器 | 设备上运行 |

---

## 🔑 认证信息

| 项目 | 用户名 | 密码 |
|------|--------|------|
| 后端管理 | admin | admin123 |
| SSH 密钥 | - | 11.pem |
| 数据库 | - | 无密码 (SQLite) |

---

## 🌐 API 端点

### 认证 API
- `POST /api/auth/login` - 登录
- `POST /api/auth/logout` - 登出
- `GET /api/auth/profile` - 用户信息

### 设备管理 API
- `GET /api/devices` - 设备列表
- `POST /api/devices` - 添加设备
- `PUT /api/devices/:id` - 更新设备
- `DELETE /api/devices/:id` - 删除设备

### 设备连接 API (扫码注册)
- `GET /api/device-conn/qrcode` - 生成二维码
- `POST /api/device-conn/register` - 设备注册
- `POST /api/device-conn/heartbeat` - 心跳
- `POST /api/device-conn/message` - 消息上报
- `GET /api/device-conn/command` - 获取指令
- `POST /api/device-conn/disconnect` - 断开连接

### 任务管理 API
- `GET /api/tasks` - 任务列表
- `POST /api/tasks` - 创建任务
- `POST /api/tasks/:id/execute` - 执行任务

### 脚本管理 API
- `GET /api/scripts` - 脚本列表
- `POST /api/scripts` - 上传脚本
- `POST /api/scripts/:id/execute` - 执行脚本

---

## 📦 备份命令

### 完整备份脚本

```bash
#!/bin/bash
# 备份日期
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/tmp/backup_$DATE"

# 创建备份目录
mkdir -p $BACKUP_DIR

# 1. 备份后端
echo "正在备份后端..."
cp -r /opt/wireless-control/backend $BACKUP_DIR/

# 2. 备份前端
echo "正在备份前端..."
cp -r /opt/wireless-control/frontend $BACKUP_DIR/

# 3. 备份 Web 文件
echo "正在备份 Web 文件..."
cp -r /var/www/html $BACKUP_DIR/

# 4. 备份数据库
echo "正在备份数据库..."
cp /opt/wireless-control/backend/instance/wireless.db $BACKUP_DIR/

# 5. 备份配置文件
echo "正在备份配置文件..."
cp /opt/wireless-control/backend/.env $BACKUP_DIR/

# 6. 压缩备份
echo "正在压缩备份..."
cd /tmp
tar -czvf wireless_control_backup_$DATE.tar.gz backup_$DATE/

echo "备份完成: /tmp/wireless_control_backup_$DATE.tar.gz"
```

---

## 🚀 迁移步骤

### 1. 新服务器准备
```bash
# 安装必要软件
sudo apt update
sudo apt install -y python3 python3-pip python3-venv nginx sqlite3

# 安装 Python 依赖
cd /opt/wireless-control/backend
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

### 2. 文件迁移
```bash
# 复制备份文件到新服务器
scp -r /tmp/backup_20260330_123456 ubuntu@新服务器IP:/opt/wireless-control/
```

### 3. 数据库初始化
```bash
cd /opt/wireless-control/backend
source venv/bin/activate
flask db upgrade
# 或直接复制 SQLite 数据库
cp backup/wireless.db instance/
```

### 4. Nginx 配置
```bash
sudo cp nginx.conf /etc/nginx/sites-available/wireless-control
sudo ln -s /etc/nginx/sites-available/wireless-control /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 5. 启动服务
```bash
# 启动后端
cd /opt/wireless-control/backend
source venv/bin/activate
nohup python run.py > logs/app.log 2>&1 &

# 或使用 Gunicorn
nohup gunicorn -w 4 -b 0.0.0.0:5000 app:app > logs/gunicorn.log 2>&1 &
```

### 6. 验证服务
```bash
# 测试后端 API
curl http://localhost:5000/api/auth/login

# 测试前端
curl http://localhost/
```

---

## 📝 重要提醒

1. **数据库**: SQLite 文件 `wireless.db` 包含所有设备、任务、用户数据，迁移时必须复制
2. **密钥文件**: SSH 密钥 `11.pem` 必须妥善保管
3. **SECRET_KEY**: 生产环境建议更换新的密钥
4. **端口占用**: 确保 80、5000 端口未被占用
5. **防火墙**: 确保 80、5000 端口开放

---

## 📞 服务状态检查命令

```bash
# 检查后端状态
curl http://101.43.0.77:5000/api/auth/login

# 检查前端状态
curl http://101.43.0.77/

# 检查二维码 API
curl http://101.43.0.77:5000/api/device-conn/qrcode

# 检查 Nginx 状态
sudo systemctl status nginx

# 检查 Python 进程
ps aux | grep python
```

---

## 🔧 快速恢复命令

```bash
# 1. 停止服务
pkill -f "python run.py"
pkill -f gunicorn

# 2. 恢复文件
tar -xzvf wireless_control_backup_20260330.tar.gz
cp -r backup_20260330/* /opt/wireless-control/

# 3. 重启服务
cd /opt/wireless-control/backend
source venv/bin/activate
python run.py &

# 4. 检查状态
curl http://localhost:5000/api/auth/login
```

---

## 📊 系统架构图

```
                    ┌─────────────────────┐
                    │   新服务器 (目标)     │
                    │   IP: 待配置         │
                    └──────────┬──────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
┌───────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Nginx :80    │    │  Flask :5000   │    │  前端静态文件   │
│  (反向代理)    │───▶│  (API 服务)    │    │  /var/www/html │
└───────────────┘    └─────────────────┘    └─────────────────┘
                           │
                           ▼
                    ┌─────────────────┐
                    │  SQLite 数据库 │
                    │  wireless.db   │
                    └─────────────────┘
```

---

**备份完成时间:** 2026-03-30 18:24
**备份文件位置:** 本地工作区 + 服务器 /opt/wireless-control/

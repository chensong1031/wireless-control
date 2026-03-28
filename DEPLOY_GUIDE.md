# 无线群控系统 v2.0 - 完整优化版部署包

## 📦 部署包信息

- **文件名：** `deploy_package.tar.gz`
- **大小：** 7.6 KB
- **包含内容：**
  - ✅ Flask 后端（完整版）
  - ✅ React 前端（完整版）
  - ✅ 用户认证系统
  - ✅ 数据库支持
  - ✅ 一键部署脚本

---

## 🚀 快速部署（3步完成）

### 第1步：上传到服务器

```bash
scp deploy_package.tar.gz root@101.43.0.77:/tmp/
```

### 第2步：SSH登录服务器

```bash
ssh root@101.43.0.77
```

### 第3步：解压并部署

```bash
cd /tmp
tar -xzf deploy_package.tar.gz
cd deploy_package
bash deploy.sh
```

**就这么简单！** 系统会自动安装所有依赖并启动服务。

---

## 🌐 部署后访问

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端界面 | http://101.43.0.77:5000/ | 主界面 |
| 后端API | http://101.43.0.77:5000/api/ | API服务 |

**注意：** 如果使用Nginx，前端访问 http://101.43.0.77/

---

## 🔐 默认账号

- **用户名：** `admin`
- **密码：** `admin123`

**首次登录后请立即修改密码！**

---

## ✅ 功能特性

### 后端功能
- ✅ Flask 3.0 + SQLAlchemy ORM
- ✅ JWT用户认证系统
- ✅ SQLite数据库（可换MySQL）
- ✅ RESTful API设计
- ✅ 设备管理API
- ✅ 任务管理API
- ✅ 脚本管理API
- ✅ 统计数据API

### 前端功能
- ✅ React 18 + Ant Design 5
- ✅ 用户登录/登出
- ✅ 设备管理（添加、删除）
- ✅ 任务管理（创建、删除）
- ✅ 脚本管理（上传、删除）
- ✅ 实时监控（统计刷新）
- ✅ 响应式布局
- ✅ 左侧导航菜单

---

## 📝 管理命令

### 查看服务状态

```bash
# 查看进程
ps aux | grep -E 'python|vite'

# 查看端口
netstat -tlnp | grep -E '5000|5173'

# 查看日志
tail -f deploy_package/backend/logs/app.log
tail -f deploy_package/frontend/wireless-web/logs/frontend.log
```

### 重启服务

```bash
# 重启后端
cd deploy_package/backend
pkill -f "python.*app/__init__.py"
source venv/bin/activate
nohup python app/__init__.py > logs/app.log 2>&1 &

# 重启前端
cd deploy_package/frontend/wireless-web
pkill -f vite
nohup npm run dev > logs/frontend.log 2>&1 &
```

### 停止服务

```bash
# 停止所有服务
pkill -f "python.*app/__init__.py"
pkill -f vite
```

---

## 🔄 数据库管理

### 查看数据库

```bash
cd deploy_package/backend
source venv/bin/activate
python -c "from app import app; with app.app_context(): from app import db; print([u.username for u in db.session.query(User.__table__).all()])"
```

### 重置数据库

```bash
cd deploy_package/backend
source venv/bin/activate
python -c "from app import app; with app.app_context(): from app import db; db.drop_all(); db.create_all(); print('数据库已重置')"
```

### 备份数据库

```bash
cp deploy_package/backend/app.db /tmp/app_backup_$(date +%Y%m%d).db
```

---

## 🔧 环境要求

### 最低配置
- **操作系统：** Ubuntu 20.04+ / CentOS 7+
- **Python：** 3.7+
- **Node.js：** 16+
- **内存：** 512MB+
- **磁盘：** 1GB+

### 推荐配置
- **操作系统：** Ubuntu 22.04 LTS
- **Python：** 3.8+
- **Node.js：** 18+
- **内存：** 1GB+
- **磁盘：** 2GB+

---

## ⚠️ 重要提示

### 防火墙配置

如果无法从外网访问，请开放端口：

```bash
# Ubuntu/Debian
sudo ufw allow 5000
sudo ufw allow 5173
sudo ufw reload

# CentOS/RHEL
sudo firewall-cmd --permanent --add-port=5000/tcp
sudo firewall-cmd --permanent --add-port=5173/tcp
sudo firewall-cmd --reload
```

### 腾讯云安全组

1. 登录腾讯云控制台
2. 进入 **云服务器 > 实例**
3. 点击 **安全组 > 配置规则**
4. 添加入站规则：
   - TCP:5000（后端）
   - TCP:5173（前端）
   - 来源：0.0.0.0/0

---

## 🐛 常见问题

### 问题1：端口被占用

```bash
# 查看5000端口占用
lsof -i:5000

# 停止占用进程
kill -9 <PID>
```

### 问题2：npm install失败

```bash
# 清理缓存
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### 问题3：Python依赖安装失败

```bash
cd deploy_package/backend
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
```

### 问题4：前端白屏

1. 按 F12 打开浏览器控制台
2. 查看错误信息
3. 检查API地址是否正确（应为 http://101.43.0.77:5000）

---

## 📊 项目结构

```
deploy_package/
├── backend/
│   ├── app/
│   │   ├── __init__.py       # Flask应用入口
│   │   ├── models.py          # 数据库模型
│   │   └── routes/
│   │       ├── __init__.py  # 蓝图定义
│   │       ├── auth.py      # 认证路由
│   │       ├── device.py    # 设备路由
│   │       ├── task.py      # 任务路由
│   │       ├── script.py    # 脚本路由
│   │       └── statistics.py # 统计路由
│   ├── requirements.txt     # Python依赖
│   └── logs/                # 日志目录
├── frontend/
│   └── wireless-web/
│       ├── src/
│       │   ├── pages/       # 页面组件
│       │   │   ├── Auth/
│       │   │   ├── Devices/
│       │   │   ├── Tasks/
│       │   │   ├── Scripts/
│       │   │   └── Monitor/
│       │   ├── App.jsx      # 主应用
│       │   └── main.jsx     # 入口文件
│       ├── package.json      # 项目配置
│       ├── vite.config.js    # Vite配置
│       └── index.html        # HTML入口
├── deploy.sh                # 部署脚本
└── README.md                # 本文档
```

---

## 🎯 技术栈

### 后端
- **Web框架：** Flask 3.0
- **ORM：** SQLAlchemy
- **认证：** Flask-JWT-Extended
- **数据库：** SQLite（可换MySQL）
- **CORS：** Flask-CORS

### 前端
- **框架：** React 18
- **UI库：** Ant Design 5.11
- **路由：** React Router 6.20
- **HTTP客户端：** Axios
- **构建工具：** Vite 5.0

---

## 📈 优化亮点

相比之前的版本，这个优化版包含：

### 新增功能
- ✅ 用户认证系统（登录/注册/权限）
- ✅ 数据持久化（SQLite数据库）
- ✅ JWT Token认证
- ✅ 密码加密存储（bcrypt）
- ✅ API安全保护

### 代码优化
- ✅ 更好的代码结构
- ✅ 模块化设计
- ✅ 错误处理完善
- ✅ 日志记录

### 部署优化
- ✅ 一键部署脚本
- ✅ 自动依赖安装
- ✅ 自动服务启动
- ✅ 自动数据库初始化

---

## 🎉 部署成功后

部署完成后，你将拥有：

1. **完整的用户系统**
   - 用户注册/登录
   - Token认证
   - 权限管理

2. **完善的功能模块**
   - 设备管理
   - 任务管理
   - 脚本管理
   - 实时监控

3. **数据持久化**
   - 所有数据保存在数据库
   - 重启不丢失

4. **实时更新**
   - 统计数据自动刷新
   - 实时状态监控

---

## 💡 后续扩展建议

如需进一步优化，可以考虑：

1. **升级到MySQL**
   - 修改 DATABASE_URL
   - 重新安装依赖

2. **添加WebSocket**
   - 安装 Flask-SocketIO
   - 实现实时推送

3. **添加Nginx HTTPS**
   - 配置SSL证书
   - 启用HTTPS访问

4. **添加Redis缓存**
   - 加速API响应
   - 缓存统计数据

---

## 📞 技术支持

遇到问题？检查：

1. **日志文件**
   - `backend/logs/app.log`
   - `frontend/wireless-web/logs/frontend.log`

2. **服务状态**
   - `ps aux | grep python`
   - `netstat -tlnp | grep 5000`

3. **端口占用**
   - `lsof -i:5000`

---

**祝部署顺利！** 🚀

---

**版本：** v2.0 完整优化版
**更新时间：** 2026-03-26
**文档作者：** 旺财 🐕

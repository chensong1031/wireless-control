# 快速开始指南

本指南帮助你在5分钟内完成设备端模块的构建和部署。

## 前置条件

1. **开发环境**
   - JDK 8或更高版本
   - Android SDK
   - Git

2. **测试设备**
   - Android 10-14设备
   - 已安装Magisk
   - 已安装LSPosed

3. **网络环境**
   - WiFi连接
   - 可以访问主控服务器

## 快速构建

### 步骤1：克隆项目
```bash
git clone <repository-url>
cd workspace/projects/workspace-main/device-module
```

### 步骤2：生成签名密钥（可选）
```bash
chmod +x generate-keystore.sh
./generate-keystore.sh
```

按照提示输入密钥信息，生成完成后会提示配置 `local.properties`。

### 步骤3：配置签名（可选）
如果跳过步骤2，可以直接构建未签名版本。

如需签名，复制配置模板：
```bash
cp local.properties.example local.properties
```

编辑 `local.properties`，填入密钥信息：
```properties
RELEASE_STORE_FILE=keystore/wireless-control.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=wireless_control
RELEASE_KEY_PASSWORD=your_key_password
```

### 步骤4：构建APK
```bash
chmod +x build.sh
./build.sh
```

构建完成后，APK文件位于 `deploy/` 目录：
- `app-debug.apk` - Debug版本
- `app-release-signed.apk` - Release版本（已签名）
- `app-release-unsigned.apk` - Release版本（未签名）

## 快速部署

### 方法1：通过Magisk安装（推荐）
```bash
# 1. 复制APK到设备
adb push deploy/app-release-signed.apk /sdcard/

# 2. 在设备上操作
# 打开Magisk应用
# → 模块
# → 从本地安装
# → 选择APK文件
# → 重启设备
```

### 方法2：通过ADB安装
```bash
# 安装Debug版本
adb install -r deploy/app-debug.apk

# 或安装Release版本
adb install -r deploy/app-release-signed.apk
```

## 快速配置

### 1. 启用Xposed模块
1. 打开LSPosed应用
2. 找到"Wireless Control"模块
3. 勾选需要Hook的应用（微信、QQ、系统UI）
4. 重启设备

### 2. 授予权限
打开"Wireless Control"应用，按照提示授予：
- 无障碍服务权限
- 通知访问权限
- 存储权限
- 其他必要权限

### 3. 测试连接
```bash
# 获取设备IP
adb shell ip addr show wlan0 | grep "inet "

# 测试HTTP服务器（替换为实际IP）
curl http://192.168.1.100:8080/api/status
```

如果看到以下输出，说明部署成功：
```json
{
  "status": "online",
  "timestamp": 1679928000000
}
```

## 快速测试

### 运行自动化测试
```bash
chmod +x test-functions.sh

# 使用默认IP测试
./test-functions.sh

# 使用指定IP测试
DEVICE_IP=192.168.1.100 ./test-functions.sh
```

### 手动测试几个关键功能
```bash
# 1. 获取设备信息
curl http://192.168.1.100:8080/api/device/info

# 2. 获取应用列表
curl http://192.168.1.100:8080/api/apps

# 3. 测试点击
curl -X POST http://192.168.1.100:8080/api/execute \
  -H "Content-Type: application/json" \
  -d '{"action":"click","x":500,"y":500}'

# 4. 启动设置应用
curl -X POST http://192.168.1.100:8080/api/apps/start \
  -H "Content-Type: application/json" \
  -d '{"package":"com.android.settings"}'
```

## 常用命令

### 查看日志
```bash
# 应用日志
adb logcat | grep -i wireless_control

# Xposed日志
adb logcat | grep -i xposed

# 服务状态
adb shell dumpsys activity services | grep wireless_control
```

### 重启应用
```bash
adb shell am force-stop com.wireless.control.device
adb shell am start -n com.wireless.control.device/.MainActivity
```

### 卸载应用
```bash
adb uninstall com.wireless.control.device
```

### 重新安装
```bash
adb install -r deploy/app-debug.apk
```

## 常见问题快速解决

### 问题1：模块未生效
**解决：**
1. 打开LSPosed，检查模块是否启用
2. 勾选目标应用
3. 重启设备

### 问题2：HTTP服务无法访问
**解决：**
1. 检查设备IP：`adb shell ip addr show wlan0`
2. 检查端口：`adb shell netstat -tlnp | grep 8080`
3. 重启应用：`adb shell am force-stop com.wireless.control.device && adb shell am start -n com.wireless.control.device/.MainActivity`

### 问题3：权限未授予
**解决：**
1. 打开应用
2. 按照提示授予权限
3. 检查设置中应用权限

## 下一步

- 📖 详细文档：查看 [README.md](README.md)
- 🧪 完整测试：查看 [TEST_CHECKLIST.md](TEST_CHECKLIST.md)
- 🔧 故障排查：查看 README.md 中的"故障排查"章节
- 🚀 进阶开发：查看 README.md 中的"开发说明"章节

## 获取帮助

如果遇到问题：

1. 查看日志：`adb logcat | grep -i wireless_control`
2. 查看故障排查：README.md 的"故障排查"章节
3. 运行测试脚本：`./test-functions.sh`
4. 查看完整文档：README.md

---

**版本：** v1.0.0
**更新时间：** 2026-03-27
**预计构建时间：** 2-3分钟
**预计部署时间：** 3-5分钟

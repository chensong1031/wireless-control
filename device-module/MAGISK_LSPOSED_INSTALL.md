# Magisk 和 LSPosed 安装指南

**更新时间：** 2026-03-27
**适用系统：** Android 10-14
**设备要求：** Bootloader已解锁

---

## 📦 一、Magisk 安装

### 1.1 什么是Magisk？

Magisk是一个开源的Android ROOT管理工具，特点：
- ✅ "Systemless" ROOT（不修改系统分区）
- ✅ 支持Magisk模块扩展
- ✅ 可以隐藏ROOT状态（用于银行/支付应用）
- ✅ 支持OTA更新而不失去ROOT

### 1.2 官方下载链接

**官方渠道：**
- **官方网站：** https://magisk.dev/download/
- **GitHub仓库：** https://github.com/topjohnwu/Magisk
- **最新版本：** v30.7（截至2026-03-16）

**下载选项：**
1. **Magisk APK** - 安装应用（已ROOT设备）
2. **Magisk Boot镜像** - 用于首次刷入（未ROOT设备）

**文件信息：**
```
文件名：Magisk-v30.7.apk
大小：约12MB
版本：v30.7
更新日期：2026-03-16
```

### 1.3 安装步骤

#### 方法1：已有ROOT（推荐）

如果设备已经ROOT（如SuperSU），直接安装Magisk应用即可：

```bash
# 1. 下载Magisk APK
wget https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.apk

# 2. 通过ADB安装
adb install Magisk-v30.7.apk

# 3. 打开应用并更新Magisk
```

#### 方法2：首次ROOT（需要解锁Bootloader）

**准备工作：**
1. 解锁设备Bootloader
2. 备份所有重要数据
3. 下载设备对应的Boot镜像

**步骤：**

```bash
# 1. 下载Magisk APK并安装
adb install Magisk-v30.7.apk

# 2. 提取设备Boot镜像
adb shell su -c dd if=/dev/block/by-name/boot of=/sdcard/boot.img
adb pull /sdcard/boot.img

# 3. 在电脑上修补Boot镜像
# 打开Magisk应用
# 选择 "安装" → "选择并修补文件"
# 选择 boot.img 文件
# 修补后的文件在 Download/magisk_patched_[随机字符].img

# 4. 刷入修补后的镜像
adb push magisk_patched.img /sdcard/
adb reboot bootloader
fastboot flash boot magisk_patched.img
fastboot reboot
```

#### 方法3：使用Recovery刷入（TWRP/OrangeFox）

```bash
# 1. 下载Magisk刷入包
wget https://github.com/topjohnwu/Magisk/releases/download/v30.7/Magisk-v30.7.zip

# 2. 重启到Recovery
adb reboot recovery

# 3. 在Recovery中选择"Install"
# 4. 选择 Magisk-v30.7.zip
# 5. 滑动确认刷入
# 6. 重启系统
```

### 1.4 验证安装

```bash
# 检查Magisk版本
adb shell su -c magisk -v

# 检查ROOT权限
adb shell su -c id

# 检查Magisk模块
adb shell su -c magisk --list-modules
```

预期输出应显示：
```
# uid=0(root) gid=0(root) ...
Magisk v30.7 (30700) installed
```

### 1.5 启用Zygisk

LSPosed需要Zygisk支持，必须启用：

```bash
# 1. 打开Magisk应用
# 2. 进入"设置"
# 3. 找到"Zygisk"选项
# 4. 启用Zygisk（重启生效）
```

或使用命令行：

```bash
adb shell su -c magisk --enable-zygisk
adb reboot
```

---

## 🧩 二、LSPosed 安装

### 2.1 什么是LSPosed？

LSPosed是Xposed框架的现代实现，特点：
- ✅ 基于Zygisk（Magisk模块）
- ✅ 支持Android 10-14
- ✅ 更好的稳定性和兼容性
- ✅ 模块化设计
- ✅ 支持隐藏框架

### 2.2 官方下载链接

**官方渠道：**
- **GitHub仓库：** https://github.com/LSPosed/LSPosed
- **Magisk模块站：** https://www.magiskmodule.com/lsposed-zygisk/
- **最新版本：** v1.11.0（截至2026-03-15）

**下载选项：**
1. **Zygisk版本**（推荐）- 需要Magisk + Zygisk
2. **Riru版本**（旧版）- 需要Riru模块（不推荐）

**文件信息：**
```
文件名：LSPosed-v1.11.0.zip
大小：约2.6MB
版本：v1.11.0
类型：Magisk模块
更新日期：2026-03-15
```

### 2.3 下载链接

**GitHub Releases（官方）：**
```bash
# 最新版本下载
https://github.com/LSPosed/LSPosed/releases

# 直接下载链接（示例）
https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip
```

**镜像站（国内加速）：**
- **官方镜像：** https://mirror.ghproxy.com/https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip
- **FastGit：** https://hub.fastgit.xyz/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip

### 2.4 安装步骤

#### 前置条件检查

```bash
# 1. 检查Magisk版本（需要v23+）
adb shell su -c magisk -v

# 2. 检查Zygisk是否启用
adb shell su -c magisk --zygisk-status

# 3. 检查Android版本（需要Android 10+）
adb shell getprop ro.build.version.release
```

#### 安装方法1：通过Magisk应用（推荐）

```bash
# 1. 下载LSPosed模块
wget https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip

# 2. 推送到设备
adb push LSPosed-v1.11.0.zip /sdcard/Download/

# 3. 打开Magisk应用
# 4. 选择"模块" → "从本地安装"
# 5. 选择 LSPosed-v1.11.0.zip
# 6. 重启设备
adb reboot
```

#### 安装方法2：通过Recovery刷入

```bash
# 1. 下载LSPosed模块
wget https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip

# 2. 推送到设备
adb push LSPosed-v1.11.0.zip /sdcard/

# 3. 重启到Recovery
adb reboot recovery

# 4. 在Recovery中选择"Install"
# 5. 选择 LSPosed-v1.11.0.zip
# 6. 滑动确认刷入
# 7. 重启系统
```

#### 安装方法3：通过ADB命令

```bash
# 1. 下载模块
wget https://github.com/LSPosed/LSPosed/releases/download/v1.11.0/LSPosed-v1.11.0.zip

# 2. 推送到设备
adb push LSPosed-v1.11.0.zip /data/local/tmp/

# 3. 通过Magisk Manager刷入
adb shell su -c magisk --install-module /data/local/tmp/LSPosed-v1.11.0.zip

# 4. 重启设备
adb reboot
```

### 2.5 验证安装

```bash
# 1. 检查LSPosed模块
adb shell su -c magisk --list-modules | grep lsposed

# 2. 检查LSPosed服务
adb shell pm list packages | grep lsposed

# 3. 启动LSPosed管理器
adb shell am start -n io.github.lsposed.manager/.ui.MainActivity
```

预期输出：
```
io.github.lsposed.manager (uid=10123)
com.android.shell
```

### 2.6 首次配置

```bash
# 1. 打开LSPosed应用
# 2. 授予必要权限（存储、通知等）
# 3. 检查框架状态（应显示"正在运行"）
# 4. 查看已安装的模块
```

---

## ⚙️ 三、安装后的配置

### 3.1 Magisk配置

#### 启用Zygisk（必需）

```
Magisk应用 → 设置 → Zygisk → 启用
```

#### 配置隐藏（可选）

如果需要使用银行/支付应用：

```
Magisk应用 → 超级用户 → Magisk隐藏
选择需要隐藏ROOT的应用
```

### 3.2 LSPosed配置

#### 配置隐藏模式（推荐）

```
LSPosed应用 → 设置 → 模式选择
推荐：Zygisk模式（最佳性能和兼容性）
```

#### 配置调试日志（开发时）

```
LSPosed应用 → 设置 → 调试
启用"日志"
启用"详细日志"
```

---

## 🚀 四、安装无线群控设备端模块

### 4.1 构建APK

```bash
cd /workspace/projects/workspace-main/device-module

# 生成签名密钥
./generate-keystore.sh

# 配置签名
cp local.properties.example local.properties
# 编辑 local.properties 填入密钥信息

# 构建APK
./build.sh
```

### 4.2 安装模块

```bash
# 1. 复制APK到设备
adb push deploy/app-release-signed.apk /sdcard/

# 2. 在Magisk中安装
# 打开Magisk应用
# → 模块
# → 从本地安装
# → 选择 app-release-signed.apk
# → 重启设备

# 3. 或者直接作为应用安装
adb install -r deploy/app-release-signed.apk
```

### 4.3 启用模块

```bash
# 1. 打开LSPosed应用
# 2. 找到"Wireless Control"模块
# 3. 点击模块
# 4. 勾选需要Hook的应用（微信、QQ、系统UI）
# 5. 重启设备使模块生效
```

### 4.4 授予权限

```bash
# 1. 打开"Wireless Control"应用
# 2. 按照提示授予权限：
#    - 无障碍服务权限
#    - 通知访问权限
#    - 存储权限
#    - 电话权限（如需拨打电话）
#    - 短信权限（如需发送短信）
#    - 相机权限（如需截图）

# 3. 重启应用
adb shell am force-stop com.wireless.control.device
adb shell am start -n com.wireless.control.device/.MainActivity
```

---

## 🔍 五、验证安装

### 5.1 检查系统环境

```bash
# 1. 检查ROOT
adb shell su -c id

# 2. 检查Magisk版本
adb shell su -c magisk -v

# 3. 检查Zygisk状态
adb shell su -c magisk --zygisk-status

# 4. 检查LSPosed
adb shell pm list packages | grep lsposed

# 5. 检查设备模块
adb shell pm list packages | grep wireless_control
```

### 5.2 测试HTTP服务器

```bash
# 1. 获取设备IP
DEVICE_IP=$(adb shell ip addr show wlan0 | grep "inet " | awk '{print $2}' | cut -d/ -f1)
echo "设备IP: $DEVICE_IP"

# 2. 测试HTTP服务器
curl http://$DEVICE_IP:8080/api/status
```

预期输出：
```json
{
  "status": "online",
  "timestamp": 1679928000000
}
```

### 5.3 测试Xposed Hook

```bash
# 1. 查看LSPosed日志
adb logcat | grep -i "LSPosed"

# 2. 查看模块日志
adb logcat | grep -i "WirelessControl"

# 3. 打开微信，发送一条消息
# 4. 检查日志中是否有Hook记录
```

---

## ⚠️ 六、常见问题

### 6.1 Magisk安装失败

**问题：** 刷入Magisk后无法启动

**解决：**
1. 检查Boot镜像是否正确下载
2. 重新修补Boot镜像
3. 尝试使用不同版本的Magisk
4. 检查设备兼容性

### 6.2 Zygisk无法启用

**问题：** Zygisk选项显示为灰色

**解决：**
1. 更新Magisk到最新版本
2. 检查Android版本（需要Android 10+）
3. 检查设备架构（仅支持ARM/ARM64）
4. 重启设备

### 6.3 LSPosed安装失败

**问题：** 刷入LSPosed后无法使用

**解决：**
1. 确认Zygisk已启用
2. 检查LSPosed版本兼容性
3. 清除LSPosed数据：
   ```bash
   adb shell pm clear io.github.lsposed.manager
   ```
4. 重新安装LSPosed

### 6.4 模块未生效

**问题：** 无线群控模块未生效

**解决：**
1. 检查模块是否在LSPosed中启用
2. 检查是否勾选了目标应用
3. 重启设备
4. 查看Xposed日志：`adb logcat | grep -i "Xposed"`
5. 查看模块日志：`adb logcat | grep -i "WirelessControl"`

### 6.5 应用闪退

**问题：** 打开模块应用即闪退

**解决：**
1. 检查Android版本（仅支持10-14）
2. 检查LSPosed是否正确安装
3. 查看崩溃日志：`adb logcat -b crash`
4. 重新安装模块

---

## 📚 七、参考资源

### 官方链接
- **Magisk官网：** https://magisk.dev
- **Magisk GitHub：** https://github.com/topjohnwu/Magisk
- **LSPosed GitHub：** https://github.com/LSPosed/LSPosed
- **Magisk模块站：** https://www.magiskmodule.com

### 文档和教程
- **Magisk官方文档：** https://topjohnwu.github.io/Magisk/
- **LSPosed官方文档：** https://lsposed.github.io/
- **Magisk使用指南：** https://github.com/topjohnwu/Magisk/wiki

### 社区支持
- **Magisk GitHub Issues：** https://github.com/topjohnwu/Magisk/issues
- **LSPosed GitHub Issues：** https://github.com/LSPosed/LSPosed/issues
- **XDA Developers：** https://forum.xda-developers.com/

---

## 📋 八、版本信息

### Magisk
- **最新版本：** v30.7
- **发布日期：** 2026-03-16
- **最低要求：** Android 5.0+
- **推荐版本：** Android 10+

### LSPosed
- **最新版本：** v1.11.0
- **发布日期：** 2026-03-15
- **最低要求：** Android 10+
- **依赖：** Magisk v23+ + Zygisk

### 无线群控设备端模块
- **当前版本：** v1.0.0
- **发布日期：** 2026-03-27
- **最低要求：** Android 10+
- **依赖：** Magisk v25+ + LSPosed v1.9+

---

## 🎯 九、快速安装流程总结

```bash
# 1. 安装Magisk
adb install Magisk-v30.7.apk
# 在应用中修补并刷入Boot镜像

# 2. 启用Zygisk
# Magisk应用 → 设置 → Zygisk → 启用
adb reboot

# 3. 安装LSPosed
adb push LSPosed-v1.11.0.zip /sdcard/Download/
# Magisk应用 → 模块 → 从本地安装 → 选择LSPosed
adb reboot

# 4. 安装无线群控模块
adb push app-release-signed.apk /sdcard/
# Magisk应用 → 模块 → 从本地安装 → 选择APK
adb reboot

# 5. 配置模块
# LSPosed应用 → 启用模块 → 勾选应用
adb reboot

# 6. 授予权限
# 打开应用 → 按提示授予权限

# 7. 测试
curl http://设备IP:8080/api/status
```

---

**文档版本：** v1.0
**更新时间：** 2026-03-27
**适用系统：** Android 10-14
**作者：** OpenClaw
